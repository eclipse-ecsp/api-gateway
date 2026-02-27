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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RedisEnabledCondition}.
 *
 * @author Abhishek Kumar
 */
@ExtendWith(MockitoExtension.class)
class RedisEnabledConditionTest {

    private static final String VALUE_TRUE = "true";
    private static final String VALUE_FALSE = "false";
    private static final String VALUE_REDIS = "redis";
    private static final String VALUE_LOCAL = "local";

    @Mock
    private ConditionContext context;

    @Mock
    private Environment environment;

    @Mock
    private AnnotatedTypeMetadata metadata;

    private RedisEnabledCondition condition;

    @BeforeEach
    void setUp() {
        condition = new RedisEnabledCondition();
        when(context.getEnvironment()).thenReturn(environment);
    }

    @Test
    void matchesWhenRateLimitEnabledReturnsTrue() {
        // Arrange - stub all properties with lenient for unused ones
        when(environment.getProperty(GatewayConstants.RATE_LIMITING_ENABLED, VALUE_TRUE))
            .thenReturn(VALUE_TRUE);
        lenient().when(environment.getProperty(GatewayConstants.CACHING_ENABLED, VALUE_FALSE))
            .thenReturn(VALUE_FALSE);
        lenient().when(environment.getProperty(GatewayConstants.CACHING_TYPE, VALUE_REDIS))
            .thenReturn(VALUE_REDIS);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertTrue(result, "Should return true when rate limit is enabled");
    }

    @Test
    void matchesWhenRedisCacheEnabledReturnsTrue() {
        // Arrange - stub all properties
        when(environment.getProperty(GatewayConstants.RATE_LIMITING_ENABLED, VALUE_TRUE))
            .thenReturn(VALUE_FALSE);
        when(environment.getProperty(GatewayConstants.CACHING_ENABLED, VALUE_FALSE))
            .thenReturn(VALUE_TRUE);
        when(environment.getProperty(GatewayConstants.CACHING_TYPE, VALUE_REDIS))
            .thenReturn(VALUE_REDIS);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertTrue(result, "Should return true when redis cache is enabled");
    }

    @Test
    void matchesWhenBothEnabledReturnsTrue() {
        // Arrange - stub all properties with lenient for short-circuit
        when(environment.getProperty(GatewayConstants.RATE_LIMITING_ENABLED, VALUE_TRUE))
            .thenReturn(VALUE_TRUE);
        lenient().when(environment.getProperty(GatewayConstants.CACHING_ENABLED, VALUE_FALSE))
            .thenReturn(VALUE_TRUE);
        lenient().when(environment.getProperty(GatewayConstants.CACHING_TYPE, VALUE_REDIS))
            .thenReturn(VALUE_REDIS);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertTrue(result, "Should return true when both rate limit and redis cache are enabled");
    }

    @Test
    void matchesWhenNeitherEnabledReturnsFalse() {
        // Arrange - stub all properties
        when(environment.getProperty(GatewayConstants.RATE_LIMITING_ENABLED, VALUE_TRUE))
            .thenReturn(VALUE_FALSE);
        when(environment.getProperty(GatewayConstants.CACHING_ENABLED, VALUE_FALSE))
            .thenReturn(VALUE_FALSE);
        lenient().when(environment.getProperty(GatewayConstants.CACHING_TYPE, VALUE_REDIS))
            .thenReturn(VALUE_REDIS);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertFalse(result, "Should return false when neither rate limit nor redis cache is enabled");
    }

    @Test
    void matchesWhenCacheEnabledButNotRedisTypeReturnsFalse() {
        // Arrange - stub all properties
        when(environment.getProperty(GatewayConstants.RATE_LIMITING_ENABLED, VALUE_TRUE))
            .thenReturn(VALUE_FALSE);
        when(environment.getProperty(GatewayConstants.CACHING_ENABLED, VALUE_FALSE))
            .thenReturn(VALUE_TRUE);
        when(environment.getProperty(GatewayConstants.CACHING_TYPE, VALUE_REDIS))
            .thenReturn(VALUE_LOCAL);

        // Act
        boolean result = condition.matches(context, metadata);

        // Assert
        assertFalse(result, "Should return false when cache is enabled but type is not redis");
    }
}
