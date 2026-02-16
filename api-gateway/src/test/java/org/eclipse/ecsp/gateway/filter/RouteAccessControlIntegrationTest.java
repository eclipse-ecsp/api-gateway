package org.eclipse.ecsp.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.eclipse.ecsp.gateway.service.ClientAccessControlCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration test for route-level access control in ClientAccessControlGatewayFilterFactory.
 * Tests complete flow: JWT extraction → security validation → cache lookup → rule matching → allow/deny.
 *
 * <p>
 * Test Scenarios:
 * - AS-3: Valid JWT with matching allow rule returns 200
 * - AS-4: Valid JWT with deny rule returns 401
 * - AS-4: Valid JWT with allow rule overridden by deny returns 401 (deny priority)
 * - EC-4: Valid JWT with no matching rule returns 401 (deny-by-default)
 * - EC-10: Valid JWT with multiple rules including deny returns 401
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
    "api.gateway.client-access-control.enabled=true",
    "api.gateway.client-access-control.claim-names[0]=clientId",
    "api.gateway.client-access-control.claim-names[1]=azp",
    "api.gateway.client-access-control.claim-names[2]=client_id",
    "api.gateway.client-access-control.skip-paths[0]=/actuator/**",
    "spring.cloud.gateway.routes[0].id=test-user-service",
    "spring.cloud.gateway.routes[0].uri=http://localhost:9999",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user-service/**",
    "spring.cloud.gateway.routes[1].id=test-payment-service",
    "spring.cloud.gateway.routes[1].uri=http://localhost:9998",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/payment-service/**"
})
class RouteAccessControlIntegrationTest {

    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int TWO_RULES = 2;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ClientAccessControlCacheService cacheService;

    @MockBean
    private WebClient.Builder webClientBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        cacheService.clearCache();

        // Mock WebClient to prevent actual API calls during cache operations
        when(webClientBuilder.build()).thenReturn(null);
    }

    /**
     * AS-3: Valid JWT with matching allow rule should return 200 (or 404 if backend not available).
     * Testing: client-admin with rule "user-service:*" accessing /user-service/get-profile
     */
    @Test
    void testAccessAllowed_MatchingAllowRule() {
        // Given: Client with allow rule for user-service
        ClientAccessConfig config = ClientAccessConfig.builder()
                .clientId("client-admin")
                .tenant("default")
                .active(true)
                .rules(parseRules(List.of("user-service:*")))
                .build();
        addConfigToCache(config);

        String jwt = createJwt("client-admin");

        // When: Request to user-service route
        // Then: Should be allowed (404 is acceptable since backend doesn't exist, but not 401)
        webTestClient.get()
                .uri("/user-service/get-profile")
                .header("Authorization", "Bearer " + jwt)
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotEqualTo(HTTP_UNAUTHORIZED));
    }

    /**
     * AS-4: Valid JWT with deny rule should return 401.
     * Testing: client-restricted with rule "!payment-service:*" accessing /payment-service/refund
     */
    @Test
    void testAccessDenied_DenyRule() {
        // Given: Client with deny rule for payment-service
        ClientAccessConfig config = ClientAccessConfig.builder()
                .clientId("client-restricted")
                .tenant("default")
                .active(true)
                .rules(parseRules(List.of("*:*", "!payment-service:*")))
                .build();
        addConfigToCache(config);

        String jwt = createJwt("client-restricted");

        // When: Request to denied payment-service route
        // Then: Should be denied with 401
        webTestClient.get()
                .uri("/payment-service/refund")
                .header("Authorization", "Bearer " + jwt)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class)
                .value(body -> body.contains("Access denied"));
    }

    /**
     * AS-4: Valid JWT with allow rule overridden by deny rule should return 401 (deny priority).
     * Testing: client-mixed with rules ["user-service:*", "!user-service:delete-*"]
     * accessing /user-service/delete-profile
     */
    @Test
    void testAccessDenied_DenyOverridesAllow() {
        // Given: Client with both allow and deny rules, deny should take precedence
        ClientAccessConfig config = ClientAccessConfig.builder()
                .clientId("client-mixed")
                .tenant("default")
                .active(true)
                .rules(parseRules(List.of("user-service:*", "!user-service:delete-*")))
                .build();
        addConfigToCache(config);

        String jwt = createJwt("client-mixed");

        // When: Request to route covered by both allow (user-service:*) and deny (!user-service:delete-*)
        // Then: Deny should take precedence, return 401
        webTestClient.get()
                .uri("/user-service/delete-profile")
                .header("Authorization", "Bearer " + jwt)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class)
                .value(body -> body.contains("Access denied"));
    }

    /**
     * EC-4: Valid JWT with no matching rule should return 401 (deny-by-default).
     * Testing: client-readonly with rule "user-service:get-*" accessing /payment-service/create-payment
     */
    @Test
    void testAccessDenied_NoMatchingRule_DenyByDefault() {
        // Given: Client with specific allow rule that doesn't match target route
        ClientAccessConfig config = ClientAccessConfig.builder()
                .clientId("client-readonly")
                .tenant("default")
                .active(true)
                .rules(parseRules(List.of("user-service:get-*")))
                .build();
        addConfigToCache(config);

        String jwt = createJwt("client-readonly");

        // When: Request to route not covered by any allow rule
        // Then: Deny-by-default should apply, return 401
        webTestClient.get()
                .uri("/payment-service/create-payment")
                .header("Authorization", "Bearer " + jwt)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class)
                .value(body -> body.contains("Access denied"));
    }

    /**
     * EC-10: Valid JWT with multiple rules including deny should return 401.
     * Testing: client-complex with rules ["*:*", "!payment-service:refund", "!user-service:delete-*"]
     * accessing /payment-service/refund
     */
    @Test
    void testAccessDenied_MultipleRulesWithDeny() {
        // Given: Client with wildcard allow and multiple specific denies
        ClientAccessConfig config = ClientAccessConfig.builder()
                .clientId("client-complex")
                .tenant("default")
                .active(true)
                .rules(parseRules(List.of("*:*", "!payment-service:refund", "!user-service:delete-*")))
                .build();
        addConfigToCache(config);

        String jwt = createJwt("client-complex");

        // When: Request to specifically denied route
        // Then: Should be denied despite wildcard allow
        webTestClient.get()
                .uri("/payment-service/refund")
                .header("Authorization", "Bearer " + jwt)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class)
                .value(body -> body.contains("Access denied"));
    }

    /**
     * Test: Client not in cache should be denied.
     */
    @Test
    void testAccessDenied_ClientNotInCache() {
        // Given: No config in cache for this client
        String jwt = createJwt("client-unknown");

        // When: Request from unknown client
        // Then: Should be denied
        webTestClient.get()
                .uri("/user-service/get-profile")
                .header("Authorization", "Bearer " + jwt)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Test: Inactive client should be denied.
     */
    @Test
    void testAccessDenied_InactiveClient() {
        // Given: Client exists but is inactive
        ClientAccessConfig config = ClientAccessConfig.builder()
                .clientId("client-inactive")
                .tenant("default")
                .active(false)
                .rules(parseRules(List.of("*:*")))
                .build();
        addConfigToCache(config);

        String jwt = createJwt("client-inactive");

        // When: Request from inactive client
        // Then: Should be denied
        webTestClient.get()
                .uri("/user-service/get-profile")
                .header("Authorization", "Bearer " + jwt)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class)
                .value(body -> body.contains("inactive"));
    }

    /**
     * Test: Missing JWT should be denied.
     */
    @Test
    void testAccessDenied_MissingJwt() {
        // When: Request without Authorization header
        // Then: Should be denied
        webTestClient.get()
                .uri("/user-service/get-profile")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Test: Wildcard allow rule "*:*" should allow all routes.
     */
    @Test
    void testAccessAllowed_WildcardRule() {
        // Given: Client with wildcard allow rule
        ClientAccessConfig config = ClientAccessConfig.builder()
                .clientId("client-superadmin")
                .tenant("default")
                .active(true)
                .rules(parseRules(List.of("*:*")))
                .build();
        addConfigToCache(config);

        String jwt = createJwt("client-superadmin");

        // When: Request to any route
        // Then: Should be allowed
        webTestClient.get()
                .uri("/user-service/any-route")
                .header("Authorization", "Bearer " + jwt)
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotEqualTo(HTTP_UNAUTHORIZED));

        webTestClient.get()
                .uri("/payment-service/any-route")
                .header("Authorization", "Bearer " + jwt)
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotEqualTo(HTTP_UNAUTHORIZED));
    }

    /**
     * Helper: Add config to cache using reflection or direct cache manipulation.
     * Since cache is private, we use the cache service's method if available,
     * or parse and cache a config directly.
     */
    private void addConfigToCache(ClientAccessConfig config) {
        try {
            // Use reflection to access private cache field and add directly
            java.lang.reflect.Field cacheField =
                    ClientAccessControlCacheService.class.getDeclaredField("clientConfigCache");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.concurrent.ConcurrentHashMap<String, ClientAccessConfig> cache =
                    (java.util.concurrent.ConcurrentHashMap<String, ClientAccessConfig>)
                            cacheField.get(cacheService);
            cache.put(config.getClientId(), config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add config to cache", e);
        }
    }

    /**
     * Helper: Create unsecured JWT with clientId claim.
     * JWT structure: header.payload.signature (signature is empty for unsecured JWT)
     */
    private String createJwt(String clientId) {
        // Create JWT payload with clientId claim
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.format("{\"clientId\":\"%s\"}", clientId).getBytes());
        
        // Unsecured JWT: header.payload. (note the trailing dot with empty signature)
        return header + "." + payload + ".";
    }

    /**
     * Helper: Parse allow/deny rule strings into AccessRule objects.
     * Format: [!]service:route where ! prefix indicates deny rule.
     */
    private List<AccessRule> parseRules(List<String> ruleStrings) {
        List<AccessRule> rules = new ArrayList<>();
        for (String rule : ruleStrings) {
            boolean deny = rule.startsWith("!");
            String cleanRule = deny ? rule.substring(1) : rule;
            String[] parts = cleanRule.split(":", TWO_RULES);
            rules.add(AccessRule.builder()
                    .service(parts[0])
                    .route(parts.length > 1 ? parts[1] : "*")
                    .deny(deny)
                    .originalRule(rule)
                    .build());
        }
        return rules;
    }
}
