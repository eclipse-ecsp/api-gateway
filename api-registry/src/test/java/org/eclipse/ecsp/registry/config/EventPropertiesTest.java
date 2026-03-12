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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.registry.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EventProperties.
 */
class EventPropertiesTest {

    private static final long TEST_DEBOUNCE_DELAY_MS = 3000L;
    private static final long CUSTOM_DEBOUNCE_DELAY_MS = 5000L;

    @Test
    void testPropertiesLoaded() {
        // Create and configure properties manually
        EventProperties testProperties = new EventProperties();
        testProperties.setEnabled(true);
        
        EventProperties.RedisConfig redis = new EventProperties.RedisConfig();
        redis.setChannel("test-channel");
        redis.setDebounceDelayMs(TEST_DEBOUNCE_DELAY_MS);
        testProperties.setRedis(redis);
        
        // Assert main properties
        assertThat(testProperties.isEnabled()).isTrue();

        // Assert Redis properties
        EventProperties.RedisConfig redisConfig = testProperties.getRedis();
        assertThat(redisConfig).isNotNull();
        assertThat(redisConfig.getChannel()).isEqualTo("test-channel");
        assertThat(redisConfig.getDebounceDelayMs()).isEqualTo(TEST_DEBOUNCE_DELAY_MS);
    }

    @Test
    void testSetters() {
        // Arrange
        EventProperties props = new EventProperties();
        EventProperties.RedisConfig redis = new EventProperties.RedisConfig();

        // Act
        props.setEnabled(true);
        redis.setChannel("custom-channel");
        redis.setDebounceDelayMs(CUSTOM_DEBOUNCE_DELAY_MS);
        props.setRedis(redis);

        // Assert
        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getRedis().getChannel()).isEqualTo("custom-channel");
        assertThat(props.getRedis().getDebounceDelayMs()).isEqualTo(CUSTOM_DEBOUNCE_DELAY_MS);
    }
}