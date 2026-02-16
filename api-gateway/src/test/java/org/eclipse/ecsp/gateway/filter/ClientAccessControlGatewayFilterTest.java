package org.eclipse.ecsp.gateway.filter;

import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.eclipse.ecsp.gateway.service.AccessRuleMatcherService;
import org.eclipse.ecsp.gateway.service.ClientAccessControlCacheService;
import org.eclipse.ecsp.gateway.service.ClientIdValidator;
import org.eclipse.ecsp.gateway.service.JwtClaimExtractor;
import org.eclipse.ecsp.gateway.service.PathExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClientAccessControlGatewayFilterFactory.
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlGatewayFilterTest {

    @Mock(lenient = true)
    private JwtClaimExtractor jwtClaimExtractor;

    @Mock(lenient = true)
    private ClientIdValidator clientIdValidator;

    @Mock(lenient = true)
    private PathExtractor pathExtractor;

    @Mock(lenient = true)
    private AccessRuleMatcherService accessRuleMatcherService;

    @Mock(lenient = true)
    private ClientAccessControlCacheService cacheService;

    @Mock(lenient = true)
    private ClientAccessControlMetrics metrics;

    @Mock(lenient = true)
    private GatewayFilterChain filterChain;

    private ClientAccessControlProperties properties;
    private ClientAccessControlGatewayFilterFactory filterFactory;

    @BeforeEach
    void setUp() {
        properties = new ClientAccessControlProperties();
        properties.setEnabled(true);
        properties.setClaimNames(List.of("clientId", "azp", "client_id"));
        properties.setSkipPaths(List.of("/v3/api-docs/**", "/actuator/**"));

        filterFactory = new ClientAccessControlGatewayFilterFactory(
                properties,
                jwtClaimExtractor,
                clientIdValidator,
                pathExtractor,
                accessRuleMatcherService,
                cacheService,
                metrics
        );

        // Default mock behavior
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void testFilter_ValidClientId_Success() {
        // Arrange
        String jwt = "valid.jwt.token";
        String clientId = "test_client";
        String service = "service";
        String route = "route";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/service/route")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock JWT extraction
        when(jwtClaimExtractor.extractClientId(eq(jwt), anyList())).thenReturn(clientId);
        when(clientIdValidator.isValid(clientId)).thenReturn(true);
        
        // Mock path extraction
        when(pathExtractor.extractService("/api/service/route")).thenReturn(service);
        when(pathExtractor.extractRoute("/api/service/route")).thenReturn(route);
        
        // Mock client config
        ClientAccessConfig clientConfig = ClientAccessConfig.builder()
                .clientId(clientId)
                .active(true)
                .rules(List.of(AccessRule.builder()
                        .service(service)
                        .route(route)
                        .deny(false)
                        .build()))
                .build();
        when(cacheService.getConfig(clientId)).thenReturn(clientConfig);
        
        // Mock access rule matching
        when(accessRuleMatcherService.isAllowed(anyList(), eq(service), eq(route))).thenReturn(true);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
        assertEquals(clientId, exchange.getAttribute("clientId"));
    }

    @Test
    void testFilter_MissingAuthorizationHeader_Unauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/service/route")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    void testFilter_InvalidBearerToken_Unauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/service/route")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz") // Not Bearer
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_NoClientIdInJwt_Unauthorized() {
        // Arrange
        String jwt = "valid.jwt.token";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/service/route")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtClaimExtractor.extractClientId(eq(jwt), anyList())).thenReturn(null);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_InvalidClientId_Unauthorized() {
        // Arrange
        String jwt = "valid.jwt.token";
        String maliciousClientId = "client' OR '1'='1";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/service/route")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtClaimExtractor.extractClientId(eq(jwt), anyList())).thenReturn(maliciousClientId);
        when(clientIdValidator.isValid(maliciousClientId)).thenReturn(false);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_SkipPath_ApiDocs() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/v3/api-docs/swagger-config")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
        verify(jwtClaimExtractor, never()).extractClientId(any(), any());
    }

    @Test
    void testFilter_SkipPath_Actuator() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/actuator/health")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
    }

    @Test
    void testFilter_FilterDisabled_SkipValidation() {
        // Arrange
        properties.setEnabled(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/service/route")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
        verify(jwtClaimExtractor, never()).extractClientId(any(), any());
    }
}
