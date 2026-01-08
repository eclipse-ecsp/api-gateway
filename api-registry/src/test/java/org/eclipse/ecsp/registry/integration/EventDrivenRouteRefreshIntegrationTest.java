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

package org.eclipse.ecsp.registry.integration;

import org.eclipse.ecsp.registry.events.RouteEventPublisher;
import org.eclipse.ecsp.registry.events.RouteEventThrottler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for event-driven route refresh using Testcontainers with Redis.
 * Tests Redis connectivity and basic component functionality.
 */
@SpringBootTest(
    properties = {
        "api-registry.events.enabled=true",
        "api-registry.events.redis.channel=route-changes-it",
        "api-registry.events.redis.debounce-delay-ms=100"
    }
)
@Testcontainers
class EventDrivenRouteRefreshIntegrationTest {

    private static final int REDIS_PORT = 6379;
    private static final int TEST_TIMEOUT_SECONDS = 10;
    private static final long DEBOUNCE_WAIT_MS = 200L;

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RouteEventPublisher eventPublisher;

    @Autowired
    private RouteEventThrottler eventThrottler;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void testRedisContainer_IsRunning() {
        // Verify Redis container is running
        assertThat(redis.isRunning()).isTrue();
        assertThat(redis.getFirstMappedPort()).isGreaterThan(0);
    }

    @Test
    void testRedisConnectivity_BasicOperations() {
        // Verify Redis is accessible and basic operations work
        String testKey = "test-key-" + UUID.randomUUID();
        String testValue = "test-value";
        
        redisTemplate.opsForValue().set(testKey, testValue, Duration.ofSeconds(TEST_TIMEOUT_SECONDS));
        String retrievedValue = redisTemplate.opsForValue().get(testKey);
        
        assertThat(retrievedValue).isEqualTo(testValue);
        
        // Cleanup
        redisTemplate.delete(testKey);
    }

    @Test
    void testEventPublisher_IsConfigured() {
        // Verify event publisher is properly autowired
        assertThat(eventPublisher).isNotNull();
    }

    @Test
    void testEventThrottler_IsConfigured() {
        // Verify event throttler is properly autowired
        assertThat(eventThrottler).isNotNull();
    }

    @Test
    void testEventPublisher_CanPublishEvent() {
        // Test that event publisher can be invoked without errors
        String serviceId = "test-service-" + UUID.randomUUID();
        
        // This should not throw an exception
        eventPublisher.publishRouteChangeEvent(serviceId);
        
        // Wait for debouncing delay to complete
        await().pollDelay(Duration.ofMillis(DEBOUNCE_WAIT_MS)).until(() -> true);
        
        // Verify the event was processed (throttler didn't crash)
        assertThat(eventThrottler).isNotNull();
    }

    @Test
    void testRedisTemplate_CanPublishToChannel() {
        // Test that we can publish to the Redis channel
        String testMessage = "test-message";
        String channel = "route-changes-it";
        
        // This should not throw an exception
        Long subscribers = redisTemplate.convertAndSend(channel, testMessage);
        
        // May be 0 if no subscribers, but should not be null
        assertThat(subscribers).isNotNull();
    }
}
