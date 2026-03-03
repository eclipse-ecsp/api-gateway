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

package org.eclipse.ecsp.security;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ScopeTagger#customize(Operation, HandlerMethod)}.
 *
 * <p>Covers: annotation-absent warning, empty-scope label, override disabled,
 * override enabled with / without a matching routeId, null security list guard,
 * multiple SecurityRequirements, operationId underscore normalization, and
 * null summary/description defaulting.
 */
@ExtendWith(MockitoExtension.class)
class ScopeTaggerTest {

    private static final int EXPECTED_SECURITY_REQUIREMENT_COUNT = 2;
    @InjectMocks
    private ScopeTagger scopeTagger;

    @Mock
    private HandlerMethod handlerMethod;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Build a minimal Operation with one SecurityRequirement entry so that
     * operation.getSecurity() is non-null by default.
     * routeId derived by ScopeTagger = "test-controller-{operationId}"
     */
    private Operation buildOperation(String operationId) {
        Operation operation = new Operation();
        operation.setTags(List.of("test-controller"));
        operation.setOperationId(operationId);
        io.swagger.v3.oas.models.security.SecurityRequirement sr =
                new io.swagger.v3.oas.models.security.SecurityRequirement();
        sr.addList("JwtAuthValidator", "OriginalScope");
        operation.setSecurity(new ArrayList<>(List.of(sr)));
        return operation;
    }

    /**
     * Create a real {@link SecurityRequirement} annotation instance with the given scopes.
     * Annotations are interfaces; an anonymous class is the correct way to instantiate them.
     */
    private static SecurityRequirement createAnnotation(final String... scopes) {
        return new SecurityRequirement() {
            @Override
            public String name() {
                return "JwtAuthValidator";
            }

            @Override
            public String[] scopes() {
                return scopes;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return SecurityRequirement.class;
            }
        };
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * When no @SecurityRequirement annotation is present on the handler method,
     * the description must contain the no-security warning.
     */
    @Test
    void testCustomize_NoAnnotation_AddsSecurityWarning() {
        Operation operation = buildOperation("myOp");
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class)).thenReturn(null);

        Operation result = scopeTagger.customize(operation, handlerMethod);

        assertTrue(result.getDescription().contains("WARNING!! - Security Config Not Found"),
                "Description should contain security-not-found warning");
    }

    /**
     * When the annotation declares zero scopes the description must show 'SCOPE: EMPTY'
     * and the existing security list must remain unchanged.
     */
    @Test
    void testCustomize_EmptyScopes_AddsScopeEmptyLabel() {
        Operation operation = buildOperation("myOp");
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class))
                .thenReturn(createAnnotation(/* empty */));

        Operation result = scopeTagger.customize(operation, handlerMethod);

        assertTrue(result.getDescription().contains("SCOPE: EMPTY"),
                "Description should contain SCOPE: EMPTY");
        assertFalse(result.getDescription().contains("OVERRIDE_SCOPE"),
                "No OVERRIDE_SCOPE label expected");
    }

    /**
     * Scopes present on the annotation, but override is disabled: the description shows
     * the annotation scope; operation.getSecurity() must keep the original scope.
     */
    @Test
    void testCustomize_WithScopes_OverrideDisabled_DoesNotChangeSecurity() {
        Operation operation = buildOperation("myOp");
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class))
                .thenReturn(createAnnotation("OriginalScope"));
        ReflectionTestUtils.setField(scopeTagger, "isOverrideScopeEnabled", false);

        Operation result = scopeTagger.customize(operation, handlerMethod);

        assertTrue(result.getDescription().contains("SCOPE:"));
        assertFalse(result.getDescription().contains("OVERRIDE_SCOPE"),
                "OVERRIDE_SCOPE label must not appear when override is disabled");
        assertEquals(List.of("OriginalScope"),
                result.getSecurity().get(0).get("JwtAuthValidator"),
                "Security scopes must not be changed");
    }

    /**
     * Override is enabled, but the scopesMap has no entry for the routeId: no scope
     * replacement should occur and the description must not contain OVERRIDE_SCOPE.
     */
    @Test
    void testCustomize_OverrideEnabled_NoMatchingRouteId_DoesNotChangeScopes() {
        final Operation operation = buildOperation("myOp");
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class))
                .thenReturn(createAnnotation("OriginalScope"));
        ReflectionTestUtils.setField(scopeTagger, "isOverrideScopeEnabled", true);
        scopeTagger.setScopesMap(Map.of("some-other-controller-otherOp", List.of("NewScope")));

        Operation result = scopeTagger.customize(operation, handlerMethod);

        assertFalse(result.getDescription().contains("OVERRIDE_SCOPE"),
                "OVERRIDE_SCOPE label must not appear when routeId has no entry in scopesMap");
        assertEquals(List.of("OriginalScope"),
                result.getSecurity().get(0).get("JwtAuthValidator"),
                "Security scopes must not be changed when routeId is not in scopesMap");
    }

    /**
     * Override is enabled and scopesMap contains an exact-case match for the routeId.
     * The scopes in operation.getSecurity() must be fully replaced by the override list,
     * and the description must contain OVERRIDE_SCOPE.
     */
    @Test
    void testCustomize_OverrideEnabled_ExactRouteIdMatch_ReplacesScopes() {
        // routeId = "test-controller-myOp"
        final Operation operation = buildOperation("myOp");
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class))
                .thenReturn(createAnnotation("OriginalScope"));
        ReflectionTestUtils.setField(scopeTagger, "isOverrideScopeEnabled", true);
        scopeTagger.setScopesMap(
                Map.of("test-controller-myOp", List.of("SelfManage", "ManageNotifications")));

        Operation result = scopeTagger.customize(operation, handlerMethod);

        assertTrue(result.getDescription().contains("OVERRIDE_SCOPE"),
                "Description must contain OVERRIDE_SCOPE label");
        assertEquals(List.of("SelfManage", "ManageNotifications"),
                result.getSecurity().get(0).get("JwtAuthValidator"),
                "Scopes in SecurityRequirement must be replaced by the override list");
    }

    /**
     * Override is enabled and the scopesMap only has a lower-case key for the routeId.
     * The ScopeTagger must fall back to the lowercase lookup and still apply the override.
     */
    @Test
    void testCustomize_OverrideEnabled_LowercaseRouteIdFallback_ReplacesScopes() {
        // operationId contains uppercase → routeId = "test-controller-MYOP"
        // lowercase  → "test-controller-myop"
        final Operation operation = buildOperation("MYOP");
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class))
                .thenReturn(createAnnotation("OriginalScope"));
        ReflectionTestUtils.setField(scopeTagger, "isOverrideScopeEnabled", true);
        scopeTagger.setScopesMap(Map.of("test-controller-myop", List.of("SelfManage")));

        Operation result = scopeTagger.customize(operation, handlerMethod);

        assertTrue(result.getDescription().contains("OVERRIDE_SCOPE"),
                "Description must contain OVERRIDE_SCOPE label with lowercase key fallback");
        assertEquals(List.of("SelfManage"),
                result.getSecurity().get(0).get("JwtAuthValidator"),
                "Scopes must be replaced using the lowercase key fallback");
    }

    /**
     * When operation.getSecurity() is null and an override would otherwise apply, the
     * method must not throw and the OVERRIDE_SCOPE label must still appear in the
     * description (the null-guard skips the replaceAll call but the label is unconditional).
     */
    @Test
    void testCustomize_OverrideEnabled_NullSecurity_NoNpeAndLabelAdded() {
        Operation operation = buildOperation("myOp");
        operation.setSecurity(null);
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class))
                .thenReturn(createAnnotation("OriginalScope"));
        ReflectionTestUtils.setField(scopeTagger, "isOverrideScopeEnabled", true);
        scopeTagger.setScopesMap(Map.of("test-controller-myOp", List.of("SelfManage")));

        Operation result = scopeTagger.customize(operation, handlerMethod);

        assertTrue(result.getDescription().contains("OVERRIDE_SCOPE"),
                "OVERRIDE_SCOPE label must still appear even when getSecurity() is null");
        assertNull(result.getSecurity(), "Security list must remain null (nothing to replace)");
    }

    /**
     * When the operation has multiple SecurityRequirement entries, every entry's scopes
     * must be replaced by the override list.
     */
    @Test
    void testCustomize_OverrideEnabled_MultipleSecurityRequirements_AllScopesReplaced() {
        Operation operation = buildOperation("myOp");
        // Add a second SR with a different filter name
        io.swagger.v3.oas.models.security.SecurityRequirement sr2 =
                new io.swagger.v3.oas.models.security.SecurityRequirement();
        sr2.addList("AnotherFilter", "AnotherScope");
        operation.addSecurityItem(sr2);

        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class))
                .thenReturn(createAnnotation("OriginalScope"));
        ReflectionTestUtils.setField(scopeTagger, "isOverrideScopeEnabled", true);
        scopeTagger.setScopesMap(
                Map.of("test-controller-myOp", List.of("SelfManage", "ManageNotifications")));

        Operation result = scopeTagger.customize(operation, handlerMethod);

        List<io.swagger.v3.oas.models.security.SecurityRequirement> security = result.getSecurity();
        assertEquals(EXPECTED_SECURITY_REQUIREMENT_COUNT, security.size(), "Both SecurityRequirements must be present");
        security.forEach(sr ->
                sr.forEach((filterName, scopes) ->
                        assertEquals(List.of("SelfManage", "ManageNotifications"), scopes,
                                "Every SR entry must have its scopes replaced: " + filterName)));
    }

    /**
     * underscores in operationId must be normalised to dashes.
     */
    @Test
    void testCustomize_OperationIdWithUnderscores_NormalisedToDashes() {
        Operation operation = buildOperation("my_op_name");
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class))
                .thenReturn(createAnnotation("SomeScope"));

        Operation result = scopeTagger.customize(operation, handlerMethod);

        assertEquals("my-op-name", result.getOperationId(),
                "Underscores in operationId must be replaced by dashes");
    }

    /**
     * When summary and description are null they must be defaulted by customize().
     */
    @Test
    void testCustomize_NullSummaryAndDescription_AreDefaulted() {
        Operation operation = buildOperation("myOp");
        operation.setSummary(null);
        operation.setDescription(null);
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class))
                .thenReturn(createAnnotation("SomeScope"));

        Operation result = scopeTagger.customize(operation, handlerMethod);

        assertNotNull(result.getSummary(), "Summary must be defaulted when originally null");
        assertNotNull(result.getDescription(), "Description must be defaulted when originally null");
    }

    /**
     * When scopesMap is null the override branch must be skipped even if the flag is true.
     */
    @Test
    void testCustomize_OverrideEnabled_NullScopesMap_NoOverrideApplied() {
        final Operation operation = buildOperation("myOp");
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class))
                .thenReturn(createAnnotation("OriginalScope"));
        ReflectionTestUtils.setField(scopeTagger, "isOverrideScopeEnabled", true);
        scopeTagger.setScopesMap(null);

        Operation result = scopeTagger.customize(operation, handlerMethod);

        assertFalse(result.getDescription().contains("OVERRIDE_SCOPE"),
                "OVERRIDE_SCOPE label must not appear when scopesMap is null");
        assertEquals(List.of("OriginalScope"),
                result.getSecurity().get(0).get("JwtAuthValidator"),
                "Scopes must remain unchanged when scopesMap is null");
    }
}
