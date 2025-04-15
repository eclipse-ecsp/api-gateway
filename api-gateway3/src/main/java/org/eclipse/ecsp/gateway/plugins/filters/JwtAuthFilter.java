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

package org.eclipse.ecsp.gateway.plugins.filters;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Filter to validate JWT Token.
 * This filter checks the JWT token in the request header, validates it, and extracts user information and scopes.
 * It also validates custom headers based on the provided configuration.
 */
public class JwtAuthFilter implements GatewayFilter, Ordered {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String INVALID_TOKEN_CODE = "api.gateway.error.token.invalid";
    private static final String INVALID_TOKEN = "Invalid Token";
    private static final String TOKEN_VERIFICATION_FAILED = "Token verification failed";
    /**
     * JWT Parser map.
     */
    protected final Map<String, JwtParser> jwtParsers;
    /**
     * Set of route scopes.
     */
    protected final Set<String> routeScopes = new HashSet<>();
    private final Map<String, Map<String, String>> tokenHeaderValidationConfig;
    private String userIdField;

    /**
     * Constructor to initialize the JwtAuthFilter.
     *
     * @param config                      the JWT Auth Validator configuration
     * @param jwtParsers                  the map of JWT parsers
     * @param tokenHeaderValidationConfig  the map of token header validation configurations
     * @param userIdField                 the user ID field in the JWT claims
     */
    public JwtAuthFilter(JwtAuthFilter.Config config, Map<String, JwtParser> jwtParsers,
                         Map<String, Map<String, String>> tokenHeaderValidationConfig, String userIdField) {
        this.tokenHeaderValidationConfig = tokenHeaderValidationConfig;
        this.jwtParsers = jwtParsers;
        this.userIdField = userIdField;
        if (config != null && config.getScope() != null) {
            LOGGER.debug("Config: {}", config);
            routeScopes.addAll(Arrays.stream((config.getScope()).split(",")).map(String::trim).toList());
        }
    }

    private static String getTokenHeader(Claims claims, String headerName) {
        String tokenHeader = null;
        if (claims != null) {
            tokenHeader = claims.keySet().stream()
                    .filter(headerKey -> headerKey.equalsIgnoreCase(headerName))
                    .findAny()
                    .orElse(null);
        }
        return tokenHeader;
    }

    private static void handleNullTokenHeaderValidation(String headerName, Map<String, String> headerConfigMap) {
        if (Boolean.parseBoolean(headerConfigMap.get(GatewayConstants.REQUIRED))) {
            LOGGER.error("Token Header: {} is null or empty", headerName);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }
    }

    private static String getTokenHeaderValue(Claims claims, String tokenHeader) {
        String tokenHeaderValue = null;
        if (tokenHeader != null) {
            tokenHeaderValue = String.valueOf(claims.get(tokenHeader));
        }
        return tokenHeaderValue;
    }

    private static void checkValidRequestHeader(Builder builder,
                                                Map<String, String> headerConfigMap,
                                                String tokenHeaderValidationRegex,
                                                String tokenHeaderValue,
                                                String tokenHeader) {
        boolean validRequestHeader = Pattern.compile(tokenHeaderValidationRegex).matcher(
                tokenHeaderValue).matches();
        if (validRequestHeader) {
            LOGGER.debug("Token header: {} validated against regex-pattern: {}",
                    tokenHeader, tokenHeaderValidationRegex);
            if (Boolean.parseBoolean(headerConfigMap.get(GatewayConstants.REQUIRED))) {
                LOGGER.debug("Token header: {} included to the request headers", tokenHeader);
                builder.header(tokenHeader, tokenHeaderValue);
            }
        } else {
            LOGGER.error("Validation Failed! Token header {} is invalid", tokenHeader);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        LOGGER.debug("JWT Auth Validation for Request: {}", request.getPath());
        String token = request.getHeaders().getFirst(GatewayConstants.AUTHORIZATION);

        if (token == null || token.trim().isEmpty() || !token.startsWith(GatewayConstants.BEARER)) {
            LOGGER.error("Token is missing from the request : {}", request.getURI().getPath());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }
        token = token.split(" ")[1];

        // Validate JWT
        Claims claims = validate(token);
        LOGGER.debug("Token validated - claims: {}", claims);
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        Builder builder = exchange.getRequest().mutate();

        if (userIdField == null || userIdField.isBlank() || claims.get(userIdField) == null) {
            this.userIdField = GatewayConstants.SUB;
        }
        // Validate user scopes against target route scope
        String scope = validateScope(route, claims);
        Object obj = claims.get(userIdField);
        if (obj == null) {
            LOGGER.error("User claim in token not available for request", request.getPath());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }

        headerValidation(claims, builder);

        String userId = String.valueOf(obj);
        Set<String> overrideScopes = new HashSet<>(Arrays.stream(scope.split(",")).map(String::trim).toList());
        overrideScopes = Stream.concat(overrideScopes.stream(), routeScopes.stream()).collect(Collectors.toSet());
        // set user id as header
        builder.header(GatewayConstants.USER_ID, userId);
        // set scopes as header
        builder.header(GatewayConstants.SCOPE, scope);
        builder.header(GatewayConstants.OVERRIDE_SCOPE, String.join(",", overrideScopes));
        LOGGER.debug("Override-Scope: {}", overrideScopes);
        LOGGER.debug("Request Valid from userId: {}, scope: {}", userId, scope);
        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    private void headerValidation(Claims claims, Builder builder) {
        try {
            //Header Validation
            if (tokenHeaderValidationConfig != null) {
                tokenHeaderValidationConfig.forEach((headerName, headerConfigMap) -> {
                    String tokenHeader = getTokenHeader(claims, headerName);
                    String tokenHeaderValue = getTokenHeaderValue(claims, tokenHeader);
                    if (!headerConfigMap.isEmpty() && !headerConfigMap.get(GatewayConstants.REGEX).isEmpty()
                            && !headerConfigMap.get(GatewayConstants.REQUIRED).isEmpty()) {
                        String tokenHeaderValidationRegex = headerConfigMap.get(GatewayConstants.REGEX);
                        if (tokenHeaderValue == null || tokenHeaderValue.isEmpty()) {
                            handleNullTokenHeaderValidation(headerName, headerConfigMap);
                        } else {
                            checkValidRequestHeader(builder,
                                    headerConfigMap,
                                    tokenHeaderValidationRegex,
                                    tokenHeaderValue,
                                    tokenHeader);
                        }
                    }
                });
            }
        } catch (PatternSyntaxException regexException) {
            LOGGER.error("Error compiling regex : {}, verify the regex-pattern config", regexException.getMessage());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        } catch (Exception ex) {
            LOGGER.error("Validation failed with : {}", ex.getMessage());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        }
    }

    private Claims validate(final String token) {
        if (jwtParsers.isEmpty()) {
            LOGGER.info("Map-jwtParsers is empty");
        }
        for (final Entry<String, JwtParser> entry : jwtParsers.entrySet()) {
            try {
                return entry.getValue().parseSignedClaims(token).getPayload();
            } catch (SecurityException
                     | MalformedJwtException
                     | ExpiredJwtException
                     | UnsupportedJwtException
                     | IllegalArgumentException ex) {
                throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
            } catch (Exception ex) {
                LOGGER.warn("Unable to parse the Token with JWTParser: {} exception:{}", entry.getKey(), ex);
            }
        }
        throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
    }

    @SuppressWarnings("unchecked")
    private String validateScope(final Route route, final Claims claims) {
        if (route == null || claims == null) {
            LOGGER.warn("Invalid route/claims");
            throw new ApiGatewayException(HttpStatus.NOT_FOUND, "api.gateway.error", "Request not found");
        }

        Set<String> userScopes = new HashSet<>();
        Object scopeObj = claims.get(GatewayConstants.SCOPE);
        if (scopeObj != null) {
            LOGGER.debug("Scope class: {}", scopeObj.getClass());
            if (scopeObj instanceof List<?>) {
                LOGGER.debug("Scope class: List : {}", scopeObj);
                // scopes are in the form of List
                userScopes = new HashSet<>((List<String>) scopeObj);
            } else if (scopeObj instanceof String scopeStr) {
                if (scopeStr.contains(",")) {
                    // scopes are in the form of comma separated
                    userScopes = new HashSet<>(Arrays.asList(((String) scopeObj).split(",")));
                    LOGGER.debug("Comma separated Scope string : {}", userScopes);
                } else {
                    // default space separated scopes
                    userScopes = new HashSet<>(Arrays.asList(((String) scopeObj).split(" ")));
                    LOGGER.debug("Space separated Scope string : {}", userScopes);
                }
            }
        }

        LOGGER.debug("User scopes: {} and route scopes: {} ", userScopes, routeScopes);
        boolean valid = false;
        if (routeScopes.isEmpty()) {
            // Scope validation is not defined for the route
            valid = true;
        } else {
            // at minimum one of the routeScopes must match userScopes
            valid = routeScopes.stream().anyMatch(userScopes::contains);
        }
        if (!valid) {
            throw new ApiGatewayException(HttpStatus.NOT_FOUND, "api.gateway.error", "Request not found");
        }
        return String.join(",", userScopes);
    }

    @Override
    public int getOrder() {
        return GatewayConstants.JWT_AUTH_FILTER_ORDER;
    }

    /**
     * Config class to add configuration to pass to filter.
     */
    @Setter
    @Getter
    @NoArgsConstructor
    @ToString
    public static class Config {
        /**
         * The scope.
         */
        protected String scope;
    }
}
