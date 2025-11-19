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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RedisCacheEnabledCondition}.
 *
 * @author Abhishek Kumar
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheEnabledConditionTest {

    private static final String PROPERTY_CACHING_TYPE = GatewayConstants.CACHING_TYPE;
    private static final String PROPERTY_CACHING_ENABLED = GatewayConstants.CACHING_ENABLED;
    private static final String VALUE_REDIS = "redis";
    private static final String VALUE_LOCAL = "local";
    private static final String VALUE_TRUE = "true";
    private static final String VALUE_FALSE = "false";

    private RedisCacheEnabledCondition condition;

    @Mock
    private ConditionContext context;

    @Mock
    private Environment environment;

    @Mock
    private AnnotatedTypeMetadata metadata;

    @BeforeEach
    void setUp() {
        condition = new RedisCacheEnabledCondition();
        when(context.getEnvironment()).thenReturn(environment);
    }

    @ParameterizedTest
    @CsvSource({
        "true, redis, true",
        "true, REDIS, true",
        "true, Redis, true",
        "false, redis, false",
        "true, local, false",
        "false, local, false",
        "false, false, false"
    })
    void matches_WithVariousConfigurations_ReturnsExpectedResult(
            String cachingEnabled, String cachingType, boolean expected) {
        // Arrange
        when(environment.getProperty(PROPERTY_CACHING_TYPE, VALUE_REDIS)).thenReturn(cachingType);
        when(environment.getProperty(PROPERTY_CACHING_ENABLED, VALUE_FALSE)).thenReturn(cachingEnabled);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        if (expected) {
            assertTrue(result);
        } else {
            assertFalse(result);
        }
    }

    @Test
    void matches_WhenRedisCachingEnabled_ReturnsTrue() {
        // Arrange
        when(environment.getProperty(PROPERTY_CACHING_TYPE, VALUE_REDIS)).thenReturn(VALUE_REDIS);
        when(environment.getProperty(PROPERTY_CACHING_ENABLED, VALUE_FALSE)).thenReturn(VALUE_TRUE);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertTrue(result);
    }

    @Test
    void matches_WhenRedisCachingDisabled_ReturnsFalse() {
        // Arrange
        when(environment.getProperty(PROPERTY_CACHING_TYPE, VALUE_REDIS)).thenReturn(VALUE_REDIS);
        when(environment.getProperty(PROPERTY_CACHING_ENABLED, VALUE_FALSE)).thenReturn(VALUE_FALSE);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertFalse(result);
    }

    @Test
    void matches_WhenLocalCachingEnabled_ReturnsFalse() {
        // Arrange
        when(environment.getProperty(PROPERTY_CACHING_TYPE, VALUE_REDIS)).thenReturn(VALUE_LOCAL);
        when(environment.getProperty(PROPERTY_CACHING_ENABLED, VALUE_FALSE)).thenReturn(VALUE_TRUE);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertFalse(result);
    }

    @Test
    void matches_WhenBothDisabled_ReturnsFalse() {
        // Arrange
        when(environment.getProperty(PROPERTY_CACHING_TYPE, VALUE_REDIS)).thenReturn(VALUE_LOCAL);
        when(environment.getProperty(PROPERTY_CACHING_ENABLED, VALUE_FALSE)).thenReturn(VALUE_FALSE);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertFalse(result);
    }

    @Test
    void matches_WhenCachingTypeIsNull_UsesDefaultRedis() {
        // Arrange
        when(environment.getProperty(PROPERTY_CACHING_TYPE, VALUE_REDIS)).thenReturn(VALUE_REDIS);
        when(environment.getProperty(PROPERTY_CACHING_ENABLED, VALUE_FALSE)).thenReturn(VALUE_TRUE);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertTrue(result);
    }

    @Test
    void matches_WhenCachingEnabledIsNull_UsesDefaultFalse() {
        // Arrange
        when(environment.getProperty(PROPERTY_CACHING_TYPE, VALUE_REDIS)).thenReturn(VALUE_REDIS);
        when(environment.getProperty(PROPERTY_CACHING_ENABLED, VALUE_FALSE)).thenReturn(VALUE_FALSE);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertFalse(result);
    }

    @Test
    void matches_WhenCachingTypeIsMixedCase_ShouldMatchIgnoreCase() {
        // Arrange
        when(environment.getProperty(PROPERTY_CACHING_TYPE, VALUE_REDIS)).thenReturn("ReDiS");
        when(environment.getProperty(PROPERTY_CACHING_ENABLED, VALUE_FALSE)).thenReturn(VALUE_TRUE);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertTrue(result);
    }
}
