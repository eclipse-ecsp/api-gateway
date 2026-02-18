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

package org.eclipse.ecsp.gateway.conditions;

import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test purpose    - Verify ClientAccessControlEnabledCondition matching logic.
 * Test data       - Various property configurations.
 * Test expected   - Correct boolean results based on property values.
 * Test type       - Positive and Negative.
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlEnabledConditionTest {

    @Mock
    private ConditionContext conditionContext;

    @Mock
    private Environment environment;

    @Mock
    private AnnotatedTypeMetadata metadata;

    /**
     * Test purpose          - Verify matches method returns true when property is set to "true".
     * Test data             - Property value "true".
     * Test expected result  - Returns true.
     * Test type             - Positive.
     */
    @Test
    void matches_PropertySetToTrue_ReturnsTrue() {
        // GIVEN: Property is set to "true"
        ClientAccessControlEnabledCondition condition = new ClientAccessControlEnabledCondition();
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty(
                eq(GatewayConstants.CLIENT_ACCESS_CONTROL_PREFIX + ".enabled"), 
                eq("false"))).thenReturn("true");

        // WHEN: Condition is evaluated
        boolean result = condition.matches(conditionContext, metadata);

        // THEN: Condition should match
        assertTrue(result);
    }

    /**
     * Test purpose          - Verify matches method returns false when property is set to "false".
     * Test data             - Property value "false".
     * Test expected result  - Returns false.
     * Test type             - Positive.
     */
    @Test
    void matches_PropertySetToFalse_ReturnsFalse() {
        // GIVEN: Property is set to "false"
        ClientAccessControlEnabledCondition condition = new ClientAccessControlEnabledCondition();
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty(
                eq(GatewayConstants.CLIENT_ACCESS_CONTROL_PREFIX + ".enabled"), 
                eq("false"))).thenReturn("false");

        // WHEN: Condition is evaluated
        boolean result = condition.matches(conditionContext, metadata);

        // THEN: Condition should not match
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify matches method returns false when property is not set.
     * Test data             - Property returns default value "false".
     * Test expected result  - Returns false.
     * Test type             - Negative.
     */
    @Test
    void matches_PropertyNotSet_ReturnsFalse() {
        // GIVEN: Property not set, returns default "false"
        ClientAccessControlEnabledCondition condition = new ClientAccessControlEnabledCondition();
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty(anyString(), eq("false"))).thenReturn("false");

        // WHEN: Condition is evaluated
        boolean result = condition.matches(conditionContext, metadata);

        // THEN: Condition should not match (default is false)
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify matches method handles invalid boolean values.
     * Test data             - Property value "invalid".
     * Test expected result  - Returns false.
     * Test type             - Negative.
     */
    @Test
    void matches_InvalidBooleanValue_ReturnsFalse() {
        // GIVEN: Property has invalid boolean value
        ClientAccessControlEnabledCondition condition = new ClientAccessControlEnabledCondition();
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty(
                eq(GatewayConstants.CLIENT_ACCESS_CONTROL_PREFIX + ".enabled"), 
                eq("false"))).thenReturn("invalid");

        // WHEN: Condition is evaluated
        boolean result = condition.matches(conditionContext, metadata);

        // THEN: Condition should not match (invalid parses as false)
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify matches method is case-insensitive.
     * Test data             - Property value "TRUE" (uppercase).
     * Test expected result  - Returns true.
     * Test type             - Positive.
     */
    @Test
    void matches_PropertySetToUppercaseTrue_ReturnsTrue() {
        // GIVEN: Property is set to "TRUE" (uppercase)
        ClientAccessControlEnabledCondition condition = new ClientAccessControlEnabledCondition();
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty(
                eq(GatewayConstants.CLIENT_ACCESS_CONTROL_PREFIX + ".enabled"), 
                eq("false"))).thenReturn("TRUE");

        // WHEN: Condition is evaluated
        boolean result = condition.matches(conditionContext, metadata);

        // THEN: Condition should match (case-insensitive)
        assertTrue(result);
    }

    /**
     * Test purpose          - Verify matches method handles empty string.
     * Test data             - Property value "" (empty).
     * Test expected result  - Returns false.
     * Test type             - Negative.
     */
    @Test
    void matches_PropertySetToEmptyString_ReturnsFalse() {
        // GIVEN: Property is empty string
        ClientAccessControlEnabledCondition condition = new ClientAccessControlEnabledCondition();
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty(
                eq(GatewayConstants.CLIENT_ACCESS_CONTROL_PREFIX + ".enabled"), 
                eq("false"))).thenReturn("");

        // WHEN: Condition is evaluated
        boolean result = condition.matches(conditionContext, metadata);

        // THEN: Condition should not match (empty parses as false)
        assertFalse(result);
    }
}
