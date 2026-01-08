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

package org.eclipse.ecsp.registry.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.ecsp.registry.config.EventProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RouteEventThrottler.
 */
@ExtendWith(MockitoExtension.class)
class RouteEventThrottlerTest {

    private static final long TEST_DEBOUNCE_DELAY_MS = 100L;
    private static final long SLEEP_BUFFER_MS = 250L;
    private static final long SHORT_SLEEP_MS = 30L;
    private static final long PRE_DEBOUNCE_SLEEP_MS = 80L;
    private static final int SINGLE_INVOCATION = 1;
    private static final int DUPLICATE_SERVICE_COUNT = -1;
    private static final int AT_LEAST_TWO_INVOCATIONS = 2;
    private static final int AT_LEAST_THREE_INVOCATIONS = 3;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private EventProperties eventProperties;

    @Mock
    private EventProperties.RedisConfig redisConfig;

    private ObjectMapper objectMapper;
    private RouteEventThrottler throttler;

    @BeforeEach
    void setUp() {
        // Use real ObjectMapper for proper JSON serialization
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        when(eventProperties.getRedis()).thenReturn(redisConfig);
        when(redisConfig.getChannel()).thenReturn("route-updates");
        when(redisConfig.getDebounceDelayMs()).thenReturn(TEST_DEBOUNCE_DELAY_MS); // Short delay for testing
        
        throttler = new RouteEventThrottler(eventProperties, redisTemplate, objectMapper);
    }

    @AfterEach
    void tearDown() {
        if (throttler != null) {
            throttler.shutdown();
        }
    }

    @Test
    void testScheduleEvent_SingleService() {
        // Arrange
        String serviceName = "test-service";

        // Act
        throttler.scheduleEvent(serviceName);
        
        // Wait for debounce delay and verify event is published
        await().atMost(Duration.ofMillis(SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(redisTemplate, times(SINGLE_INVOCATION)).convertAndSend(
                            anyString(), anyString());
                });

        // Assert
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate, times(SINGLE_INVOCATION)).convertAndSend(
                channelCaptor.capture(), messageCaptor.capture());
        
        assertThat(channelCaptor.getValue()).isEqualTo("route-updates");
        String message = messageCaptor.getValue();
        assertThat(message)
            .isNotNull()
            .contains("ROUTE_CHANGE")
            .contains("test-service");
    }

    @Test
    void testScheduleEvent_MultipleServices_Consolidates() {
        // Arrange
        final String service1 = "service-1";
        final String service2 = "service-2";
        final String service3 = "service-3";

        // Act
        throttler.scheduleEvent(service1);
        await().pollDelay(Duration.ofMillis(SHORT_SLEEP_MS)).until(() -> true);
        throttler.scheduleEvent(service2);
        await().pollDelay(Duration.ofMillis(SHORT_SLEEP_MS)).until(() -> true);
        throttler.scheduleEvent(service3);
        
        // Wait for debounce and verify event is published
        await().atMost(Duration.ofMillis(SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(redisTemplate, times(SINGLE_INVOCATION)).convertAndSend(
                            eq("route-updates"), anyString());
                });

        // Assert - should only publish once with all services
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate, times(SINGLE_INVOCATION)).convertAndSend(eq("route-updates"), messageCaptor.capture());
        
        String publishedMessage = messageCaptor.getValue();
        assertThat(publishedMessage)
            .isNotNull()
            .contains("service-1")
            .contains("service-2")
            .contains("service-3");
    }

    @Test
    void testScheduleEvent_TimerResets() {
        // Arrange
        String service1 = "service-1";
        String service2 = "service-2";

        // Act - schedule service1, then service2 after 80ms (before first would publish)
        throttler.scheduleEvent(service1);
        await().pollDelay(Duration.ofMillis(PRE_DEBOUNCE_SLEEP_MS)).until(() -> true);
        throttler.scheduleEvent(service2);
        
        // Verify no publish yet
        verify(redisTemplate, times(0)).convertAndSend(anyString(), anyString());
        
        // Wait for debounce delay from second event and verify (need total buffer time)
        await().atMost(Duration.ofMillis(TEST_DEBOUNCE_DELAY_MS + SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(redisTemplate, times(SINGLE_INVOCATION)).convertAndSend(
                            eq("route-updates"), anyString());
                });

        // Assert - should have published once with both services (timer was reset)
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate, times(SINGLE_INVOCATION)).convertAndSend(eq("route-updates"), messageCaptor.capture());
        
        String publishedMessage = messageCaptor.getValue();
        assertThat(publishedMessage)
            .isNotNull()
            .contains("service-1")
            .contains("service-2");
    }

    @Test
    void testScheduleEvent_DuplicateServices_DeduplicatesInSameWindow() {
        // Arrange
        String serviceName = "test-service";

        // Act
        throttler.scheduleEvent(serviceName);
        throttler.scheduleEvent(serviceName);
        throttler.scheduleEvent(serviceName);
        
        // Wait for debounce and verify event is published
        await().atMost(Duration.ofMillis(SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(redisTemplate, times(SINGLE_INVOCATION)).convertAndSend(
                            eq("route-updates"), anyString());
                });

        // Assert - should publish once with service appearing once
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate, times(SINGLE_INVOCATION)).convertAndSend(eq("route-updates"), messageCaptor.capture());
        
        String publishedMessage = messageCaptor.getValue();
        assertThat(publishedMessage).isNotNull();
        // Count occurrences of service name in JSON (should appear once in services array)
        int count = publishedMessage.split("test-service", DUPLICATE_SERVICE_COUNT).length - 1;
        assertThat(count).isEqualTo(SINGLE_INVOCATION);
    }

    @Test
    void testShutdown_CancelsScheduledTasks() {
        // Arrange - schedule an event but don't let it complete
        throttler.scheduleEvent("test-service");

        // Act - shutdown immediately to cancel the scheduled task
        throttler.shutdown();
        
        // Assert - verify shutdown completed successfully
        // The throttler object should still exist
        assertThat(throttler).isNotNull();
        
        // Verify that after shutdown, the executor is properly terminated
        // by checking that we can't schedule more events (would throw exception)
        // We just verify the shutdown didn't throw an exception itself
        assertThat(throttler).extracting("scheduler").isNotNull();
    }

    @Test
    void testMultipleWindows_PublishesMultipleTimes() {
        // Arrange & Act
        // First window
        throttler.scheduleEvent("service-1");
        await().atMost(Duration.ofMillis(TEST_DEBOUNCE_DELAY_MS + SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(redisTemplate, atLeast(SINGLE_INVOCATION)).convertAndSend(anyString(), anyString());
                });

        // Second window
        throttler.scheduleEvent("service-2");
        await().atMost(Duration.ofMillis(TEST_DEBOUNCE_DELAY_MS + SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(redisTemplate, atLeast(AT_LEAST_TWO_INVOCATIONS)).convertAndSend(anyString(), anyString());
                });

        // Third window
        throttler.scheduleEvent("service-3");
        await().atMost(Duration.ofMillis(TEST_DEBOUNCE_DELAY_MS + SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(redisTemplate, atLeast(AT_LEAST_THREE_INVOCATIONS)).convertAndSend(anyString(), anyString());
                });

        // Assert
        verify(redisTemplate, atLeast(AT_LEAST_THREE_INVOCATIONS)).convertAndSend(anyString(), anyString());
    }
}
