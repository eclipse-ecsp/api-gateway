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

package org.eclipse.ecsp.gateway.plugins;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtVisitor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter;
import org.eclipse.ecsp.gateway.plugins.filters.RequestBodyFilter;
import org.eclipse.ecsp.gateway.plugins.filters.RequestBodyFilter.Config;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.JwtPublicKeyLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.openapi4j.schema.validator.v3.SchemaValidator;
import org.reactivestreams.Publisher;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Test class for JwtAuthValidator.
 */
@ExtendWith(SpringExtension.class)
@ConfigurationProperties(prefix = "jwt")
@SuppressWarnings("checkstyle:MethodLength")
class JwtAuthValidatorTest {
    private static final String BEARER_TOKEN = "Bearer eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.jYW04zLDHfR1v7xdrW3lCGZrMIsVe0vWCfVkN2DRns2c3MN-mcp_-RE6TN9umSBYoNV-mnb31wFf8iun3fB6aDS6m_OXAiURVEKrPFNGlR38JSHUtsFzqTOj-wFrJZN4RwvZnNGSMvK3wzzUriZqmiNLsG8lktlEn6KA4kYVaM61_NpmPHWAjGExWv7cjHYupcjMSmR8uMTwN5UuAwgW6FRstCJEfoxwb0WKiyoaSlDuIiHZJ0cyGhhEmmAPiCwtPAwGeaL1yZMcp0p82cpTQ5Qb-7CtRov3N4DcOHgWYk6LomPR5j5cCkePAz87duqyzSMpCB0mCOuE3CU2VMtGeQ";

    private static final Long START_DATE = 1683811748923L;
    public static final int TWO = 2;
    public Map<String, JwtParser> jwtParsers = new LinkedHashMap<>();
    @InjectMocks
    JwtAuthFilter jwtAuthFilterWithInvalidScope;
    @Getter
    @Setter
    Map<String, Map<String, String>> tokenHeaderValidatorConfig;
    Route route = Mockito.mock(Route.class);
    String userIdField;
    ServerWebExchangeImpl serverWebExchangeImpl = new ServerWebExchangeImpl();
    InvalidServerWebExchangeImplMock invalidServerWebExchangeImplMock = new InvalidServerWebExchangeImplMock();
    GatewayFilterChain gatewayFilterChain = exchange -> {
        GatewayFilterChain chain = Mockito.mock(GatewayFilterChain.class);
        return chain.filter(exchange);
    };
    @InjectMocks
    private JwtAuthValidator jwtAuthValidator;
    @InjectMocks
    private JwtPublicKeyLoader jwtPublicKeyLoader;
    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;
    @InjectMocks
    private RequestBodyValidator requestBodyValidator;
    @InjectMocks
    private RequestBodyFilter requestBodyFilter;
    @InjectMocks
    private AccessLog accessLog;

    @Test
    void testInvalidPublicKeyFile() {
        ReflectionTestUtils.setField(jwtPublicKeyLoader, "jwtPublicKeyFiles",
                new String[]{"non-existing-file.pem", "error-pem-file.key", "wrong-public-key-file.pem",
                    "wrong-certificate-file.pem"});
        ReflectionTestUtils.setField(jwtAuthValidator, "jwtPublicKeyLoader", jwtPublicKeyLoader);
        Assertions.assertDoesNotThrow(() -> jwtPublicKeyLoader.init());
    }

    @BeforeEach
    void loadPublicKeySignature() throws IOException {
        ReflectionTestUtils.setField(jwtAuthValidator, "userIdField", "admin");
        ReflectionTestUtils.setField(jwtPublicKeyLoader, "jwtPublicKeyFilePath", "./src/test/resources/");
        ReflectionTestUtils.setField(jwtPublicKeyLoader, "jwtPublicKeyFiles",
                new String[]{"test-certificate.pem", "test-public-key.pem", "poc-public.key"});
        ReflectionTestUtils.setField(jwtAuthValidator, "jwtPublicKeyLoader", jwtPublicKeyLoader);
        jwtPublicKeyLoader.init();
        this.jwtParsers = jwtPublicKeyLoader.getJwtParsers();
    }

    @Test
    void tokenValidationScenariosTest() throws Exception {
        tokenHeaderValidatorConfig = new HashMap<>();
        Map<String, String> subjectHeaderValidationConfig = new HashMap<>();
        subjectHeaderValidationConfig.put("required", "true");
        subjectHeaderValidationConfig.put("regex", "^[a-zA-Z0-9]+$");
        this.tokenHeaderValidatorConfig.put("sub", subjectHeaderValidationConfig);
        Map<String, String> audHeaderValidationConfig = new HashMap<>();
        audHeaderValidationConfig.put("required", "false");
        audHeaderValidationConfig.put("regex", "^[a-zA-Z0-9]+$");
        this.tokenHeaderValidatorConfig.put("aud", audHeaderValidationConfig);
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, this.jwtParsers, this.tokenHeaderValidatorConfig, userIdField);

        // Invalid scope config
        JwtAuthFilter.Config invalidConfig = new JwtAuthFilter.Config();
        invalidConfig.setScope("InvalidScope");
        jwtAuthValidator.apply(invalidConfig);
        jwtAuthFilterWithInvalidScope =
                new JwtAuthFilter(invalidConfig, this.jwtParsers, this.tokenHeaderValidatorConfig, this.userIdField);

        Method privateMethod = JwtAuthFilter.class.getDeclaredMethod("validate", String.class);
        privateMethod.setAccessible(true);
        try {
            JwtParser jwtParser = Mockito.mock(JwtParser.class);
            Jws<Claims> jws = getClaims();
            // Null token validation test
            when(jwtParser.parseSignedClaims(null)).thenReturn(jws);
            // Invalid User test
            jws.getPayload().put("sub", null);
            // Invalid Scope test
            when(jwtParser.parseSignedClaims(Mockito.anyString())).thenReturn(jws);
            // Token Validation Test
            this.jwtParsers.put("test-certificate.pem", null);
            when(jwtParser.parseSignedClaims(Mockito.anyString())).thenReturn(jws);
            ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class, () -> jwtAuthFilter.filter(serverWebExchangeImpl, gatewayFilterChain));
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
            Assertions.assertEquals("Token verification failed", exception.getMessage());
            jws.getPayload().put("sub", "admin");
            this.jwtParsers.put("test-certificate.pem", jwtParser);
            when(jwtParser.parseSignedClaims(Mockito.anyString())).thenReturn(jws);
            jwtAuthFilter.filter(serverWebExchangeImpl, gatewayFilterChain);
            // Invalid scope test-execution
            when(jwtParser.parseSignedClaims(Mockito.anyString())).thenReturn(jws);
            ApiGatewayException insufficientScopeException = Assertions.assertThrows(ApiGatewayException.class, () -> jwtAuthFilterWithInvalidScope.filter(serverWebExchangeImpl, gatewayFilterChain));
            Assertions.assertEquals(HttpStatus.NOT_FOUND, insufficientScopeException.getStatusCode());
            Assertions.assertEquals("Request not found", insufficientScopeException.getMessage());
            ApiGatewayException invalidTokenException = Assertions.assertThrows(ApiGatewayException.class, () -> jwtAuthFilterWithInvalidScope.filter(invalidServerWebExchangeImplMock, gatewayFilterChain));
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, invalidTokenException.getStatusCode());
            Assertions.assertEquals("Invalid Token", invalidTokenException.getMessage());
            ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validate", " ");
            jwtAuthValidator.setTokenHeaderValidationConfig(jwtAuthValidator.getTokenHeaderValidationConfig());
        } catch (UndeclaredThrowableException ex) {
            Assertions.assertEquals(IllegalAccessException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testTokenVerificationFailed() {
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        this.jwtParsers.clear();
        jwtAuthFilter = new JwtAuthFilter(config, this.jwtParsers, this.tokenHeaderValidatorConfig, this.userIdField);
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class, () -> jwtAuthFilter.filter(serverWebExchangeImpl, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertEquals("Token verification failed", exception.getMessage());
    }

    @Test
    void testInvalidToken() {
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        ServerWebExchangeImpl mockedExchange = Mockito.spy(serverWebExchangeImpl);
        ServerHttpRequest mockedRequest = Mockito.mock(ServerHttpRequest.class);
        Mockito.when(mockedExchange.getRequest()).thenReturn(mockedRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer InvalidToken");
        doReturn(headers).when(mockedRequest).getHeaders();
        jwtAuthFilter = new JwtAuthFilter(config, this.jwtParsers, this.tokenHeaderValidatorConfig, this.userIdField);
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class, () -> jwtAuthFilter.filter(mockedExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertEquals("Token verification failed", exception.getMessage());
    }

    @Test
    void testPrivateMethodValidateScope() throws Exception {
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, this.jwtParsers, this.tokenHeaderValidatorConfig, this.userIdField);
        ClaimImpl claims = new ClaimImpl();
        claims.setScope("SelfManage");
        Method privateMethod = JwtAuthFilter.class.getDeclaredMethod("validateScope", Route.class, Claims.class);
        privateMethod.setAccessible(true);
        ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims);
        List<String> scopesList = Arrays.asList("SelfManage", "IgniteSystem");
        claims.setScope(scopesList);
        ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims);
        claims.setScope(String.join(",", scopesList));
        String result = ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims);
        Assertions.assertTrue(StringUtils.contains(result, "SelfManage"));
    }

    @Test
    void testInvalidTokenHeader() throws Exception {
        tokenHeaderValidatorConfig = new HashMap<>();
        Map<String, String> invalidHeaderValidationConfig = new HashMap<>();
        invalidHeaderValidationConfig.put("required", "true");
        invalidHeaderValidationConfig.put("regex", "^[a-zA-Z0-9]+$");
        this.tokenHeaderValidatorConfig.put("invalidHeader", invalidHeaderValidationConfig);
        Map<String, String> invalidRegexValidationConfig = new HashMap<>();
        invalidRegexValidationConfig.put("required", "true");
        invalidRegexValidationConfig.put("regex",
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        this.tokenHeaderValidatorConfig.put("aud", invalidRegexValidationConfig);
        Map<String, String> invalidRegexTest = new HashMap<>();
        invalidRegexTest.put("required", "true");
        invalidRegexTest.put("regex", "?[^a-zA-Z0-9]");
        this.tokenHeaderValidatorConfig.put("aud", invalidRegexTest);
        Map<String, String> nullHeaderValidatorConfig = new HashMap<>();
        nullHeaderValidatorConfig.put("required", "true");
        nullHeaderValidatorConfig.put("regex",
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, this.jwtParsers, this.tokenHeaderValidatorConfig, userIdField);
        // Invalid scope config
        JwtAuthFilter.Config invalidConfig = new JwtAuthFilter.Config();
        invalidConfig.setScope("InvalidScope");
        jwtAuthValidator.apply(invalidConfig);
        jwtAuthFilterWithInvalidScope =
                new JwtAuthFilter(invalidConfig, this.jwtParsers, this.tokenHeaderValidatorConfig, this.userIdField);

        Method privateMethod = JwtAuthFilter.class.getDeclaredMethod("validate", String.class);
        privateMethod.setAccessible(true);
        try {
            JwtParser jwtParser = Mockito.mock(JwtParser.class);
            Jws<Claims> jws = getClaims();
            // Null token validation test
            when(jwtParser.parseSignedClaims(null)).thenReturn(jws);
            // Invalid User test
            jws.getPayload().put("aud", null);
            // Invalid Scope test
            when(jwtParser.parseSignedClaims(Mockito.anyString())).thenReturn(jws);
            // Token Validation Test
            this.tokenHeaderValidatorConfig.put("aud", nullHeaderValidatorConfig);
            this.jwtParsers.put("test-certificate.pem", null);
            when(jwtParser.parseSignedClaims(Mockito.anyString())).thenReturn(jws);
            ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class, () -> jwtAuthFilter.filter(serverWebExchangeImpl, gatewayFilterChain));
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
            Assertions.assertEquals("Token verification failed", exception.getMessage());
            jws.getPayload().put("sub", "admin");
            this.jwtParsers.put("test-certificate.pem", jwtParser);
            when(jwtParser.parseSignedClaims(Mockito.anyString())).thenReturn(jws);
            ApiGatewayException tokenValidationEx = Assertions.assertThrows(ApiGatewayException.class, () -> jwtAuthFilter.filter(serverWebExchangeImpl, gatewayFilterChain));
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, tokenValidationEx.getStatusCode());
            Assertions.assertEquals("Token verification failed", tokenValidationEx.getMessage());
            // Invalid scope test-execution
            when(jwtParser.parseSignedClaims(Mockito.anyString())).thenReturn(jws);
            ApiGatewayException insufficientAccessEx = Assertions.assertThrows(ApiGatewayException.class, () -> jwtAuthFilterWithInvalidScope.filter(serverWebExchangeImpl, gatewayFilterChain));
            Assertions.assertEquals(HttpStatus.NOT_FOUND, insufficientAccessEx.getStatusCode());
            Assertions.assertEquals("Request not found", insufficientAccessEx.getMessage());
            ApiGatewayException invalidTokenEx = Assertions.assertThrows(ApiGatewayException.class, () -> jwtAuthFilterWithInvalidScope.filter(invalidServerWebExchangeImplMock, gatewayFilterChain));
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, invalidTokenEx.getStatusCode());
            Assertions.assertEquals("Invalid Token", invalidTokenEx.getMessage());
            ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validate", " ");
        } catch (RestClientResponseException ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        } catch (UndeclaredThrowableException ex) {
            Assertions.assertEquals(IllegalAccessException.class, ex.getCause().getClass());
        }
    }

    /**
     * creates and returns the Claims Object.
     *
     * @return Jws claims
     */
    public Jws<Claims> getClaims() {
        return new Jws<Claims>() {
            @Override
            public byte[] getDigest() {
                return new byte[0];
            }

            @Override
            public JwsHeader getHeader() {
                return null;
            }

            @Override
            public Claims getBody() {
                return getPayload();
            }

            @Override
            public Claims getPayload() {
                ClaimImpl claims = new ClaimImpl();
                claims.setScope("SelfManage");
                return claims;
            }

            @Override
            public <T> T accept(JwtVisitor<T> jwtVisitor) {
                return jwtVisitor.visit(this);
            }

            @Override
            public String getSignature() {
                return null;
            }
        };
    }

    @Test
    void testRequestBodyFilter() {
        requestBodyValidator.apply(new Config());
        accessLog = new AccessLog();
        when(route.getMetadata()).thenReturn(null);
        GatewayFilterChain mockedGatewayFilterChain = Mockito.mock(GatewayFilterChain.class);
        requestBodyFilter.filter(serverWebExchangeImpl, mockedGatewayFilterChain);
        Mockito.verify(mockedGatewayFilterChain, Mockito.times(1)).filter(serverWebExchangeImpl);

        serverWebExchangeImpl.getAttributes(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        Map<String, Object> metadataMap = new HashMap<>();
        SchemaValidator schemaValidator = Mockito.mock(SchemaValidator.class);
        metadataMap.put(GatewayConstants.SCHEMA_VALIDATOR, schemaValidator);
        when(route.getMetadata()).thenReturn(metadataMap);
        requestBodyFilter.filter(serverWebExchangeImpl, mockedGatewayFilterChain);
        Mockito.verify(mockedGatewayFilterChain, Mockito.times(TWO)).filter(serverWebExchangeImpl);
        requestBodyFilter.filter(serverWebExchangeImpl, mockedGatewayFilterChain);
        when(route.getMetadata()).thenThrow(new RuntimeException("Mocked Exception"));
        try {
            requestBodyFilter.filter(Mockito.mock(ServerWebExchange.class), mockedGatewayFilterChain);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static class ClaimImpl implements Claims {

        private Object scope;

        public void setScope(Object scope) {
            this.scope = scope;
        }

        @Override
        public String getIssuer() {
            return "http://localhost:443/oauth2/token";
        }

        @SuppressWarnings("unused")
        public Claims setIssuer(String iss) {
            return null;
        }

        @Override
        public String getSubject() {
            return "admin";
        }

        @SuppressWarnings("unused")
        public Claims setSubject(String sub) {
            return null;
        }

        @Override
        public Set<String> getAudience() {
            return Set.of("GO7ZgKKVxJgejMkb_NR0GCKAr3wa");
        }

        @SuppressWarnings("unused")
        public Claims setAudience(String aud) {
            return null;
        }

        @Override
        public Date getExpiration() {
            return new Date(START_DATE);
        }

        @SuppressWarnings("unused")
        public Claims setExpiration(Date exp) {
            return null;
        }

        @Override
        public Date getNotBefore() {
            return new Date(START_DATE);
        }

        @SuppressWarnings("unused")
        public Claims setNotBefore(Date nbf) {
            return null;
        }

        @Override
        public Date getIssuedAt() {
            return new Date(START_DATE);
        }

        @SuppressWarnings("unused")
        public Claims setIssuedAt(Date iat) {
            return null;
        }

        @Override
        public String getId() {
            return "ea8cef5b-fd49-439e-b63f-bdb56e9f638d";
        }

        @SuppressWarnings("unused")
        public Claims setId(String jti) {
            return null;
        }

        @Override
        public <T> T get(String claimName, Class<T> requiredType) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
        @Override
        public Object get(Object key) {
            return this.scope;
        }

        @Override
        public Object put(String key, Object value) {
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            // No implementation needed
        }

        @Override
        public void clear() {
            // No implementation needed
        }

        @Override
        public Set<String> keySet() {
            Set<String> claimsKeySet = new HashSet<>();
            claimsKeySet.add("sub");
            claimsKeySet.add("aud");
            return claimsKeySet;
        }

        @Override
        public Collection<Object> values() {
            return null;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return null;
        }
    }

    class InvalidServerWebExchangeImplMock implements ServerWebExchange {

        @Override
        public ServerHttpRequest getRequest() {
            return new ServerHttpRequest() {
                @Override
                public String getId() {
                    return null;
                }

                @Override
                public RequestPath getPath() {
                    return new RequestPath() {
                        @Override
                        public PathContainer contextPath() {
                            return null;
                        }

                        @Override
                        public PathContainer pathWithinApplication() {
                            return null;
                        }

                        @Override
                        public RequestPath modifyContextPath(String contextPath) {
                            return null;
                        }

                        @Override
                        public String value() {
                            return null;
                        }

                        @Override
                        public List<Element> elements() {
                            return null;
                        }
                    };
                }

                @Override
                public MultiValueMap<String, String> getQueryParams() {
                    return null;
                }

                @Override
                public MultiValueMap<String, HttpCookie> getCookies() {
                    return null;
                }

                @Override
                public HttpMethod getMethod() {
                    return HttpMethod.POST;
                }

                @Override
                public URI getURI() {
                    try {
                        return URI.create("https://v1/vehicleType");
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                public Map<String, Object> getAttributes() {
                    return Map.of();
                }

                @Override
                public Flux<DataBuffer> getBody() {
                    return null;
                }

                @Override
                public HttpHeaders getHeaders() {
                    MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
                    headerMap.put("Authorization", List.of(""));
                    return new HttpHeaders(headerMap);
                }
            };
        }

        @Override
        public ServerHttpResponse getResponse() {
            return new ServerHttpResponse() {
                @Override
                public HttpHeaders getHeaders() {
                    return null;
                }

                @Override
                public DataBufferFactory bufferFactory() {
                    return null;
                }

                @Override
                public void beforeCommit(Supplier<? extends Mono<Void>> action) {
                    // No implementation needed
                }

                @Override
                public boolean isCommitted() {
                    return false;
                }

                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    return null;
                }

                @Override
                public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                    return null;
                }

                @Override
                public Mono<Void> setComplete() {
                    return null;
                }

                @Override
                public boolean setStatusCode(HttpStatusCode status) {
                    return true;
                }

                @Override
                public HttpStatusCode getStatusCode() {
                    return HttpStatus.ACCEPTED;
                }

                @Override
                public MultiValueMap<String, ResponseCookie> getCookies() {
                    return null;
                }

                @Override
                public void addCookie(ResponseCookie cookie) {
                    // No implementation needed
                }
            };
        }

        @Override
        public Map<String, Object> getAttributes() {
            Map<String, Object> attributeMap = new HashMap<>();
            attributeMap.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
            attributeMap.put(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, "{}");
            return attributeMap;
        }

        @Override
        public Mono<WebSession> getSession() {
            return null;
        }

        @Override
        public <T extends Principal> Mono<T> getPrincipal() {
            return null;
        }

        @Override
        public Mono<MultiValueMap<String, String>> getFormData() {
            return null;
        }

        @Override
        public Mono<MultiValueMap<String, Part>> getMultipartData() {
            return null;
        }

        @Override
        public LocaleContext getLocaleContext() {
            return null;
        }

        @Override
        public ApplicationContext getApplicationContext() {
            return null;
        }

        @Override
        public boolean isNotModified() {
            return false;
        }

        @Override
        public boolean checkNotModified(Instant lastModified) {
            return false;
        }

        @Override
        public boolean checkNotModified(String etag) {
            return false;
        }

        @Override
        public boolean checkNotModified(String etag, Instant lastModified) {
            return false;
        }

        @Override
        public String transformUrl(String url) {
            return null;
        }

        @Override
        public void addUrlTransformer(Function<String, String> transformer) {
            // No implementation needed
        }

        @Override
        public String getLogPrefix() {
            return null;
        }

        @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
        public void getAttributes(String gatewayRouteAttr) {
            // No implementation needed
        }
    }

    class ServerWebExchangeImpl implements ServerWebExchange {

        @Override
        public ServerHttpRequest getRequest() {
            return new ServerHttpRequest() {
                @Override
                public String getId() {
                    return null;
                }

                @Override
                public RequestPath getPath() {
                    return new RequestPath() {
                        @Override
                        public PathContainer contextPath() {
                            return Mockito.mock(PathContainer.class);
                        }

                        @Override
                        public PathContainer pathWithinApplication() {
                            return null;
                        }

                        @Override
                        public RequestPath modifyContextPath(String contextPath) {
                            return null;
                        }

                        @Override
                        public String value() {
                            return null;
                        }

                        @Override
                        public List<Element> elements() {
                            return null;
                        }
                    };

                }

                @Override
                public MultiValueMap<String, String> getQueryParams() {
                    return null;
                }

                @Override
                public MultiValueMap<String, HttpCookie> getCookies() {
                    return null;
                }

                @Override
                public HttpMethod getMethod() {
                    return HttpMethod.POST;
                }

                @Override
                public URI getURI() {
                    try {
                        return URI.create("https://v1/vehicleType");
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                public Map<String, Object> getAttributes() {
                    return Map.of();
                }

                @Override
                public Flux<DataBuffer> getBody() {
                    return null;
                }

                @Override
                public HttpHeaders getHeaders() {
                    MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
                    headerMap.put("Authorization", List.of(BEARER_TOKEN));
                    return new HttpHeaders(headerMap);
                }
            };
        }

        @Override
        public ServerHttpResponse getResponse() {
            return new ServerHttpResponse() {
                @Override
                public HttpHeaders getHeaders() {
                    return null;
                }

                @Override
                public DataBufferFactory bufferFactory() {
                    return null;
                }

                @Override
                public void beforeCommit(Supplier<? extends Mono<Void>> action) {
                    // No implementation needed
                }

                @Override
                public boolean isCommitted() {
                    return false;
                }

                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    return null;
                }

                @Override
                public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                    return null;
                }

                @Override
                public Mono<Void> setComplete() {
                    return null;
                }

                @Override
                public boolean setStatusCode(HttpStatusCode status) {
                    return true;
                }

                @Override
                public HttpStatusCode getStatusCode() {
                    return HttpStatus.ACCEPTED;
                }

                @Override
                public MultiValueMap<String, ResponseCookie> getCookies() {
                    return null;
                }

                @Override
                public void addCookie(ResponseCookie cookie) {
                    // No implementation needed
                }
            };
        }

        @Override
        public Map<String, Object> getAttributes() {
            Map<String, Object> attributeMap = new HashMap<>();
            attributeMap.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
            attributeMap.put(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, "{}");
            return attributeMap;
        }

        @Override
        public Mono<WebSession> getSession() {
            return null;
        }

        @Override
        public <T extends Principal> Mono<T> getPrincipal() {
            return null;
        }

        @Override
        public Mono<MultiValueMap<String, String>> getFormData() {
            return null;
        }

        @Override
        public Mono<MultiValueMap<String, Part>> getMultipartData() {
            return null;
        }

        @Override
        public LocaleContext getLocaleContext() {
            return null;
        }

        @Override
        public ApplicationContext getApplicationContext() {
            return null;
        }

        @Override
        public boolean isNotModified() {
            return false;
        }

        @Override
        public boolean checkNotModified(Instant lastModified) {
            return false;
        }

        @Override
        public boolean checkNotModified(String etag) {
            return false;
        }

        @Override
        public boolean checkNotModified(String etag, Instant lastModified) {
            return false;
        }

        @Override
        public String transformUrl(String url) {
            return null;
        }

        @Override
        public void addUrlTransformer(Function<String, String> transformer) {
            // No implementation needed
        }

        @Override
        public String getLogPrefix() {
            return null;
        }

        @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
        public void getAttributes(String gatewayRouteAttr) {
            // No implementation needed
        }
    }
}
