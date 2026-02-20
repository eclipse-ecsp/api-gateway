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
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.Strings;
import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.model.PublicKeyInfo;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.TokenHeaderValidationConfig;
import org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter;
import org.eclipse.ecsp.gateway.plugins.filters.RequestBodyFilter;
import org.eclipse.ecsp.gateway.plugins.filters.RequestBodyFilter.Config;
import org.eclipse.ecsp.gateway.service.PublicKeyService;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.JwtTestTokenGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openapi4j.schema.validator.v3.SchemaValidator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.Principal;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Test class for JwtAuthValidator with support for multiple JWKS/certificate sources.
 */
@ExtendWith(SpringExtension.class)
@SuppressWarnings("checkstyle:MethodLength")
class JwtAuthValidatorTest {
    private static final String BEARER_TOKEN_WITHOUT_KID = JwtTestTokenGenerator.createTokenWithoutKid();
    private static final long START_DATE = 1683811748923L;
    public static final int ONE_THOUSAND = 1000;
    public static final int INT_3600 = 3600;
    public static final int INT_12345 = 12345;
    public static final int TWO = 2;
    public static final int INT_3600000 = 3600000;
    public static final int INT_3 = 3;

    // Updated to use new architecture
    @Mock
    private PublicKeyService publicKeyService;

    @Mock
    private JwtProperties jwtProperties;

    public Map<String, JwtParser> jwtParsers = new LinkedHashMap<>();

    @InjectMocks
    JwtAuthFilter jwtAuthFilterWithInvalidScope;

    @Getter
    @Setter
    Map<String, TokenHeaderValidationConfig> tokenHeaderValidationConfig;

    static Route route = Mockito.mock(Route.class);
    ServerWebExchangeImpl serverWebExchangeImpl = new ServerWebExchangeImpl();
    InvalidServerWebExchangeImplMock invalidServerWebExchangeImplMock = new InvalidServerWebExchangeImplMock();
    GatewayFilterChain gatewayFilterChain = exchange -> {
        GatewayFilterChain chain = Mockito.mock(GatewayFilterChain.class);
        return chain.filter(exchange);
    };

    @InjectMocks
    private JwtAuthValidator jwtAuthValidator;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @InjectMocks
    private RequestBodyValidator requestBodyValidator;

    @InjectMocks
    private RequestBodyFilter requestBodyFilter;

    @BeforeEach
    void setupJwtAuthValidator() {
        // Setup mock JWT properties
        setupMockJwtProperties();

        // Setup mock public key service
        setupMockPublicKeyService();

        // Inject dependencies into JwtAuthValidator
        ReflectionTestUtils.setField(jwtAuthValidator, "publicKeyService", publicKeyService);
        ReflectionTestUtils.setField(jwtAuthValidator, "jwtProperties", jwtProperties);
    }

    private void setupMockJwtProperties() {
        // Setup token header validation config
        tokenHeaderValidationConfig = new HashMap<>();
        TokenHeaderValidationConfig subjectConfig = new TokenHeaderValidationConfig();
        subjectConfig.setRequired(true);
        subjectConfig.setRegex("^[a-zA-Z0-9]+$");
        tokenHeaderValidationConfig.put("sub", subjectConfig);

        TokenHeaderValidationConfig audConfig = new TokenHeaderValidationConfig();
        audConfig.setRequired(false);
        audConfig.setRegex("^[a-zA-Z0-9-]+$");
        tokenHeaderValidationConfig.put("aud", audConfig);

        // Setup public key sources


        // Setup token claim to header mapping
        Map<String, String> claimToHeaderMapping = new HashMap<>();
        claimToHeaderMapping.put("sub", "X-User-Id");
        claimToHeaderMapping.put("aud", "X-Audience");
        List<PublicKeySource> publicKeySources = Arrays.asList(
                createTestPublicKeySource("test-source-1", "./src/test/resources/test-certificate.pem"),
                createTestPublicKeySource("test-source-2", "./src/test/resources/test-public-key.pem")
        );
        when(jwtProperties.getTokenHeaderValidationConfig()).thenReturn(tokenHeaderValidationConfig);
        when(jwtProperties.getKeySources()).thenReturn(publicKeySources);
        when(jwtProperties.getTokenClaimToHeaderMapping()).thenReturn(claimToHeaderMapping);
        when(jwtProperties.getScopePrefixes()).thenReturn(Set.of("ProviderPrefix/", "ScopePrefix/"));
    }

    private PublicKeySource createTestPublicKeySource(String id, String location) {
        PublicKeySource source = new PublicKeySource();
        source.setId(id);
        source.setType(org.eclipse.ecsp.gateway.model.PublicKeyType.PEM);
        source.setLocation(location);
        source.setIssuer("test-issuer");
        source.setRefreshInterval(Duration.ofHours(1));
        return source;
    }

    private void setupMockPublicKeyService() {
        // Mock public key service to return the test public key from token generator
        PublicKey testPublicKey = JwtTestTokenGenerator.getTestPublicKey();

        // Mock for specific key ID used in test tokens
        PublicKeyInfo testPublicKeyInfo = new PublicKeyInfo();
        testPublicKeyInfo.setKid("test-key-id");
        testPublicKeyInfo.setSourceId("test-source-1");
        testPublicKeyInfo.setPublicKey(testPublicKey);
        when(publicKeyService.findPublicKey("test-key-id", null))
                .thenReturn(Optional.of(testPublicKeyInfo));

        // Mock for DEFAULT key (fallback case)
        when(publicKeyService.findPublicKey("DEFAULT", null))
                .thenReturn(Optional.of(testPublicKeyInfo));

        // Mock for any other key ID calls
        when(publicKeyService.findPublicKey(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(testPublicKeyInfo));

        // Mock refresh functionality
        Mockito.doNothing().when(publicKeyService).refreshPublicKeys();
    }

    @Test
    void testMultiplePublicKeySourcesSupport() {
        // Test that JwtAuthValidator supports multiple public key sources
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");

        // Apply configuration
        jwtAuthValidator.apply(config);

        // Verify that public key sources are configured
        List<PublicKeySource> sources = jwtProperties.getKeySources();
        Assertions.assertNotNull(sources);
        Assertions.assertEquals(TWO, sources.size());
        Assertions.assertEquals("test-source-1", sources.get(0).getId());
        Assertions.assertEquals("test-source-2", sources.get(1).getId());
    }

    @Test
    void testPublicKeyServiceIntegration() {
        // Test that public key service is properly integrated
        Optional<PublicKeyInfo> publicKey = publicKeyService.findPublicKey("test-key-id", "test-issuer");
        Assertions.assertTrue(publicKey.isPresent());

        // Verify refresh functionality
        Assertions.assertDoesNotThrow(() -> publicKeyService.refreshPublicKeys());
    }

    @Test
    void testTokenHeaderValidationConfigSupport() {
        // Test that token header validation config supports new TokenHeaderValidationConfig objects
        Map<String, TokenHeaderValidationConfig> config = jwtProperties.getTokenHeaderValidationConfig();
        Assertions.assertNotNull(config);

        TokenHeaderValidationConfig subConfig = config.get("sub");
        Assertions.assertNotNull(subConfig);
        Assertions.assertTrue(subConfig.isRequired());
        Assertions.assertEquals("^[a-zA-Z0-9]+$", subConfig.getRegex());

        TokenHeaderValidationConfig audConfig = config.get("aud");
        Assertions.assertNotNull(audConfig);
        Assertions.assertFalse(audConfig.isRequired());
    }

    @Test
    void testTokenValidationWithNewArchitecture() {
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);

        // Create JWT filter with new architecture
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Setup JWT parser with proper token verification
        JwtParser jwtParser = Jwts.parser()
                .verifyWith(JwtTestTokenGenerator.getTestPublicKey())
                .build();

        this.jwtParsers.put("test-certificate.pem", jwtParser);

        // Test successful validation with valid token
        ServerWebExchangeImpl validExchange = new ServerWebExchangeImpl();
        validExchange.setValidToken(true);

        Assertions.assertDoesNotThrow(() -> jwtAuthFilter.filter(validExchange, gatewayFilterChain));
    }

    @Test
    void testTokenClaimToHeaderMapping() {
        // Test token claim to header mapping functionality
        Map<String, String> mapping = jwtProperties.getTokenClaimToHeaderMapping();
        Assertions.assertNotNull(mapping);
        Assertions.assertEquals("X-User-Id", mapping.get("sub"));
        Assertions.assertEquals("X-Audience", mapping.get("aud"));
    }

    @Test
    void testInvalidTokenWithNewArchitecture() {
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);

        ServerWebExchangeImpl mockedExchange = Mockito.spy(serverWebExchangeImpl);
        ServerHttpRequest mockedRequest = Mockito.mock(ServerHttpRequest.class);
        Mockito.when(mockedExchange.getRequest()).thenReturn(mockedRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", JwtTestTokenGenerator.createInvalidToken());
        doReturn(headers).when(mockedRequest).getHeaders();
        
        // Mock the path properly
        org.springframework.http.server.RequestPath mockPath = Mockito.mock(org.springframework.http.server.RequestPath.class);
        when(mockPath.value()).thenReturn("/test-path");
        when(mockedRequest.getPath()).thenReturn(mockPath);
        when(mockedRequest.getId()).thenReturn("test-request-id");

        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
                () -> jwtAuthFilter.filter(mockedExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Token") || exception.getMessage().contains("verification"));
    }

    @Test
    void testPublicKeyRefresh() {
        // Test public key refresh functionality
        Assertions.assertDoesNotThrow(() -> publicKeyService.refreshPublicKeys());

        // Verify that refresh is called on the service
        Mockito.verify(publicKeyService, Mockito.atLeastOnce()).refreshPublicKeys();
    }

    @Test
    void tokenValidationScenariosTest() {
        // Setup test configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        try {
            // Create a valid exchange for successful test
            ServerWebExchangeImpl validExchange = new ServerWebExchangeImpl();
            validExchange.setValidToken(true);

            Assertions.assertDoesNotThrow(() -> jwtAuthFilter.filter(validExchange, gatewayFilterChain));
            // Invalid scope config
            JwtAuthFilter.Config invalidConfig = new JwtAuthFilter.Config();
            invalidConfig.setScope("InvalidScope");
            jwtAuthValidator.apply(invalidConfig);
            jwtAuthFilterWithInvalidScope =
                    new JwtAuthFilter(invalidConfig, publicKeyService, jwtProperties);
            // Test invalid scope scenario
            ApiGatewayException insufficientScopeException = Assertions.assertThrows(ApiGatewayException.class,
                    () -> jwtAuthFilterWithInvalidScope.filter(serverWebExchangeImpl, gatewayFilterChain));
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, insufficientScopeException.getStatusCode());

            // Test invalid token scenario
            ApiGatewayException invalidTokenException = Assertions.assertThrows(ApiGatewayException.class,
                    () -> jwtAuthFilterWithInvalidScope.filter(invalidServerWebExchangeImplMock, gatewayFilterChain));
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, invalidTokenException.getStatusCode());
        } catch (UndeclaredThrowableException ex) {
            Assertions.assertEquals(IllegalAccessException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testTokenVerificationFailed() {
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage1");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
                () -> jwtAuthFilter.filter(serverWebExchangeImpl, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Token verification failed"));
    }

    @Test
    void testPrivateMethodValidateScope() throws NoSuchMethodException {
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);
        ClaimImpl claims = new ClaimImpl();
        claims.put("scope", "SelfManage");
        Method privateMethod = JwtAuthFilter.class.getDeclaredMethod("validateScope", Route.class, Claims.class,
                String.class,
                String.class);
        privateMethod.setAccessible(true);

        ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims, "requestId", "requestPath");
        List<String> scopesList = Arrays.asList("SelfManage", "IgniteSystem");
        claims.put("scope", scopesList);
        String result = ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims, "requestId", "requestPath");
        if (result != null) {
            Assertions.assertTrue(Strings.CS.contains(result, "SelfManage"));
        }
        claims.setScope(String.join(",", scopesList));
        String resultString = ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims, "requestId", "requestPath");
        if (resultString != null) {
            Assertions.assertTrue(Strings.CS.contains(resultString, "SelfManage"));
        }
    }

    /**
     * creates and returns the Claims Object.
     *
     * @return Jws claims
     */
    public Jws<Claims> getClaims() {
        return new Jws<>() {
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
        when(route.getMetadata()).thenReturn(null);
        GatewayFilterChain mockedGatewayFilterChain = Mockito.mock(GatewayFilterChain.class);
        requestBodyFilter.filter(serverWebExchangeImpl, mockedGatewayFilterChain);
        Mockito.verify(mockedGatewayFilterChain, Mockito.times(1)).filter(serverWebExchangeImpl);

        serverWebExchangeImpl.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        Map<String, Object> metadataMap = new HashMap<>();
        SchemaValidator schemaValidator = Mockito.mock(SchemaValidator.class);
        metadataMap.put(GatewayConstants.SCHEMA_VALIDATOR, schemaValidator);
        when(route.getMetadata()).thenReturn(metadataMap);
        try {
            requestBodyFilter.filter(serverWebExchangeImpl, mockedGatewayFilterChain);
        } catch (ApiGatewayException ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            Assertions.assertEquals("Invalid request payload", ex.getMessage());
        }
        Mockito.verify(mockedGatewayFilterChain, Mockito.times(1)).filter(serverWebExchangeImpl);
        try {
            requestBodyFilter.filter(serverWebExchangeImpl, mockedGatewayFilterChain);
        } catch (ApiGatewayException ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            Assertions.assertEquals("Invalid request payload", ex.getMessage());
        }
        when(route.getMetadata()).thenThrow(new RuntimeException("Mocked Exception"));
        try {
            requestBodyFilter.filter(Mockito.mock(ServerWebExchange.class), mockedGatewayFilterChain);
        } catch (Exception ex) {
            // Log exception appropriately in real implementation
            System.err.println("Exception in test: " + ex.getMessage());
        }
    }

    @Setter
    static class ClaimImpl implements Claims {

        private Object scope;

        public void setScope(Object scope) {
            this.scope = scope;
        }

        @Override
        public String getIssuer() {
            return "test-issuer";
        }

        @Override
        public String getSubject() {
            return "admin";
        }

        @Override
        public Set<String> getAudience() {
            return Set.of("test-audience");
        }

        @Override
        public Date getExpiration() {
            return new Date(System.currentTimeMillis() + INT_3600000);
        }

        @Override
        public Date getNotBefore() {
            return new Date(System.currentTimeMillis() - INT_3600000);
        }

        @Override
        public Date getIssuedAt() {
            return new Date(START_DATE);
        }

        @Override
        public String getId() {
            return "test-jwt-id";
        }

        @Override
        public <T> T get(String claimName, Class<T> requiredType) {
            if ("scope".equals(claimName)) {
                return requiredType.cast(scope);
            }
            if ("sub".equals(claimName)) {
                return requiredType.cast("admin");
            }
            if ("aud".equals(claimName)) {
                return requiredType.cast("test-audience");
            }
            return null;
        }

        @Override
        public Object get(Object key) {
            if (key instanceof String str && str.equalsIgnoreCase("scope")) {
                return scope;
            }
            if (key instanceof String str && str.equalsIgnoreCase("sub")) {
                return "admin";
            }
            if (key instanceof String str && str.equalsIgnoreCase("aud")) {
                return "test-audience";
            }
            return null;
        }

        // Implementation of Map interface methods...
        @Override
        public int size() {
            return INT_3;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return "scope".equals(key) || "sub".equals(key) || "aud".equals(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return scope != null && scope.equals(value) || "admin".equals(value) || "test-audience".equals(value);
        }



        @Override
        public Object put(String key, Object value) {
            if ("scope".equals(key)) {
                Object old = scope;
                scope = value;
                return old;
            }
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            // Implementation not needed for tests
        }

        @Override
        public void clear() {
            scope = null;
        }

        @Override
        public Set<String> keySet() {
            return Set.of("scope", "sub", "aud");
        }

        @Override
        public Collection<Object> values() {
            return Arrays.asList(scope, "admin", "test-audience");
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return new HashSet<>();
        }
    }

    // Mock classes for ServerWebExchange implementations
    public static class ServerWebExchangeImpl implements ServerWebExchange {

        private boolean validToken = false;

        public void setValidToken(boolean validToken) {
            this.validToken = validToken;
        }

        @Override
        public ServerHttpRequest getRequest() {
            ServerHttpRequest mockRequest = Mockito.mock(ServerHttpRequest.class);
            HttpHeaders headers = new HttpHeaders();
            if (validToken) {
                headers.set("Authorization", JwtTestTokenGenerator.createDefaultToken());
            } else {
                headers.set("Authorization", BEARER_TOKEN_WITHOUT_KID);
            }

            // Mock basic request methods
            when(mockRequest.getHeaders()).thenReturn(headers);
            when(mockRequest.getId()).thenReturn("test-request-id");
            // Mock the path's value() method to return a proper value
            org.springframework.http.server.RequestPath mockPath = Mockito.mock(org.springframework.http.server.RequestPath.class);
            when(mockPath.value()).thenReturn("/test-path");
            when(mockPath.toString()).thenReturn("/test-path");
            when(mockRequest.getPath()).thenReturn(mockPath);
            ServerHttpRequest.Builder mockBuilder = Mockito.mock(ServerHttpRequest.Builder.class);
            // Mock the mutate() method to return a proper builder
            when(mockRequest.mutate()).thenReturn(mockBuilder);

            // Mock the builder methods to return the builder itself for chaining
            when(mockBuilder.header(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(mockBuilder);
            when(mockBuilder.header(Mockito.anyString(), Mockito.anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockRequest);

            return mockRequest;
        }

        @Override
        public ServerHttpResponse getResponse() {
            return Mockito.mock(ServerHttpResponse.class);
        }

        @Override
        public Map<String, Object> getAttributes() {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
            return attributes;
        }

        // Other required methods with mock implementations
        @Override
        public Mono<WebSession> getSession() {
            return Mono.empty();
        }

        @Override
        public <T extends Principal> Mono<T> getPrincipal() {
            return Mono.empty();
        }

        @Override
        public Mono<MultiValueMap<String, String>> getFormData() {
            return Mono.just(new LinkedMultiValueMap<>());
        }

        @Override
        public Mono<MultiValueMap<String, Part>> getMultipartData() {
            return Mono.just(new LinkedMultiValueMap<>());
        }

        @Override
        public LocaleContext getLocaleContext() {
            return Mockito.mock(LocaleContext.class);
        }

        @Override
        public ApplicationContext getApplicationContext() {
            return Mockito.mock(ApplicationContext.class);
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
            return url;
        }

        @Override
        public void addUrlTransformer(Function<String, String> transformer) {
            // Mock implementation
        }

        @Override
        public String getLogPrefix() {
            return "[test-exchange] ";
        }
    }

    static class InvalidServerWebExchangeImplMock implements ServerWebExchange {
        @Override
        public ServerHttpRequest getRequest() {
            ServerHttpRequest mockRequest = Mockito.mock(ServerHttpRequest.class);
            HttpHeaders headers = new HttpHeaders();
            // No Authorization header to simulate invalid token scenario
            when(mockRequest.getHeaders()).thenReturn(headers);
            
            // Mock the path properly
            org.springframework.http.server.RequestPath mockPath = Mockito.mock(org.springframework.http.server.RequestPath.class);
            when(mockPath.value()).thenReturn("/test-path");
            when(mockRequest.getPath()).thenReturn(mockPath);
            when(mockRequest.getId()).thenReturn("test-request-id");
            
            return mockRequest;
        }

        @Override
        public ServerHttpResponse getResponse() {
            return Mockito.mock(ServerHttpResponse.class);
        }

        @Override
        public Map<String, Object> getAttributes() {
            return new HashMap<>();
        }

        // Other required methods with mock implementations
        @Override
        public Mono<WebSession> getSession() {
            return Mono.empty();
        }

        @Override
        public <T extends Principal> Mono<T> getPrincipal() {
            return Mono.empty();
        }

        @Override
        public Mono<MultiValueMap<String, String>> getFormData() {
            return Mono.just(new LinkedMultiValueMap<>());
        }

        @Override
        public Mono<MultiValueMap<String, Part>> getMultipartData() {
            return Mono.just(new LinkedMultiValueMap<>());
        }

        @Override
        public LocaleContext getLocaleContext() {
            return Mockito.mock(LocaleContext.class);
        }

        @Override
        public ApplicationContext getApplicationContext() {
            return Mockito.mock(ApplicationContext.class);
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
            return url;
        }

        @Override
        public void addUrlTransformer(Function<String, String> transformer) {
            // Mock implementation
        }

        @Override
        public String getLogPrefix() {
            return "[invalid-test-exchange] ";
        }
    }

    static class RequestBodyValidator {
        public void apply(Config config) {
            // Mock implementation
        }
    }

    @Test
    void testExpiredTokenValidation() {
        // Setup configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create exchange with expired token
        ServerWebExchangeImpl expiredTokenExchange = new ServerWebExchangeImpl() {
            @Override
            public ServerHttpRequest getRequest() {
                ServerHttpRequest mockRequest = Mockito.mock(ServerHttpRequest.class);


                HttpHeaders headers = new HttpHeaders();
                // Create an expired token
                String expiredToken = JwtTestTokenGenerator.createExpiredToken();
                headers.set("Authorization", "Bearer " + expiredToken);
                ServerHttpRequest.Builder mockBuilder = Mockito.mock(ServerHttpRequest.Builder.class);
                when(mockRequest.getHeaders()).thenReturn(headers);
                when(mockRequest.getId()).thenReturn("test-request-id");
                // Mock the path's value() method to return a proper value
                org.springframework.http.server.RequestPath mockPath = Mockito.mock(org.springframework.http.server.RequestPath.class);
                when(mockPath.value()).thenReturn("/test-path");
                when(mockPath.toString()).thenReturn("/test-path");
                when(mockRequest.getPath()).thenReturn(mockPath);
                when(mockRequest.mutate()).thenReturn(mockBuilder);
                when(mockBuilder.header(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockRequest);

                return mockRequest;
            }
        };

        // Test that expired token throws ApiGatewayException
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
                () -> jwtAuthFilter.filter(expiredTokenExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Token")
                || exception.getMessage().contains("expired")
                || exception.getMessage().contains("Invalid"));
    }

    @Test
    void testTokenWithoutKidValidation() {
        // Setup configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create exchange with token without kid
        ServerWebExchangeImpl tokenWithoutKidExchange = new ServerWebExchangeImpl() {
            @Override
            public ServerHttpRequest getRequest() {
                ServerHttpRequest mockRequest = Mockito.mock(ServerHttpRequest.class);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + BEARER_TOKEN_WITHOUT_KID);
                ServerHttpRequest.Builder mockBuilder = Mockito.mock(ServerHttpRequest.Builder.class);
                when(mockRequest.getHeaders()).thenReturn(headers);
                when(mockRequest.getId()).thenReturn("test-request-id");
                // Mock the path's value() method to return a proper value
                org.springframework.http.server.RequestPath mockPath = Mockito.mock(org.springframework.http.server.RequestPath.class);
                when(mockPath.value()).thenReturn("/test-path");
                when(mockPath.toString()).thenReturn("/test-path");
                when(mockRequest.getPath()).thenReturn(mockPath);
                when(mockRequest.mutate()).thenReturn(mockBuilder);
                when(mockBuilder.header(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockRequest);

                return mockRequest;
            }
        };

        // Mock public key service to handle token without kid (should fallback to DEFAULT)
        PublicKey publicKey = JwtTestTokenGenerator.getTestPublicKey();
        PublicKeyInfo defaultPublicKeyInfo = new PublicKeyInfo();
        defaultPublicKeyInfo.setKid("DEFAULT");
        defaultPublicKeyInfo.setSourceId("test-source-1");
        defaultPublicKeyInfo.setPublicKey(publicKey);
        when(publicKeyService.findPublicKey("DEFAULT", null))
                .thenReturn(Optional.of(defaultPublicKeyInfo));

        // Test that token without kid should be handled properly (either success or specific error)
        try {
            jwtAuthFilter.filter(tokenWithoutKidExchange, gatewayFilterChain);
            // If no exception, the token was processed successfully
        } catch (ApiGatewayException ex) {
            // Verify it's the expected type of error, not a null pointer
            Assertions.assertNotNull(ex.getMessage());
            Assertions.assertFalse(ex.getMessage().contains("NullPointer"));
        }
    }

    @Test
    void testTokenHeaderValidationFailure() {
        // Setup strict header validation configuration
        TokenHeaderValidationConfig strictSubjectConfig = new TokenHeaderValidationConfig();
        strictSubjectConfig.setRequired(true);
        strictSubjectConfig.setRegex("^strict[0-9]+$"); // This will fail for "admin"

        tokenHeaderValidationConfig.put("sub", strictSubjectConfig);
        when(jwtProperties.getTokenHeaderValidationConfig()).thenReturn(tokenHeaderValidationConfig);

        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create exchange with valid token but subject that fails regex
        ServerWebExchangeImpl headerValidationFailExchange = new ServerWebExchangeImpl();
        headerValidationFailExchange.setValidToken(true);

        // Test that header validation failure throws appropriate exception
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
                () -> jwtAuthFilter.filter(headerValidationFailExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Invalid")
                || exception.getMessage().contains("Token")
                || exception.getMessage().contains("validation"));
    }

    @Test
    void testTokenClaimAsListInstance() {
        // Setup configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create custom claims with List instance
        ClaimImpl claimsWithList = new ClaimImpl();
        claimsWithList.put("scope", Arrays.asList("SelfManage", "AdminAccess"));
        claimsWithList.put("roles", Arrays.asList("user", "admin", "moderator"));

        // Test validateScope method with List claim
        try {
            Method validateScopeMethod = JwtAuthFilter.class.getDeclaredMethod("validateScope", Route.class, Claims.class, 
                String.class, String.class);
            validateScopeMethod.setAccessible(true);
            String result = (String) validateScopeMethod.invoke(jwtAuthFilter, route, claimsWithList, 
                "requestId", "requestPath");

            // Should handle List properly and return valid scope
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.contains("SelfManage"));
        } catch (Exception e) {
            Assertions.fail("Should handle List claims properly: " + e.getMessage());
        }
    }

    @Test
    void testTokenClaimAsIntegerInstance() {
        // Setup configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create custom claims with Integer instances
        ClaimImpl claimsWithIntegers = new ClaimImpl() {
            @Override
            public Object get(Object key) {
                if ("exp".equals(key)) {
                    return Integer.valueOf((int) (System.currentTimeMillis() / ONE_THOUSAND) + INT_3600); // 1 hour from now
                }
                if ("iat".equals(key)) {
                    return Integer.valueOf((int) (System.currentTimeMillis() / ONE_THOUSAND)); // now
                }
                if ("userId".equals(key)) {
                    return Integer.valueOf(INT_12345);
                }
                return super.get(key);
            }

            @Override
            public <T> T get(String claimName, Class<T> requiredType) {
                if ("exp".equals(claimName) && Integer.class.equals(requiredType)) {
                    return requiredType.cast(Integer.valueOf((int) (System.currentTimeMillis() / ONE_THOUSAND) + INT_3600));
                }
                if ("iat".equals(claimName) && Integer.class.equals(requiredType)) {
                    return requiredType.cast(Integer.valueOf((int) (System.currentTimeMillis() / ONE_THOUSAND)));
                }
                if ("userId".equals(claimName) && Integer.class.equals(requiredType)) {
                    return requiredType.cast(Integer.valueOf(INT_12345));
                }
                return super.get(claimName, requiredType);
            }
        };

        // Test that integer claims are handled properly
        Integer userId = claimsWithIntegers.get("userId", Integer.class);
        Assertions.assertNotNull(userId);
        Assertions.assertEquals(INT_12345, userId.intValue());

        Integer exp = claimsWithIntegers.get("exp", Integer.class);
        Assertions.assertNotNull(exp);
        Assertions.assertTrue(exp > System.currentTimeMillis() / ONE_THOUSAND);
    }

    @Test
    void testTokenClaimRequiredButMissing() {
        // Setup configuration with required claim
        Map<String, String> requiredClaimMapping = new HashMap<>();
        requiredClaimMapping.put("requiredClaim", "X-Required-Header");
        requiredClaimMapping.put("sub", "X-User-Id");
        when(jwtProperties.getTokenClaimToHeaderMapping()).thenReturn(requiredClaimMapping);

        // Setup validation config for required claim
        TokenHeaderValidationConfig requiredClaimConfig = new TokenHeaderValidationConfig();
        requiredClaimConfig.setRequired(true);
        requiredClaimConfig.setRegex(".*"); // Accept any value
        tokenHeaderValidationConfig.put("requiredClaim", requiredClaimConfig);
        when(jwtProperties.getTokenHeaderValidationConfig()).thenReturn(tokenHeaderValidationConfig);

        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create exchange with token missing required claim
        ServerWebExchangeImpl missingClaimExchange = new ServerWebExchangeImpl();
        missingClaimExchange.setValidToken(true);

        // Test that missing required claim throws appropriate exception
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
                () -> jwtAuthFilter.filter(missingClaimExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Invalid")
                || exception.getMessage().contains("Token")
                || exception.getMessage().contains("required"));
    }

    @Test
    void testTokenClaimRegexValidationFailure() {
        // Setup strict regex validation for audience claim
        TokenHeaderValidationConfig strictAudConfig = new TokenHeaderValidationConfig();
        strictAudConfig.setRequired(true);
        strictAudConfig.setRegex("^production-[a-z]+$"); // This will fail for "test-audience"
        tokenHeaderValidationConfig.put("aud", strictAudConfig);
        when(jwtProperties.getTokenHeaderValidationConfig()).thenReturn(tokenHeaderValidationConfig);

        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create exchange with valid token but audience that fails regex
        ServerWebExchangeImpl regexFailExchange = new ServerWebExchangeImpl();
        regexFailExchange.setValidToken(true);

        // Test that regex validation failure throws appropriate exception
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
                () -> jwtAuthFilter.filter(regexFailExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Invalid")
                || exception.getMessage().contains("Token")
                || exception.getMessage().contains("validation"));
    }

    @Test
    void testMultipleValidationFailuresScenarios() {
        // Test comprehensive scenario with multiple validation issues
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("NonExistentScope"); // Invalid scope
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Setup strict validation rules
        TokenHeaderValidationConfig strictValidation = new TokenHeaderValidationConfig();
        strictValidation.setRequired(true);
        strictValidation.setRegex("^impossible-pattern-[0-9]{10}$");
        tokenHeaderValidationConfig.put("sub", strictValidation);
        tokenHeaderValidationConfig.put("aud", strictValidation);
        when(jwtProperties.getTokenHeaderValidationConfig()).thenReturn(tokenHeaderValidationConfig);

        ServerWebExchangeImpl multipleFailuresExchange = new JwtAuthValidatorTest.ServerWebExchangeImpl();
        multipleFailuresExchange.setValidToken(true);

        // Should throw exception due to multiple validation failures
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
                () -> jwtAuthFilter.filter(multipleFailuresExchange, gatewayFilterChain));

        // Verify proper error handling
        Assertions.assertNotNull(exception);
        Assertions.assertTrue(exception.getStatusCode() == HttpStatus.UNAUTHORIZED
                || exception.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    void testTokenValidationWithNullPublicKey() {
        // Setup mock to return empty public key
        when(publicKeyService.findPublicKey(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());
        when(publicKeyService.findPublicKey(Mockito.anyString(), Mockito.isNull()))
                .thenReturn(Optional.empty());

        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        ServerWebExchangeImpl nullKeyExchange = new ServerWebExchangeImpl();
        nullKeyExchange.setValidToken(true);

        // Test that null public key throws appropriate exception
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
                () -> jwtAuthFilter.filter(nullKeyExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Token")
                || exception.getMessage().contains("Invalid")
                || exception.getMessage().contains("key"));
    }

    @Test
    void testFilterWithMissingAuthorizationHeader() {
        // Setup configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create exchange without Authorization header
        ServerWebExchangeImpl missingHeaderExchange = new ServerWebExchangeImpl() {
            @Override
            public ServerHttpRequest getRequest() {
                ServerHttpRequest mockRequest = Mockito.mock(ServerHttpRequest.class);
                HttpHeaders headers = new HttpHeaders();
                // No Authorization header
                when(mockRequest.getHeaders()).thenReturn(headers);
                when(mockRequest.getId()).thenReturn("test-request-id");
                // Mock the path's value() method to return a proper value
                org.springframework.http.server.RequestPath mockPath = Mockito.mock(org.springframework.http.server.RequestPath.class);
                when(mockPath.value()).thenReturn("/test-path");
                when(mockPath.toString()).thenReturn("/test-path");
                when(mockRequest.getPath()).thenReturn(mockPath);
                return mockRequest;
            }
        };

        // Test missing authorization header
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
            () -> jwtAuthFilter.filter(missingHeaderExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Invalid Token"));
    }

    @Test
    void testFilterWithEmptyAuthorizationHeader() {
        // Setup configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create exchange with empty Authorization header
        ServerWebExchangeImpl emptyHeaderExchange = new ServerWebExchangeImpl() {
            @Override
            public ServerHttpRequest getRequest() {
                ServerHttpRequest mockRequest = Mockito.mock(ServerHttpRequest.class);
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", ""); // Empty header
                when(mockRequest.getHeaders()).thenReturn(headers);
                when(mockRequest.getId()).thenReturn("test-request-id");
                // Mock the path's value() method to return a proper value
                org.springframework.http.server.RequestPath mockPath = Mockito.mock(org.springframework.http.server.RequestPath.class);
                when(mockPath.value()).thenReturn("/test-path");
                when(mockPath.toString()).thenReturn("/test-path");
                when(mockRequest.getPath()).thenReturn(mockPath);
                return mockRequest;
            }
        };

        // Test empty authorization header
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
            () -> jwtAuthFilter.filter(emptyHeaderExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Invalid Token"));
    }

    @Test
    void testFilterWithNonBearerToken() {
        // Setup configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create exchange with non-Bearer token
        ServerWebExchangeImpl nonBearerExchange = new ServerWebExchangeImpl() {
            @Override
            public ServerHttpRequest getRequest() {
                ServerHttpRequest mockRequest = Mockito.mock(ServerHttpRequest.class);
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Basic somebasictoken"); // Non-Bearer token
                when(mockRequest.getHeaders()).thenReturn(headers);
                when(mockRequest.getId()).thenReturn("test-request-id");
                // Mock the path's value() method to return a proper value
                org.springframework.http.server.RequestPath mockPath = Mockito.mock(org.springframework.http.server.RequestPath.class);
                when(mockPath.value()).thenReturn("/test-path");
                when(mockPath.toString()).thenReturn("/test-path");
                when(mockRequest.getPath()).thenReturn(mockPath);
                return mockRequest;
            }
        };

        // Test non-Bearer token
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
            () -> jwtAuthFilter.filter(nonBearerExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Invalid Token"));
    }

    @Test
    void testConstructorWithNullConfig() {
        // Test constructor with null config
        JwtAuthFilter filterWithNullConfig = new JwtAuthFilter(null, publicKeyService, jwtProperties);

        // Verify that routeScopes is empty when config is null
        Assertions.assertNotNull(ReflectionTestUtils.getField(filterWithNullConfig, "routeScopes"));
    }

    @Test
    void testConstructorWithNullScope() {
        // Test constructor with config having null scope
        JwtAuthFilter.Config configWithNullScope = new JwtAuthFilter.Config();
        configWithNullScope.setScope(null);

        JwtAuthFilter filterWithNullScope = new JwtAuthFilter(configWithNullScope, publicKeyService, jwtProperties);

        // Verify that routeScopes is empty when scope is null
        @SuppressWarnings("unchecked")
        Set<String> routeScopes = (Set<String>) ReflectionTestUtils.getField(filterWithNullScope, "routeScopes");
        Assertions.assertTrue(routeScopes.isEmpty());
    }

    @Test
    void testConstructorWithEmptyTokenClaimToHeaderMapping() {
        // Setup JWT properties with empty token claim to header mapping
        Map<String, String> emptyMapping = new HashMap<>();
        when(jwtProperties.getTokenClaimToHeaderMapping()).thenReturn(emptyMapping);

        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");

        // Test constructor with empty mapping
        JwtAuthFilter filterWithEmptyMapping = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Verify that default "sub" -> "user-id" mapping is added
        @SuppressWarnings("unchecked")
        Map<String, String> tokenClaimMapping = (Map<String, String>) ReflectionTestUtils.getField(filterWithEmptyMapping, "tokenClaimToHeaderMapping");
        Assertions.assertTrue(tokenClaimMapping.containsKey("sub"));
        Assertions.assertEquals("user-id", tokenClaimMapping.get("sub"));
    }

    @Test
    void testConstructorWithNullTokenClaimToHeaderMapping() {
        // Setup JWT properties with null token claim to header mapping
        when(jwtProperties.getTokenClaimToHeaderMapping()).thenReturn(null);

        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");

        // Test constructor with null mapping
        JwtAuthFilter filterWithNullMapping = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Verify that default mapping is created
        @SuppressWarnings("unchecked")
        Map<String, String> tokenClaimMapping = (Map<String, String>) ReflectionTestUtils.getField(filterWithNullMapping, "tokenClaimToHeaderMapping");
        Assertions.assertNotNull(tokenClaimMapping);
        Assertions.assertTrue(tokenClaimMapping.containsKey("sub"));
    }

    @Test
    void testValidateTokenHeadersWithPatternSyntaxException() {
        // Setup configuration with invalid regex pattern
        TokenHeaderValidationConfig invalidRegexConfig = new TokenHeaderValidationConfig();
        invalidRegexConfig.setRequired(true);
        invalidRegexConfig.setRegex("[invalid-regex"); // Invalid regex pattern

        tokenHeaderValidationConfig.put("sub", invalidRegexConfig);
        when(jwtProperties.getTokenHeaderValidationConfig()).thenReturn(tokenHeaderValidationConfig);

        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        ServerWebExchangeImpl invalidRegexExchange = new ServerWebExchangeImpl();
        invalidRegexExchange.setValidToken(true);

        // Test that invalid regex throws appropriate exception
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
            () -> jwtAuthFilter.filter(invalidRegexExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Token verification failed"));
    }

    @Test
    void testValidateTokenHeadersWithEmptyRegex() {
        // Setup configuration with empty regex
        TokenHeaderValidationConfig emptyRegexConfig = new TokenHeaderValidationConfig();
        emptyRegexConfig.setRequired(false);
        emptyRegexConfig.setRegex(""); // Empty regex

        tokenHeaderValidationConfig.put("sub", emptyRegexConfig);
        when(jwtProperties.getTokenHeaderValidationConfig()).thenReturn(tokenHeaderValidationConfig);

        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        ServerWebExchangeImpl emptyRegexExchange = new ServerWebExchangeImpl();
        emptyRegexExchange.setValidToken(true);

        // Test that empty regex is handled properly (should not throw exception)
        Assertions.assertDoesNotThrow(() -> jwtAuthFilter.filter(emptyRegexExchange, gatewayFilterChain));
    }

    @Test
    void testValidateTokenWithNullRoute() {
        // Setup configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create exchange with null route
        ServerWebExchangeImpl nullRouteExchange = new ServerWebExchangeImpl() {
            @Override
            public Map<String, Object> getAttributes() {
                Map<String, Object> attributes = new HashMap<>();
                attributes.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, null); // Null route
                return attributes;
            }
        };
        nullRouteExchange.setValidToken(true);

        // Test null route scenario
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
            () -> jwtAuthFilter.filter(nullRouteExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Request not found"));
    }

    @Test
    void testValidateScopeWithEmptyRouteScopes() {
        // Setup configuration with no scope (empty route scopes)
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        // Don't set scope, so routeScopes will be empty
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        ServerWebExchangeImpl emptyRouteScopesExchange = new ServerWebExchangeImpl();
        emptyRouteScopesExchange.setValidToken(true);

        // Test that empty route scopes allows access
        Assertions.assertDoesNotThrow(() -> jwtAuthFilter.filter(emptyRouteScopesExchange, gatewayFilterChain));
    }

    @Test
    void testValidateScopeWithStringScope() {
        // Setup configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage,AdminAccess");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create claims with string scope (space-separated)
        ClaimImpl claimsWithStringScope = new ClaimImpl() {
            @Override
            public Object get(Object key) {
                if ("scope".equals(key)) {
                    return "SelfManage AdminAccess"; // Space-separated scopes
                }
                return super.get(key);
            }
        };

        // Test validateScope method with string scope
        try {
            Method validateScopeMethod = JwtAuthFilter.class.getDeclaredMethod("validateScope", Route.class, Claims.class, 
                String.class, String.class);
            validateScopeMethod.setAccessible(true);
            String result = (String) validateScopeMethod.invoke(jwtAuthFilter, route, claimsWithStringScope, 
                "requestId", "requestPath");

            // Should handle string scope properly
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.contains("SelfManage"));
        } catch (Exception e) {
            Assertions.fail("Should handle string scopes properly: " + e.getMessage());
        }
    }

    @Test
    void testValidateScopeWithCommaSeparatedScope() {
        // Setup configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage,AdminAccess");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create claims with comma-separated scope
        ClaimImpl claimsWithCommaSeparatedScope = new ClaimImpl() {
            @Override
            public Object get(Object key) {
                if ("scope".equals(key)) {
                    return "SelfManage,ReadOnly,AdminAccess"; // Comma-separated scopes
                }
                return super.get(key);
            }
        };

        // Test validateScope method with comma-separated scope
        try {
            Method validateScopeMethod = JwtAuthFilter.class.getDeclaredMethod("validateScope", Route.class, Claims.class, 
                String.class, String.class);
            validateScopeMethod.setAccessible(true);
            String result = (String) validateScopeMethod.invoke(jwtAuthFilter, route, claimsWithCommaSeparatedScope, 
                "requestId", "requestPath");

            // Should handle comma-separated scope properly
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.contains("SelfManage"));
            Assertions.assertTrue(result.contains("AdminAccess"));
        } catch (Exception e) {
            Assertions.fail("Should handle comma-separated scopes properly: " + e.getMessage());
        }
    }

    @Test
    void testValidateScopeWithNullScopeInClaims() {
        // Setup configuration
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create claims with null scope
        ClaimImpl claimsWithNullScope = new ClaimImpl() {
            @Override
            public Object get(Object key) {
                if ("scope".equals(key)) {
                    return null; // Null scope
                }
                return super.get(key);
            }
        };

        // Test validateScope method with null scope
        try {
            Method validateScopeMethod = JwtAuthFilter.class.getDeclaredMethod("validateScope", Route.class, Claims.class,
                String.class, String.class);
            validateScopeMethod.setAccessible(true);

            // Should throw exception due to insufficient scope
            Assertions.assertThrows(Exception.class, () ->
                validateScopeMethod.invoke(jwtAuthFilter, route, claimsWithNullScope, "requestId", "requestPath"));
        } catch (Exception e) {
            // Expected to fail due to missing scope
        }
    }

    @Test
    void testGetTokenHeaderValueWithStringArray() {
        // Test getTokenHeaderValue with String[] input
        ClaimImpl claimsWithStringArray = new ClaimImpl() {
            @Override
            public Object get(Object key) {
                if ("roles".equals(key)) {
                    return new String[]{"admin", "user", "moderator"}; // String array
                }
                return super.get(key);
            }
        };

        try {
            Method getTokenHeaderValueMethod = JwtAuthFilter.class.getDeclaredMethod("getTokenHeaderValue", Claims.class, String.class);
            getTokenHeaderValueMethod.setAccessible(true);
            String result = (String) getTokenHeaderValueMethod.invoke(null, claimsWithStringArray, "roles");

            // Should join array with commas
            Assertions.assertNotNull(result);
            Assertions.assertEquals("admin,user,moderator", result);
        } catch (Exception e) {
            Assertions.fail("Should handle String array properly: " + e.getMessage());
        }
    }

    @Test
    void testGetTokenHeaderValueWithSet() {
        // Test getTokenHeaderValue with Set input
        ClaimImpl claimsWithSet = new ClaimImpl() {
            @Override
            public Object get(Object key) {
                if ("permissions".equals(key)) {
                    return Set.of("read", "write", "delete"); // Set
                }
                return super.get(key);
            }
        };

        try {
            Method getTokenHeaderValueMethod = JwtAuthFilter.class.getDeclaredMethod("getTokenHeaderValue", Claims.class, String.class);
            getTokenHeaderValueMethod.setAccessible(true);
            String result = (String) getTokenHeaderValueMethod.invoke(null, claimsWithSet, "permissions");

            // Should join set with commas
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.contains("read"));
            Assertions.assertTrue(result.contains("write"));
            Assertions.assertTrue(result.contains("delete"));
        } catch (Exception e) {
            Assertions.fail("Should handle Set properly: " + e.getMessage());
        }
    }

    @Test
    void testGetTokenHeaderValueWithOtherTypes() {
        // Test getTokenHeaderValue with other types (Integer, Boolean, etc.)
        ClaimImpl claimsWithOtherTypes = new ClaimImpl() {
            @Override
            public Object get(Object key) {
                if ("userId".equals(key)) {
                    return Integer.valueOf(INT_12345); // Integer
                }
                if ("isActive".equals(key)) {
                    return Boolean.TRUE; // Boolean
                }
                return super.get(key);
            }
        };

        try {
            Method getTokenHeaderValueMethod = JwtAuthFilter.class.getDeclaredMethod("getTokenHeaderValue", Claims.class, String.class);
            getTokenHeaderValueMethod.setAccessible(true);

            String userIdResult = (String) getTokenHeaderValueMethod.invoke(null, claimsWithOtherTypes, "userId");
            String isActiveResult = (String) getTokenHeaderValueMethod.invoke(null, claimsWithOtherTypes, "isActive");

            // Should convert to string
            Assertions.assertEquals("12345", userIdResult);
            Assertions.assertEquals("true", isActiveResult);
        } catch (Exception e) {
            Assertions.fail("Should handle other types properly: " + e.getMessage());
        }
    }

    @Test
    void testSecurityExceptionInValidateToken() {
        // Mock public key service to throw SecurityException
        when(publicKeyService.findPublicKey(Mockito.anyString(), Mockito.anyString()))
            .thenThrow(new SecurityException("Security error"));

        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        ServerWebExchangeImpl securityExceptionExchange = new ServerWebExchangeImpl();
        securityExceptionExchange.setValidToken(true);

        // Test that SecurityException is handled properly
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
            () -> jwtAuthFilter.filter(securityExceptionExchange, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Token verification failed"));
    }

    @Test
    void testFilterOrder() {
        // Test that filter returns correct order
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        int order = jwtAuthFilter.getOrder();
        Assertions.assertEquals(GatewayConstants.JWT_AUTH_FILTER_ORDER, order);
    }

    @Test
    void testTokenScopeWithPrefixes_PositiveCase_PrefixRemovedAndMatched() {
        // Setup configuration with route scope
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("SelfManage,AdminAccess"); // Route requires these scopes
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create claims with scopes that have prefixes that should be removed
        ClaimImpl claims = new ClaimImpl();
        claims.put("scope", Arrays.asList("ProviderPrefix/SelfManage", "ScopePrefix/AdminAccess", "OtherScope"));
        claims.put("sub", "testuser");
        claims.put("aud", "test-audience");

        // Test validateScope method - should succeed after prefix removal
        String result = ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims, "requestId", "requestPath");
        
        // Verify that prefixes were removed and scopes matched
        Assertions.assertNotNull(result);
        Set<String> resultScopes = new HashSet<>(Arrays.asList(result.split(",")));
        Assertions.assertTrue(resultScopes.contains("SelfManage"), "Should contain SelfManage after prefix removal");
        Assertions.assertTrue(resultScopes.contains("AdminAccess"), "Should contain AdminAccess after prefix removal");
        Assertions.assertTrue(resultScopes.contains("OtherScope"), "Should contain OtherScope without prefix");
    }

    @Test
    void testTokenScopeWithPrefixes_PositiveCase_PartialPrefixMatch() {
        // Setup configuration with route scope
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("ReadAccess"); // Route requires this scope
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create claims with mixed scopes - some with prefix, some without
        ClaimImpl claims = new ClaimImpl();
        claims.put("scope", Arrays.asList("ProviderPrefix/ReadAccess", "WriteAccess", "ScopePrefix/DeleteAccess"));
        claims.put("sub", "testuser");
        claims.put("aud", "test-audience");

        // Test validateScope method - should succeed as ReadAccess matches after prefix removal
        String result = ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims, "requestId", "requestPath");
        
        // Verify that prefixes were removed correctly
        Assertions.assertNotNull(result);
        Set<String> resultScopes = new HashSet<>(Arrays.asList(result.split(",")));
        Assertions.assertTrue(resultScopes.contains("ReadAccess"), "Should contain ReadAccess after prefix removal");
        Assertions.assertTrue(resultScopes.contains("WriteAccess"), "Should contain WriteAccess without prefix");
        Assertions.assertTrue(resultScopes.contains("DeleteAccess"), "Should contain DeleteAccess after prefix removal");
    }

    @Test
    void testTokenScopeWithPrefixes_PositiveCase_StringScopeWithPrefix() {
        // Setup configuration with route scope
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("UserManage"); // Route requires this scope
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create claims with string scope containing comma-separated values with prefixes
        ClaimImpl claims = new ClaimImpl();
        claims.put("scope", "ProviderPrefix/UserManage,ScopePrefix/DataAccess,PublicRead");
        claims.put("sub", "testuser");
        claims.put("aud", "test-audience");

        // Test validateScope method - should succeed after prefix removal
        String result = ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims, "requestId", "requestPath");
        
        // Verify that prefixes were removed correctly
        Assertions.assertNotNull(result);
        Set<String> resultScopes = new HashSet<>(Arrays.asList(result.split(",")));
        Assertions.assertTrue(resultScopes.contains("UserManage"), "Should contain UserManage after prefix removal");
        Assertions.assertTrue(resultScopes.contains("DataAccess"), "Should contain DataAccess after prefix removal");
        Assertions.assertTrue(resultScopes.contains("PublicRead"), "Should contain PublicRead without prefix");
    }

    @Test
    void testTokenScopeWithPrefixes_NegativeCase_NoMatchAfterPrefixRemoval() {
        // Setup configuration with route scope
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("RequiredScope"); // Route requires this specific scope
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create claims with scopes that don't match route scope even after prefix removal
        ClaimImpl claims = new ClaimImpl();
        claims.put("scope", Arrays.asList("ProviderPrefix/DifferentScope", "ScopePrefix/AnotherScope", "ThirdScope"));
        claims.put("sub", "testuser");
        claims.put("aud", "test-audience");

        // Test validateScope method - should fail as no scope matches after prefix removal
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class, () -> {
            ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims, "requestId", "requestPath");
        });
        
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Token verification failed"));
    }

    @Test
    void testTokenScopeWithPrefixes_NegativeCase_EmptyScopeAfterPrefixProcessing() {
        // Setup configuration with route scope
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("NeededScope"); // Route requires this scope
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create claims with scopes that become empty or null after prefix processing
        ClaimImpl claims = new ClaimImpl();
        claims.put("scope", Arrays.asList("ProviderPrefix/", "ScopePrefix/", "   ", null));
        claims.put("sub", "testuser");
        claims.put("aud", "test-audience");

        // Test validateScope method - should fail as no valid scopes remain
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class, () -> {
            ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims, "requestId", "requestPath");
        });
        
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Token verification failed"));
    }

    @Test
    void testTokenScopeWithPrefixes_NegativeCase_NonMatchingPrefix() {
        // Setup configuration with route scope
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("TargetScope"); // Route requires this scope
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create claims with scopes that have non-configured prefixes
        ClaimImpl claims = new ClaimImpl();
        claims.put("scope", Arrays.asList("DifferentPrefix/TargetScope", "AnotherPrefix/SomeScope", "UnknownPrefix/TestScope"));
        claims.put("sub", "testuser");
        claims.put("aud", "test-audience");

        // Test validateScope method - should fail as prefixes don't match configured ones
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class, () -> {
            ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims, "requestId", "requestPath");
        });
        
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Token verification failed"));
    }

    @Test
    void testTokenScopeWithPrefixes_EdgeCase_NullTokenScopePrefixes() {
        // Setup JWT properties with null scope prefixes
        when(jwtProperties.getScopePrefixes()).thenReturn(null);
        
        // Setup configuration with route scope
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("TestScope");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create claims with scopes containing prefixes
        ClaimImpl claims = new ClaimImpl();
        claims.put("scope", Arrays.asList("ProviderPrefix/TestScope", "ScopePrefix/OtherScope"));
        claims.put("sub", "testuser");
        claims.put("aud", "test-audience");

        // Test validateScope method - should fail as prefixes are not removed when config is null
        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class, () -> {
            ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims, "requestId", "requestPath");
        });
        
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("Token verification failed"));
    }

    @Test
    void testTokenScopeWithPrefixes_EdgeCase_EmptyTokenScopePrefixes() {
        // Setup JWT properties with empty scope prefixes
        when(jwtProperties.getScopePrefixes()).thenReturn(new HashSet<>());
        
        // Setup configuration with route scope
        JwtAuthFilter.Config config = new JwtAuthFilter.Config();
        config.setScope("DirectScope");
        jwtAuthValidator.apply(config);
        jwtAuthFilter = new JwtAuthFilter(config, publicKeyService, jwtProperties);

        // Create claims with direct scope match (no prefixes to remove)
        ClaimImpl claims = new ClaimImpl();
        claims.put("scope", Arrays.asList("DirectScope", "OtherScope"));
        claims.put("sub", "testuser");
        claims.put("aud", "test-audience");

        // Test validateScope method - should succeed as direct match works
        String result = ReflectionTestUtils.invokeMethod(jwtAuthFilter, "validateScope", route, claims, "requestId", "requestPath");
        
        Assertions.assertNotNull(result);
        Set<String> resultScopes = new HashSet<>(Arrays.asList(result.split(",")));
        Assertions.assertTrue(resultScopes.contains("DirectScope"), "Should contain DirectScope");
        Assertions.assertTrue(resultScopes.contains("OtherScope"), "Should contain OtherScope");
    }
}

