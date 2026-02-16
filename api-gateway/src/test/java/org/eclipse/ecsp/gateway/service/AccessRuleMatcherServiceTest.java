package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.model.AccessRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AccessRuleMatcherService.
 */
class AccessRuleMatcherServiceTest {

    private AccessRuleMatcherService matcher;

    @BeforeEach
    void setUp() {
        matcher = new AccessRuleMatcherService();
    }

    // ========================
    // Rule Parsing Tests
    // ========================

    @Test
    void testParseAllowRule() {
        AccessRule rule = matcher.parseRule("user-service:get-profile");

        assertThat(rule).isNotNull();
        assertThat(rule.getService()).isEqualTo("user-service");
        assertThat(rule.getRoute()).isEqualTo("get-profile");
        assertThat(rule.isDeny()).isFalse();
        assertThat(rule.getOriginalRule()).isEqualTo("user-service:get-profile");
    }

    @Test
    void testParseDenyRule() {
        AccessRule rule = matcher.parseRule("!payment-service:refund");

        assertThat(rule).isNotNull();
        assertThat(rule.getService()).isEqualTo("payment-service");
        assertThat(rule.getRoute()).isEqualTo("refund");
        assertThat(rule.isDeny()).isTrue();
        assertThat(rule.getOriginalRule()).isEqualTo("!payment-service:refund");
    }

    @Test
    void testParseWildcardRule() {
        AccessRule rule = matcher.parseRule("*:*");

        assertThat(rule).isNotNull();
        assertThat(rule.getService()).isEqualTo("*");
        assertThat(rule.getRoute()).isEqualTo("*");
        assertThat(rule.isDeny()).isFalse();
    }

    @Test
    void testParseInvalidRuleFormat() {
        AccessRule rule = matcher.parseRule("invalid-rule-no-colon");

        assertThat(rule).isNull();
    }

    @Test
    void testParseNullRule() {
        AccessRule rule = matcher.parseRule(null);

        assertThat(rule).isNull();
    }

    @Test
    void testParseRuleWithWhitespace() {
        AccessRule rule = matcher.parseRule("  user-service : get-profile  ");

        assertThat(rule).isNotNull();
        assertThat(rule.getService()).isEqualTo("user-service");
        assertThat(rule.getRoute()).isEqualTo("get-profile");
    }

    @Test
    void testParseMultipleRules() {
        List<String> ruleStrings = List.of(
                "user-service:*",
                "!user-service:ban-user",
                "*:health"
        );

        List<AccessRule> rules = matcher.parseRules(ruleStrings);

        assertThat(rules).hasSize(3);
        assertThat(rules.get(0).isDeny()).isFalse();
        assertThat(rules.get(1).isDeny()).isTrue();
        assertThat(rules.get(2).getService()).isEqualTo("*");
    }

    // ========================
    // Exact Match Tests
    // ========================

    @Test
    void testExactServiceAndRouteMatch() {
        List<AccessRule> rules = List.of(
                AccessRule.builder()
                        .service("user-service")
                        .route("get-profile")
                        .deny(false)
                        .originalRule("user-service:get-profile")
                        .build()
        );

        boolean allowed = matcher.isAllowed(rules, "user-service", "get-profile");

        assertThat(allowed).isTrue();
    }

    @Test
    void testExactMatchNoMatch() {
        List<AccessRule> rules = List.of(
                AccessRule.builder()
                        .service("user-service")
                        .route("get-profile")
                        .deny(false)
                        .originalRule("user-service:get-profile")
                        .build()
        );

        boolean allowed = matcher.isAllowed(rules, "user-service", "delete-profile");

        assertThat(allowed).isFalse();
    }

    // ========================
    // Wildcard Tests
    // ========================

    @Test
    void testWildcardServiceAllRoutes() {
        List<AccessRule> rules = List.of(
                AccessRule.builder()
                        .service("user-service")
                        .route("*")
                        .deny(false)
                        .originalRule("user-service:*")
                        .build()
        );

        assertThat(matcher.isAllowed(rules, "user-service", "get-profile")).isTrue();
        assertThat(matcher.isAllowed(rules, "user-service", "delete-profile")).isTrue();
        assertThat(matcher.isAllowed(rules, "user-service", "anything")).isTrue();
    }

    @Test
    void testWildcardRouteAllServices() {
        List<AccessRule> rules = List.of(
                AccessRule.builder()
                        .service("*")
                        .route("health")
                        .deny(false)
                        .originalRule("*:health")
                        .build()
        );

        assertThat(matcher.isAllowed(rules, "user-service", "health")).isTrue();
        assertThat(matcher.isAllowed(rules, "payment-service", "health")).isTrue();
        assertThat(matcher.isAllowed(rules, "vehicle-service", "health")).isTrue();
        assertThat(matcher.isAllowed(rules, "user-service", "get-profile")).isFalse();
    }

    @Test
    void testWildcardAllowAll() {
        List<AccessRule> rules = List.of(
                AccessRule.builder()
                        .service("*")
                        .route("*")
                        .deny(false)
                        .originalRule("*:*")
                        .build()
        );

        assertThat(matcher.isAllowed(rules, "user-service", "get-profile")).isTrue();
        assertThat(matcher.isAllowed(rules, "payment-service", "refund")).isTrue();
        assertThat(matcher.isAllowed(rules, "any-service", "any-route")).isTrue();
    }

    @Test
    void testAntStyleWildcardPattern() {
        List<AccessRule> rules = List.of(
                AccessRule.builder()
                        .service("user-service")
                        .route("get-*")
                        .deny(false)
                        .originalRule("user-service:get-*")
                        .build()
        );

        assertThat(matcher.isAllowed(rules, "user-service", "get-profile")).isTrue();
        assertThat(matcher.isAllowed(rules, "user-service", "get-settings")).isTrue();
        assertThat(matcher.isAllowed(rules, "user-service", "delete-profile")).isFalse();
    }

    // ========================
    // Deny Rule Tests
    // ========================

    @Test
    void testDenyRuleBlocksAccess() {
        List<AccessRule> rules = List.of(
                AccessRule.builder()
                        .service("user-service")
                        .route("ban-user")
                        .deny(true)
                        .originalRule("!user-service:ban-user")
                        .build()
        );

        boolean allowed = matcher.isAllowed(rules, "user-service", "ban-user");

        assertThat(allowed).isFalse();
    }

    @Test
    void testDenyOverridesAllow() {
        List<AccessRule> rules = List.of(
                AccessRule.builder()
                        .service("user-service")
                        .route("*")
                        .deny(false)
                        .originalRule("user-service:*")
                        .build(),
                AccessRule.builder()
                        .service("user-service")
                        .route("ban-user")
                        .deny(true)
                        .originalRule("!user-service:ban-user")
                        .build()
        );

        assertThat(matcher.isAllowed(rules, "user-service", "get-profile")).isTrue();
        assertThat(matcher.isAllowed(rules, "user-service", "ban-user")).isFalse();
    }

    @Test
    void testDenyOrderIndependent() {
        // Deny rule first
        List<AccessRule> rulesWithDenyFirst = List.of(
                AccessRule.builder()
                        .service("user-service")
                        .route("ban-user")
                        .deny(true)
                        .originalRule("!user-service:ban-user")
                        .build(),
                AccessRule.builder()
                        .service("user-service")
                        .route("*")
                        .deny(false)
                        .originalRule("user-service:*")
                        .build()
        );

        // Allow rule first
        List<AccessRule> rulesWithAllowFirst = List.of(
                AccessRule.builder()
                        .service("user-service")
                        .route("*")
                        .deny(false)
                        .originalRule("user-service:*")
                        .build(),
                AccessRule.builder()
                        .service("user-service")
                        .route("ban-user")
                        .deny(true)
                        .originalRule("!user-service:ban-user")
                        .build()
        );

        // Both should deny
        assertThat(matcher.isAllowed(rulesWithDenyFirst, "user-service", "ban-user")).isFalse();
        assertThat(matcher.isAllowed(rulesWithAllowFirst, "user-service", "ban-user")).isFalse();
    }

    // ========================
    // Deny-by-Default Tests
    // ========================

    @Test
    void testEmptyRulesDenyByDefault() {
        List<AccessRule> rules = List.of();

        boolean allowed = matcher.isAllowed(rules, "user-service", "get-profile");

        assertThat(allowed).isFalse();
    }

    @Test
    void testNullRulesDenyByDefault() {
        boolean allowed = matcher.isAllowed(null, "user-service", "get-profile");

        assertThat(allowed).isFalse();
    }

    @Test
    void testNoMatchingRuleDenyByDefault() {
        List<AccessRule> rules = List.of(
                AccessRule.builder()
                        .service("payment-service")
                        .route("*")
                        .deny(false)
                        .originalRule("payment-service:*")
                        .build()
        );

        boolean allowed = matcher.isAllowed(rules, "user-service", "get-profile");

        assertThat(allowed).isFalse();
    }

    // ========================
    // Complex Scenarios
    // ========================

    @Test
    void testMultipleAllowRulesWithDeny() {
        List<AccessRule> rules = List.of(
                AccessRule.builder()
                        .service("user-service")
                        .route("*")
                        .deny(false)
                        .originalRule("user-service:*")
                        .build(),
                AccessRule.builder()
                        .service("payment-service")
                        .route("*")
                        .deny(false)
                        .originalRule("payment-service:*")
                        .build(),
                AccessRule.builder()
                        .service("payment-service")
                        .route("refund")
                        .deny(true)
                        .originalRule("!payment-service:refund")
                        .build()
        );

        assertThat(matcher.isAllowed(rules, "user-service", "get-profile")).isTrue();
        assertThat(matcher.isAllowed(rules, "payment-service", "charge")).isTrue();
        assertThat(matcher.isAllowed(rules, "payment-service", "refund")).isFalse();
        assertThat(matcher.isAllowed(rules, "vehicle-service", "get-vehicle")).isFalse();
    }

    @Test
    void testMixedWildcardsAndDeny() {
        List<AccessRule> rules = List.of(
                AccessRule.builder()
                        .service("*")
                        .route("*")
                        .deny(false)
                        .originalRule("*:*")
                        .build(),
                AccessRule.builder()
                        .service("*")
                        .route("delete-*")
                        .deny(true)
                        .originalRule("!*:delete-*")
                        .build()
        );

        assertThat(matcher.isAllowed(rules, "user-service", "get-profile")).isTrue();
        assertThat(matcher.isAllowed(rules, "user-service", "delete-profile")).isFalse();
        assertThat(matcher.isAllowed(rules, "payment-service", "delete-payment")).isFalse();
        assertThat(matcher.isAllowed(rules, "payment-service", "charge")).isTrue();
    }
}
