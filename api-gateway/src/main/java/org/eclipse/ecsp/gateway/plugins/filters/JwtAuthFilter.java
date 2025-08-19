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

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.model.TokenHeaderValidationConfig;
import org.eclipse.ecsp.gateway.service.PublicKeyService;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
    private static final String DEFAULT = "DEFAULT";

    /**
     * Set of route scopes.
     */
    protected final Set<String> routeScopes = new HashSet<>();
    private final Map<String, TokenHeaderValidationConfig> tokenHeaderValidationConfig;
    private final PublicKeyService publicKeyService;
    Map<String, String> tokenClaimToHeaderMapping;

    /**
     * Constructor to initialize the JwtAuthFilter.
     *
     * @param config                      the JWT Auth Validator configuration
     * @param publicKeyService            the public key service to validate JWT signatures
     * @param jwtProperties               the map of token header validation configurations
     */
    public JwtAuthFilter(JwtAuthFilter.Config config,
                         PublicKeyService publicKeyService,
                         JwtProperties jwtProperties) {
        this.tokenHeaderValidationConfig = jwtProperties.getTokenHeaderValidationConfig();
        this.publicKeyService = publicKeyService;
        if (config != null && config.getScope() != null) {
            LOGGER.debug("Config: {}", config);
            routeScopes.addAll(Arrays.stream((config.getScope()).split(",")).map(String::trim).toList());
        }
        if (CollectionUtils.isEmpty(jwtProperties.getTokenClaimToHeaderMapping())) {
            LOGGER.debug("No token claim to header mapping configured");
            this.tokenClaimToHeaderMapping = new HashMap<>();
        } else {
            this.tokenClaimToHeaderMapping = jwtProperties.getTokenClaimToHeaderMapping();
            LOGGER.debug("Token claim to header mapping: {}", tokenClaimToHeaderMapping);
        }

        if (!this.tokenClaimToHeaderMapping.containsKey("user_id")) {
            LOGGER.debug("UserId claim is configured in token claim to header mapping");
            this.tokenClaimToHeaderMapping.put("sub", "user-id");
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

    private static String getTokenHeaderValue(Claims claims, String tokenHeader) {
        String tokenHeaderValue = null;
        if (tokenHeader != null) {
            Object headerValue = claims.get(tokenHeader);
            if (headerValue instanceof List<?> list) {
                // If the header value is a list, join it with commas
                tokenHeaderValue = list.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
            } else if (headerValue instanceof String[] strArray) {
                // If the header value is an array, join it with commas
                tokenHeaderValue = String.join(",", strArray);
            } else if (headerValue instanceof Set<?> setHeaders) {
                // If the header value is a set, join it with commas
                tokenHeaderValue = setHeaders.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));

            } else if (headerValue instanceof String str) {
                // If the header value is a string, use it directly
                tokenHeaderValue = str;
            } else {
                // For other types, convert to string
                tokenHeaderValue = String.valueOf(claims.get(tokenHeader));
            }
        }
        return tokenHeaderValue;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getId();
        ServerHttpRequest request = exchange.getRequest();
        LOGGER.debug("JWT Auth Validation for Request: {}, requestId: {}", request.getPath(), requestId);
        String token = request.getHeaders().getFirst(GatewayConstants.AUTHORIZATION);

        if (token == null || token.trim().isEmpty() || !token.startsWith(GatewayConstants.BEARER)) {
            LOGGER.error("Token is missing from the request : {}, requestId", request.getPath(), requestId);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }
        token = token.split(" ")[1];

        // Validate JWT
        Claims claims = validateToken(token);
        LOGGER.debug("Token validated for request: {}, requestId: {}, token claims: {}",
                request.getPath(), requestId, claims);



        LOGGER.debug("Validating token claims  for request: {}, requestId: {}", request.getPath(), requestId);
        //validate token headers
        validateTokenHeaders(claims);

        LOGGER.debug("Token claims validated for request: {}, requestId: {}, validating route scopes",
                request.getPath(), requestId);
        // Validate user scopes against target route scope
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String scope = validateScope(route, claims);
        LOGGER.debug("Scope validated for request: {}, requestId: {}, scope: {},"
                        + " appending scope and override-scope headers to request",
                request.getPath(), requestId, scope);
        Set<String> overrideScopes = new HashSet<>(Arrays.stream(scope.split(",")).map(String::trim).toList());
        overrideScopes = Stream.concat(overrideScopes.stream(), routeScopes.stream()).collect(Collectors.toSet());

        // set scopes as header
        Builder builder = exchange.getRequest().mutate();
        builder.header(GatewayConstants.SCOPE, scope);
        builder.header(GatewayConstants.OVERRIDE_SCOPE, String.join(",", overrideScopes));
        LOGGER.debug("Added claims to request headers: scope={}, override-scope={}",
                scope, String.join(",", overrideScopes));
        // claim header mapping
        for (Entry<String, String> entry : tokenClaimToHeaderMapping.entrySet()) {
            String claimKey = entry.getKey();
            String headerName = entry.getValue();
            String claimValue = getTokenHeaderValue(claims, claimKey);
            if (!StringUtils.isEmpty(claimValue)) {
                builder.header(headerName, claimValue);
                LOGGER.debug("Added claim {} to request header: {} with value: {}", claimKey, headerName, claimValue);
            }
        }
        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    private void validateTokenHeaders(Claims claims) {
        try {
            //Header Validation
            if (!CollectionUtils.isEmpty(tokenHeaderValidationConfig)) {
                for (Entry<String, TokenHeaderValidationConfig> entry : tokenHeaderValidationConfig.entrySet()) {
                    String headerName = entry.getKey();
                    TokenHeaderValidationConfig headerConfigMap = entry.getValue();
                    validateClaims(claims, headerName, headerConfigMap);
                }
            }
        } catch (PatternSyntaxException regexException) {
            LOGGER.error("Error compiling regex : {}, verify the regex-pattern config", regexException);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        } catch (Exception ex) {
            LOGGER.error("Validation failed with : {}", ex);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        }
    }

    private static void validateClaims(Claims claims,
                                       String headerName,
                                       TokenHeaderValidationConfig headerConfigMap) {
        if (headerConfigMap.isRequired()) {
            String tokenHeader = getTokenHeader(claims, headerName);
            String tokenHeaderValue = getTokenHeaderValue(claims, tokenHeader);
            if (StringUtils.isEmpty(tokenHeader)
                    || StringUtils.isEmpty(tokenHeaderValue)) {
                LOGGER.debug("Required token Header: {} is not present in the claims", headerName);
                throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
            }
            if (StringUtils.isEmpty(headerConfigMap.getRegex())) {
                LOGGER.debug("Token Header: {} does not have a regex validation configured", headerName);
            } else {
                String regex = headerConfigMap.getRegex();
                boolean validRequestHeader = Pattern.compile(regex).matcher(
                        tokenHeaderValue).matches();
                if (!validRequestHeader) {
                    LOGGER.error("Validation Failed! Token header {}={} is invalid", tokenHeader,
                            tokenHeaderValue);
                    throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
                }
            }
        }
    }

    private Claims validateToken(final String token) {
        LOGGER.debug("Validating JWT Token: {}", token);
        try {
            JWT jwt = JWTParser.parse(token);
            Object kidObject = jwt.getHeader().toJSONObject().get("kid");
            Object tenantIdObject = jwt.getJWTClaimsSet().toJSONObject().get("tenantId");
            String kid = (kidObject == null || StringUtils.isEmpty(kidObject.toString()))
                    ? DEFAULT : kidObject.toString();
            String tenantId = (tenantIdObject == null || StringUtils.isEmpty(tenantIdObject.toString()))
                    ? "" : tenantIdObject.toString();
            if (DEFAULT.equals(kid)) {
                LOGGER.warn("JWT Token Header 'kid' is missing or empty, validating with default key");
            }
            LOGGER.debug("JWT Token Header 'kid': {}", kid);
            Optional<PublicKey> key = publicKeyService.findPublicKey(kid, tenantId);
            if (key.isEmpty() && !DEFAULT.equals(kid)) {
                LOGGER.warn("Public Key not found for kid: {}, tenantId: {}, trying with default", kid, tenantId);
                key = publicKeyService.findPublicKey(DEFAULT, null);
            }

            if (key.isEmpty()) {
                LOGGER.error("Public Key not found for kid: {}. Default key also not found.", kid);
                throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
            }
            LOGGER.debug("Public Key found for kid: {}", kid);
            JwtParser jwtParser = Jwts.parser().verifyWith(key.get()).build();
            Jws<Claims> parsedToken = jwtParser.parseSignedClaims(token);
            LOGGER.debug("JWT Token parsed successfully with kid: {}", kid);
            return parsedToken.getPayload();
        } catch (SecurityException
                 | MalformedJwtException
                 | ExpiredJwtException
                 | UnsupportedJwtException
                 | IllegalArgumentException ex) {
            LOGGER.warn("Token validation failed with exception: {} of type: {}, error: {}", ex.getMessage(),
                    ex.getClass(), ex);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        } catch (ApiGatewayException e) {
            throw e;
        } catch (Exception ex) {
            LOGGER.warn("Unable to parse the Token, exception:{}", ex.getMessage());
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
                String delimiter = scopeStr.contains(",") ? "," : StringUtils.SPACE;
                userScopes = new HashSet<>(Arrays.asList(scopeStr.split(delimiter)));
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
            LOGGER.error("User scopes: {} do not match route scopes: {} for routeId: {}", 
                    userScopes, routeScopes, route.getId());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, 
                    TOKEN_VERIFICATION_FAILED);
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
