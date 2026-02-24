/********************************************************************************
 * Copyright (c) 2023-24 Harman International
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and\
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.gateway.filter;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.eclipse.ecsp.gateway.service.AccessRuleMatcherService;
import org.eclipse.ecsp.gateway.service.ClientAccessControlService;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.InputValidator;
import org.eclipse.ecsp.gateway.utils.JwtUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.HasRouteId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Gateway filter factory for Client Access Control validation.
 *
 * <p>Extracts client ID from JWT claims and validates access rules.
 * Rejects unauthorized requests with 401 Unauthorized.
 *
 * <p>Implementation flow:
 * 1. Check if path should skip validation (health, docs, etc.)
 * 2. Extract JWT from Authorization header
 * 3. Extract client ID from JWT using configured claim chain
 * 4. Validate client ID for security patterns
 * 5. Validate access rules (deferred to Phase 4/US-2)
 * 6. Allow or deny request
 *
 * @see InputValidator
 * @see JwtUtils
 */
public class ClientAccessControlGatewayFilterFactory extends
        AbstractGatewayFilterFactory<ClientAccessControlGatewayFilterFactory.Config> {

    private static final IgniteLogger LOGGER = 
        IgniteLoggerFactory.getLogger(ClientAccessControlGatewayFilterFactory.class);
    private static final String UNKNOWN = "unknown";
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final int MAX_LOG_VALUE_LENGTH = 100;

    private final ClientAccessControlProperties properties;
    private final AccessRuleMatcherService accessRuleMatcherService;
    private final ClientAccessControlService cacheService;
    private final ClientAccessControlMetrics metrics;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Constructs the filter factory with all required dependencies.
     *
     * @param properties Configuration properties
     * @param accessRuleMatcherService Access rule matching service
     * @param cacheService Configuration cache service
     * @param metrics Metrics recording service
     */
    public ClientAccessControlGatewayFilterFactory(
            ClientAccessControlProperties properties,
            AccessRuleMatcherService accessRuleMatcherService,
            ClientAccessControlService cacheService,
            ClientAccessControlMetrics metrics) {
        super(Config.class);
        this.properties = properties;
        this.accessRuleMatcherService = accessRuleMatcherService;
        this.cacheService = cacheService;
        this.metrics = metrics;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            // Check if filter is globally enabled
            if (!properties.isEnabled()) {
                LOGGER.debug("Client access control is disabled globally");
                return chain.filter(exchange);
            }

            // Check if path should skip validation
            String requestPath = exchange.getRequest().getPath().value();
            if (shouldSkipPath(requestPath)) {
                LOGGER.debug("Skipping validation for path: {}", requestPath);
                return chain.filter(exchange);
            }

            // Start validation timer
            Instant validationStart = Instant.now();

            // Perform access control validation
            return performAccessControl(exchange, chain, requestPath, validationStart, config);
        }, GatewayConstants.CLIENT_ACCESS_CTRL_FILTER_ORDER);
    }

    /**
     * Perform access control validation for the request.
     *
     * @param exchange Server web exchange
     * @param chain Gateway filter chain
     * @param requestPath Request path
     * @param validationStart Validation start time
     * @return Mono response
     */
    private Mono<Void> performAccessControl(ServerWebExchange exchange, GatewayFilterChain chain,
                                            String requestPath, Instant validationStart,
                                            Config config) {
        // Extract JWT from Authorization header
        String jwt = extractJwt(exchange);
        if (jwt == null) {
            return handleDeniedRequest(exchange, requestPath, UNKNOWN, validationStart, "missing_jwt",
                    "Missing or invalid Authorization header for path: {}", "Missing or invalid JWT token");
        }

        // Extract client ID from JWT claims
        String clientId = JwtUtils.extractClientId(jwt, properties.getClaimNames());
        if (clientId == null) {
            return handleDeniedRequest(exchange, requestPath, UNKNOWN, validationStart, "missing_client_id",
                    "No client ID found in JWT claims for path: {}", "Client ID not found in JWT claims");
        }

        // Record request checked
        metrics.recordRequestChecked(clientId);

        // Validate client ID for security patterns
        if (!InputValidator.isValid(clientId)) {
            LOGGER.error("[AUDIT] Invalid or malicious client ID detected - clientId: {}, path: {} - Request denied",
                    sanitizeLogValue(clientId), requestPath);
            metrics.recordRequestDenied(clientId, UNKNOWN, requestPath, "invalid_client_id");
            metrics.recordValidationDuration(Duration.between(validationStart, Instant.now()), clientId);
            return unauthorizedResponse(exchange, "Invalid client identifier");
        }

        // Extract service and route from request path
        String service = config.getServiceName() != null ? config.getServiceName() : UNKNOWN;
        String route = config.getRouteId() != null ? config.getRouteId() : UNKNOWN;

        // Validate client configuration and access rules
        return validateClientAccess(exchange, chain, validationStart, clientId, service, route);
    }

    /**
     * Validate client configuration and access rules.
     *
     * @param exchange Server web exchange
     * @param chain Gateway filter chain
     * @param requestPath Request path
     * @param validationStart Validation start time
     * @param clientId Client identifier
     * @param service Service name
     * @param route Route path
     * @return Mono response
     */
    private Mono<Void> validateClientAccess(ServerWebExchange exchange, GatewayFilterChain chain,
                                            Instant validationStart,
                                            String clientId, String service, String route) {
        // Lookup client configuration from cache
        ClientAccessConfig clientConfig = cacheService.getConfig(clientId);
        if (clientConfig == null) {
            LOGGER.warn("[AUDIT] Unauthorized access attempt - Client not found: "
                    + "clientId={}, service={}, route={} - Request denied",
                    clientId, service, route);
            metrics.recordRequestDenied(clientId, service, route, "client_not_found");
            metrics.recordValidationDuration(Duration.between(validationStart, Instant.now()), clientId);
            return unauthorizedResponse(exchange, "Client not found or inactive");
        }

        if (!clientConfig.isActive()) {
            LOGGER.warn("[AUDIT] Unauthorized access attempt - Inactive client: "
                    + "clientId={}, service={}, route={} - Request denied",
                    clientId, service, route);
            metrics.recordRequestDenied(clientId, service, route, "client_inactive");
            metrics.recordValidationDuration(Duration.between(validationStart, Instant.now()), clientId);
            return unauthorizedResponse(exchange, "Client is inactive");
        }

        // Validate access rules against service:route
        List<AccessRule> rules = clientConfig.getRules();
        if (!accessRuleMatcherService.isAllowed(rules, service, route)) {
            LOGGER.warn("[AUDIT] Access denied by rules - clientId={}, service={}, route={}, "
                    + "ruleCount={} - Request denied",
                    clientId, service, route, rules.size());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[DEBUG] Rule evaluation trace for clientId={}: rules={}", clientId, rules);
            }
            metrics.recordRequestDenied(clientId, service, route, "rule_denied");
            metrics.recordValidationDuration(Duration.between(validationStart, Instant.now()), clientId);
            return unauthorizedResponse(exchange, "Access denied");
        }

        // Access granted - record success and continue
        return handleAllowedRequest(exchange, chain, clientId, service, route, validationStart);
    }

    /**
     * Handle allowed request - record metrics and continue filter chain.
     *
     * @param exchange Server web exchange
     * @param chain Gateway filter chain
     * @param clientId Client identifier
     * @param service Service name
     * @param route Route path
     * @param validationStart Validation start time
     * @return Mono response
     */
    private Mono<Void> handleAllowedRequest(ServerWebExchange exchange, GatewayFilterChain chain,
                                            String clientId, String service, String route,
                                            Instant validationStart) {
        // Add client ID to exchange attributes for downstream use
        exchange.getAttributes().put("clientId", clientId);

        // Record successful access and validation duration
        metrics.recordRequestAllowed(clientId, service, route);
        Duration validationDuration = Duration.between(validationStart, Instant.now());
        metrics.recordValidationDuration(validationDuration, clientId);

        LOGGER.info("[AUDIT] Access allowed - clientId={}, service={}, route={}, "
                + "validationTime={}ms - Request authorized",
                clientId, service, route, validationDuration.toMillis());

        return chain.filter(exchange);
    }

    /**
     * Check if request path should skip validation.
     *
     * @param requestPath Request path
     * @return true if path matches any skip pattern
     */
    private boolean shouldSkipPath(String requestPath) {
        List<String> skipPaths = properties.getSkipPaths();
        if (skipPaths == null || skipPaths.isEmpty()) {
            return false;
        }

        for (String pattern : skipPaths) {
            if (pathMatcher.match(pattern, requestPath)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extract JWT from Authorization header.
     *
     * @param exchange Server web exchange
     * @return JWT token string (without "Bearer " prefix), or null if not found
     */
    private String extractJwt(ServerWebExchange exchange) {
        List<String> authHeaders = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }

        String authHeader = authHeaders.get(0);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        // Remove "Bearer " prefix
        return authHeader.substring(BEARER_PREFIX_LENGTH);
    }

    /**
     * Return 401 Unauthorized response.
     *
     * @param exchange Server web exchange
     * @param reason Rejection reason for logging
     * @return Completed mono with 401 response
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String reason) {
        LOGGER.warn("Unauthorized access rejected: {}", reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /**
     * Handle denied request with metrics and logging.
     *
     * @param exchange Server web exchange
     * @param requestPath Request path
     * @param clientId Client identifier
     * @param validationStart Validation start time
     * @param denialReason Denial reason tag
     * @param logMessage Log message pattern
     * @param responseMessage Response message
     * @return Completed mono with 401 response
     */
    private Mono<Void> handleDeniedRequest(ServerWebExchange exchange, String requestPath, String clientId,
                                           Instant validationStart, String denialReason, 
                                           String logMessage, String responseMessage) {
        LOGGER.warn("[AUDIT] {} - reason={} - Request denied", logMessage, denialReason);
        metrics.recordRequestDenied(clientId, UNKNOWN, requestPath, denialReason);
        metrics.recordValidationDuration(Duration.between(validationStart, Instant.now()), clientId);
        return unauthorizedResponse(exchange, responseMessage);
    }

    /**
     * Sanitize value for safe logging (prevent log injection).
     *
     * @param value Value to sanitize
     * @return Sanitized string
     */
    private String sanitizeLogValue(String value) {
        if (value == null) {
            return "null";
        }
        String sanitized = value.replaceAll("[\r\n\t]", " ");
        return sanitized.length() > MAX_LOG_VALUE_LENGTH 
                ? sanitized.substring(0, MAX_LOG_VALUE_LENGTH) + "..." 
                : sanitized;
    }

    /**
     * Configuration class for filter.
     */
    @Getter
    @Setter
    public static class Config implements HasRouteId {
        private String serviceName;
        private String routeId;
    }
}
