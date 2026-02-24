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

package org.eclipse.ecsp.gateway.health;

import org.eclipse.ecsp.gateway.config.RouteRefreshProperties;
import org.eclipse.ecsp.gateway.events.RouteRefreshFallbackScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteRefreshHealthIndicatorTest {

    @Mock
    private RouteRefreshFallbackScheduler fallbackScheduler;

    @Mock
    private RouteRefreshProperties properties;

    @Mock
    private RouteRefreshProperties.RedisConfig redisConfig;

    @InjectMocks
    private RouteRefreshHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getRedis()).thenReturn(redisConfig);
        lenient().when(redisConfig.getChannel()).thenReturn("test-channel");
        when(properties.getStrategy()).thenReturn(RouteRefreshProperties.RefreshStrategy.EVENT_DRIVEN);
    }

    @Test
    void testHealthUpRedisConnectedAndFallbackInactive() {
        // Arrange
        when(fallbackScheduler.checkRedisConnection()).thenReturn(true);
        when(fallbackScheduler.isFallbackActive()).thenReturn(false);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("mode", "EVENT_DRIVEN")
                .containsEntry("redisConnected", true)
                .containsEntry("channel", "test-channel");
    }

    @Test
    void testHealthDegradedFallbackActive() {
        // Arrange
        when(fallbackScheduler.checkRedisConnection()).thenReturn(true);
        when(fallbackScheduler.isFallbackActive()).thenReturn(true);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));
        assertThat(health.getDetails()).containsEntry("mode", "POLLING_FALLBACK");
    }

    @Test
    void testHealthDegradedRedisConnectionFailed() {
        // Arrange
        when(fallbackScheduler.checkRedisConnection()).thenReturn(false);
        when(fallbackScheduler.isFallbackActive()).thenReturn(true);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));
        assertThat(health.getDetails()).containsEntry("redisConnected", false);
    }

    @Test
    void testHealthDownUnexpectedException() {
        // Arrange
        when(fallbackScheduler.checkRedisConnection()).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }
}
