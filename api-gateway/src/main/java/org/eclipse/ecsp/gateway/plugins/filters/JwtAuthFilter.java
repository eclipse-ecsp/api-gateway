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
import org.eclipse.ecsp.gateway.utils.GatewayUtils;
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
        String clientIp = GatewayUtils.getClientIpAddress(request);
        String requestPath = request.getPath() != null ? request.getPath().toString() : "unknown";
        LOGGER.debug("JWT Auth Validation for Request: {}, requestId: {}, clientIp: {}", 
                requestPath, requestId, clientIp);
        
        String token = request.getHeaders().getFirst(GatewayConstants.AUTHORIZATION);

        // authorization header not available in the request
        if (StringUtils.isBlank(token) || !token.startsWith(GatewayConstants.BEARER)) {
            LOGGER.error("Token validation failed - Token missing or invalid format. "
                    + "Request: {}, requestId: {}, clientIp: {}", 
                    requestPath, requestId, clientIp);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }

        token = token.split(" ")[1];

        // Validate JWT
        LOGGER.debug("Token format validation passed for request: {}, requestId: {}, clientIp: {}", 
                requestPath, requestId, clientIp);
        Claims claims = validateToken(token, requestId, requestPath, clientIp);
        LOGGER.debug("Token validated, validating claims... for request: {}, requestId: {}, clientIp: {},"
                + " token claims: {}", requestPath, requestId, clientIp, claims);

        //validate token headers
        validateTokenHeaders(claims, requestId, requestPath, clientIp);

        LOGGER.debug("Token claims validated for request: {}, requestId: {}, clientIp: {}, validating route scopes",
                requestPath, requestId, clientIp);
        
        // Validate user scopes against target route scope
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String scope = validateScope(route, claims, requestId, requestPath, clientIp);
        
        LOGGER.debug("Scope validated for request: {}, requestId: {}, clientIp: {}, scope: {},"
                        + " appending scope and override-scope headers to request",
                requestPath, requestId, clientIp, scope);
        
        Set<String> overrideScopes = new HashSet<>(Arrays.stream(scope.split(",")).map(String::trim).toList());
        overrideScopes = Stream.concat(overrideScopes.stream(), routeScopes.stream()).collect(Collectors.toSet());

        // set scopes as header
        Builder builder = exchange.getRequest().mutate();
        builder.header(GatewayConstants.SCOPE, scope);
        builder.header(GatewayConstants.OVERRIDE_SCOPE, String.join(",", overrideScopes));
        
        LOGGER.debug("Added claims to request headers: scope={}, override-scope={}, request: {}, "
                + "requestId: {}, clientIp: {}",
                scope, String.join(",", overrideScopes), requestPath, requestId, clientIp);
        
        // Token claims are added to request headers and to be sent to downstream microservices
        for (Entry<String, String> entry : tokenClaimToHeaderMapping.entrySet()) {
            String claimKey = entry.getKey();
            String headerName = entry.getValue();
            String claimValue = getTokenHeaderValue(claims, claimKey);
            if (!StringUtils.isEmpty(claimValue)) {
                builder.header(headerName, claimValue);
                LOGGER.debug("Added claim {} to request header: {} with value: {}, request: {}, "
                        + "requestId: {}, clientIp: {}", 
                        claimKey, headerName, claimValue, requestPath, requestId, clientIp);
            }
        }

        LOGGER.info("JWT authentication successful for request: {}, requestId: {}, clientIp: {}",
                requestPath, requestId, clientIp);
        
        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    private void validateTokenHeaders(Claims claims, String requestId, String requestPath, 
                                      String clientIp) {
        try {
            LOGGER.debug("Starting token header validation for request: {}, requestId: {}, clientIp: {}", 
                    requestPath, requestId, clientIp);
            
            //Header Validation
            if (!CollectionUtils.isEmpty(tokenHeaderValidationConfig)) {
                for (Entry<String, TokenHeaderValidationConfig> entry : tokenHeaderValidationConfig.entrySet()) {
                    String headerName = entry.getKey();
                    TokenHeaderValidationConfig headerConfigMap = entry.getValue();
                    
                    LOGGER.debug("Validating token header: {} for request: {}, requestId: {}, clientIp: {}", 
                            headerName, requestPath, requestId, clientIp);
                    
                    validateClaims(claims, headerName, headerConfigMap, requestId, requestPath, clientIp);
                    
                    LOGGER.debug("Token header validation passed for header: {}, request: {}, "
                            + "requestId: {}, clientIp: {}", 
                            headerName, requestPath, requestId, clientIp);
                }
            }
            
            LOGGER.debug("All token header validations passed for request: {}, requestId: {}, clientIp: {}", 
                    requestPath, requestId, clientIp);
            
        } catch (PatternSyntaxException regexException) {
            LOGGER.error("Token header validation failed - Invalid regex pattern. Request: {}, requestId: {}, "
                    + "clientIp: {}, error: {}", 
                    requestPath, requestId, clientIp, regexException.getMessage());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        } catch (Exception ex) {
            LOGGER.error("Token header validation failed with unexpected error. Request: {}, requestId: {}, "
                    + "clientIp: {}, error: {}", 
                    requestPath, requestId, clientIp, ex.getMessage());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        }
    }

    private static void validateClaims(Claims claims, String headerName, 
                                       TokenHeaderValidationConfig headerConfigMap, 
                                       String requestId, String requestPath, String clientIp) {
        if (headerConfigMap.isRequired()) {
            String tokenHeader = getTokenHeader(claims, headerName);
            String tokenHeaderValue = getTokenHeaderValue(claims, tokenHeader);
            
            if (StringUtils.isEmpty(tokenHeader) || StringUtils.isEmpty(tokenHeaderValue)) {
                LOGGER.error("Token claim validation failed - Required token header '{}' is missing. "
                        + "Request: {}, requestId: {}, clientIp: {}", 
                        headerName, requestPath, requestId, clientIp);
                throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
            }
            
            if (StringUtils.isEmpty(headerConfigMap.getRegex())) {
                LOGGER.debug("Token header '{}' validation passed (no regex configured). "
                        + "Request: {}, requestId: {}, clientIp: {}", 
                        headerName, requestPath, requestId, clientIp);
            } else {
                String regex = headerConfigMap.getRegex();
                boolean validRequestHeader = Pattern.compile(regex).matcher(tokenHeaderValue).matches();
                if (!validRequestHeader) {
                    LOGGER.error("Token claim validation failed - Token header '{}' with value '{}' "
                            + "does not match regex pattern '{}'. Request: {}, requestId: {}, clientIp: {}", 
                            tokenHeader, tokenHeaderValue, regex, requestPath, requestId, clientIp);
                    throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
                } else {
                    LOGGER.debug("Token header '{}' validation passed with regex pattern '{}'. "
                            + "Request: {}, requestId: {}, clientIp: {}", 
                            headerName, regex, requestPath, requestId, clientIp);
                }
            }
        } else {
            LOGGER.debug("Token header '{}' is not required, skipping validation. "
                    + "Request: {}, requestId: {}, clientIp: {}", 
                    headerName, requestPath, requestId, clientIp);
        }
    }

    private Claims validateToken(final String token, String requestId, String requestPath, 
                                 String clientIp) {
        LOGGER.debug("Starting JWT token validation for request: {}, requestId: {}, clientIp: {}", 
                requestPath, requestId, clientIp);
        
        try {
            // Parse token and extract metadata
            TokenMetadata metadata = parseTokenMetadata(token, requestPath, requestId, clientIp);
            
            // Get public key for validation
            PublicKey publicKey = getValidationKey(metadata, requestPath, requestId, clientIp);
            
            // Validate token signature and claims
            return validateTokenSignature(token, publicKey, metadata, requestPath, requestId, clientIp);
            
        } catch (SecurityException
                 | MalformedJwtException
                 | ExpiredJwtException
                 | UnsupportedJwtException
                 | IllegalArgumentException ex) {
            // Token validation failed due to expiration, signature, etc.
            String failureReason = GatewayUtils.getTokenValidationFailureReason(ex);
            LOGGER.error("Token validation failed - {}. Request: {}, requestId: {}, clientIp: {}, "
                    + "error: {}", 
                    failureReason, requestPath, requestId, clientIp, ex.getMessage());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        } catch (ApiGatewayException e) {
            throw e;
        } catch (Exception ex) {
            LOGGER.error("Token validation failed - Unexpected parsing error. Request: {}, requestId: {}, "
                    + "clientIp: {}, error: {}", 
                    requestPath, requestId, clientIp, ex.getMessage());
        }
        throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
    }

    /**
     * Parse token metadata including kid and tenantId.
     */
    private TokenMetadata parseTokenMetadata(String token, String requestPath, String requestId, 
                                            String clientIp) throws Exception {
        // 3. Token parsing
        JWT jwt = JWTParser.parse(token);
        Object kidObject = jwt.getHeader().toJSONObject().get("kid");
        Object tenantIdObject = jwt.getJWTClaimsSet().toJSONObject().get("tenantId");
        String kid = (kidObject == null || StringUtils.isEmpty(kidObject.toString()))
                ? DEFAULT : kidObject.toString();
        String tenantId = (tenantIdObject == null || StringUtils.isEmpty(tenantIdObject.toString()))
                ? "" : tenantIdObject.toString();
                
        LOGGER.debug("JWT token parsed successfully. Kid: {}, tenantId: {}, request: {}, requestId: {}, clientIp: {}", 
                kid, tenantId, requestPath, requestId, clientIp);
        
        if (DEFAULT.equals(kid)) {
            LOGGER.warn("JWT Token Header 'kid' is missing or empty, using default key for validation. "
                    + "Request: {}, requestId: {}, clientIp: {}, tenantId: {}", 
                    requestPath, requestId, clientIp, tenantId);
        }
        
        return new TokenMetadata(kid, tenantId);
    }

    /**
     * Get the public key for token validation.
     */
    private PublicKey getValidationKey(TokenMetadata metadata, String requestPath, String requestId, 
                                      String clientIp) {
        // 4. Public key retrieval
        LOGGER.debug("Fetching public key for kid: {}, tenantId: {}, request: {}, requestId: {}, clientIp: {}", 
                metadata.kid, metadata.tenantId, requestPath, requestId, clientIp);
        
        Optional<PublicKey> key = publicKeyService.findPublicKey(metadata.kid, metadata.tenantId);
        
        if (key.isEmpty() && !DEFAULT.equals(metadata.kid)) {
            LOGGER.warn("Public key not found for kid: {}, tenantId: {}, attempting fallback to default key. "
                    + "Request: {}, requestId: {}, clientIp: {}", 
                    metadata.kid, metadata.tenantId, requestPath, requestId, clientIp);
            key = publicKeyService.findPublicKey(DEFAULT, null);
        }

        if (key.isEmpty()) {
            LOGGER.error("Token validation failed - Public key not found. Kid: {}, tenantId: {}, "
                    + "request: {}, requestId: {}, clientIp: {}", 
                    metadata.kid, metadata.tenantId, requestPath, requestId, clientIp);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }
        
        LOGGER.debug("Public key found and will be used for validation. for kid: {}, request: {}, "
                + "requestId: {}, clientIp: {}", 
                metadata.kid, requestPath, requestId, clientIp);

        return key.get();
    }

    /**
     * Validate token signature and return claims.
     */
    private Claims validateTokenSignature(String token, PublicKey publicKey, TokenMetadata metadata,
                                         String requestPath, String requestId, String clientIp) {
        JwtParser jwtParser = Jwts.parser().verifyWith(publicKey).build();
        Jws<Claims> parsedToken = jwtParser.parseSignedClaims(token);
        
        // 8. Token validation successful
        String keySource = DEFAULT.equals(metadata.kid) ? "default" : "kid:" + metadata.kid;
        LOGGER.info("JWT token validation successful. Kid: {}, tenantId: {}, keySource: {}, "
                + "request: {}, requestId: {}, clientIp: {}", 
                metadata.kid, metadata.tenantId, keySource, requestPath, requestId, clientIp);
        
        return parsedToken.getPayload();
    }

    /**
     * Inner class to hold token metadata.
     */
    private static class TokenMetadata {
        final String kid;
        final String tenantId;
        
        TokenMetadata(String kid, String tenantId) {
            this.kid = kid;
            this.tenantId = tenantId;
        }
    }

    @SuppressWarnings("unchecked")
    private String validateScope(final Route route, final Claims claims, String requestId, 
                                String requestPath, String clientIp) {
        if (route == null || claims == null) {
            LOGGER.error("Scope validation failed - Invalid route or claims. Request: {}, requestId: {}, "
                    + "clientIp: {}", 
                    requestPath, requestId, clientIp);
            throw new ApiGatewayException(HttpStatus.NOT_FOUND, "api.gateway.error", "Request not found");
        }

        LOGGER.debug("Starting scope validation for route: {}, request: {}, requestId: {}, clientIp: {}", 
                route.getId(), requestPath, requestId, clientIp);

        Set<String> userScopes = new HashSet<>();
        Object scopeObj = claims.get(GatewayConstants.SCOPE);
        if (scopeObj != null) {
            LOGGER.debug("Token scope found, type: {}, value: {}, request: {}, requestId: {}, clientIp: {}", 
                    scopeObj.getClass().getSimpleName(), scopeObj, requestPath, requestId, clientIp);
            
            if (scopeObj instanceof List<?>) {
                // scopes are in the form of List
                userScopes = new HashSet<>((List<String>) scopeObj);
            } else if (scopeObj instanceof String scopeStr) {
                String delimiter = scopeStr.contains(",") ? "," : StringUtils.SPACE;
                userScopes = new HashSet<>(Arrays.asList(scopeStr.split(delimiter)));
            }
        } else {
            LOGGER.debug("No scope claim found in token for request: {}, requestId: {}, clientIp: {}", 
                    requestPath, requestId, clientIp);
        }

        LOGGER.debug("Extracted user scopes: {}, configured route scopes: {}, request: {}, "
                + "requestId: {}, clientIp: {}", 
                userScopes, routeScopes, requestPath, requestId, clientIp);
        
        boolean valid = false;
        if (routeScopes.isEmpty()) {
            // Scope validation is not defined for the route
            LOGGER.debug("No route scopes configured, scope validation passed. Route: {}, request: {}, "
                    + "requestId: {}, clientIp: {}", 
                    route.getId(), requestPath, requestId, clientIp);
            valid = true;
        } else {
            // at minimum one of the routeScopes must match userScopes
            valid = routeScopes.stream().anyMatch(userScopes::contains);
            if (valid) {
                LOGGER.debug("Scope validation passed - User scopes match route requirements. "
                        + "Matching scopes: {}, route: {}, request: {}, requestId: {}, clientIp: {}", 
                        routeScopes.stream().filter(userScopes::contains).collect(Collectors.toSet()), 
                        route.getId(), requestPath, requestId, clientIp);
            }
        }
        
        if (!valid) {
            // 5. Token and route scope validation failed
            LOGGER.error("Scope validation failed - User scopes do not match route requirements. "
                    + "User scopes: {}, required route scopes: {}, route: {}, request: {}, requestId: {}, "
                    + "clientIp: {}", 
                    userScopes, routeScopes, route.getId(), requestPath, requestId, clientIp);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, 
                    TOKEN_VERIFICATION_FAILED);
        }
        
        LOGGER.debug("Scope validation passed successfully. User scopes: {}, route: {}, request: {}, "
                + "requestId: {}, clientIp: {}", 
                userScopes, route.getId(), requestPath, requestId, clientIp);
        
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
