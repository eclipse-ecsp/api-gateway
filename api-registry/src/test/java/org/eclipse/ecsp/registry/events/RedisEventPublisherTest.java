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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.registry.config.EventProperties;
import org.eclipse.ecsp.registry.events.data.AbstractEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test purpose    - Verify RedisEventPublisher event publishing to Redis.
 * Test data       - Various event data and error scenarios.
 * Test expected   - Events published successfully or errors handled.
 * Test type       - Positive and Negative.
 */
@ExtendWith(MockitoExtension.class)
class RedisEventPublisherTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventProperties eventProperties;

    @Mock
    private EventProperties.RedisConfig redisConfig;

    private RedisEventPublisher publisher;

    private static final String TEST_CHANNEL = "test-channel";

    @BeforeEach
    void setUp() {
        when(eventProperties.getRedis()).thenReturn(redisConfig);
        when(redisConfig.getChannel()).thenReturn(TEST_CHANNEL);
        publisher = new RedisEventPublisher(redisTemplate, objectMapper, eventProperties);
    }

    /**
     * Test purpose          - Verify publishEvent publishes successfully to Redis.
     * Test data             - Valid event data.
     * Test expected result  - Returns true and publishes to Redis.
     * Test type             - Positive.
     */
    @Test
    void publishEventValidEventDataPublishesToRedis() throws Exception {
        // GIVEN: Valid event data
        TestEventData eventData = new TestEventData("test-event-1", RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED);
        String expectedJson = "{\"eventId\":\"test-event-1\"}";
        when(objectMapper.writeValueAsString(eventData)).thenReturn(expectedJson);

        // WHEN: Event is published
        boolean result = publisher.publishEvent(eventData);

        // THEN: Should publish to Redis and return true
        assertTrue(result);
        verify(objectMapper, times(1)).writeValueAsString(eventData);
        verify(redisTemplate, times(1)).convertAndSend(TEST_CHANNEL, expectedJson);
    }

    /**
     * Test purpose          - Verify publishEvent handles JSON serialization failure.
     * Test data             - Event data that fails to serialize.
     * Test expected result  - Returns false.
     * Test type             - Negative.
     */
    @Test
    void publishEventJsonProcessingExceptionReturnsFalse() throws Exception {
        // GIVEN: Event data that fails serialization
        TestEventData eventData = new TestEventData("test-event-1", RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED);
        when(objectMapper.writeValueAsString(eventData))
                .thenThrow(new JsonProcessingException("Serialization failed") {});

        // WHEN: Event is published
        boolean result = publisher.publishEvent(eventData);

        // THEN: Should return false
        assertFalse(result);
        verify(objectMapper, times(1)).writeValueAsString(eventData);
        verify(redisTemplate, times(0)).convertAndSend(anyString(), anyString());
    }

    /**
     * Test purpose          - Verify publishEvent handles Redis publishing failure.
     * Test data             - Redis template throws exception.
     * Test expected result  - Returns false.
     * Test type             - Negative.
     */
    @Test
    void publishEventRedisExceptionReturnsFalse() throws Exception {
        // GIVEN: Redis template throws exception
        TestEventData eventData = new TestEventData("test-event-1", RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED);
        String expectedJson = "{\"eventId\":\"test-event-1\"}";
        when(objectMapper.writeValueAsString(eventData)).thenReturn(expectedJson);
        doThrow(new RuntimeException("Redis connection failed"))
                .when(redisTemplate).convertAndSend(anyString(), anyString());

        // WHEN: Event is published
        boolean result = publisher.publishEvent(eventData);

        // THEN: Should return false
        assertFalse(result);
        verify(redisTemplate, times(1)).convertAndSend(TEST_CHANNEL, expectedJson);
    }

    /**
     * Test purpose          - Verify publishEvent publishes to correct channel.
     * Test data             - Event data with custom channel.
     * Test expected result  - Publishes to configured channel.
     * Test type             - Positive.
     */
    @Test
    void publishEventCustomChannelPublishesToConfiguredChannel() throws Exception {
        // GIVEN: Custom channel configuration
        String customChannel = "custom-channel";
        when(redisConfig.getChannel()).thenReturn(customChannel);
        RedisEventPublisher customPublisher = new RedisEventPublisher(redisTemplate, objectMapper, eventProperties);
        TestEventData eventData = new TestEventData("test-event-1", RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED);
        String expectedJson = "{\"eventId\":\"test-event-1\"}";
        when(objectMapper.writeValueAsString(eventData)).thenReturn(expectedJson);

        // WHEN: Event is published
        boolean result = customPublisher.publishEvent(eventData);

        // THEN: Should publish to custom channel
        assertTrue(result);
        verify(redisTemplate, times(1)).convertAndSend(eq(customChannel), eq(expectedJson));
    }

    /**
     * Test purpose          - Verify publishEvent handles multiple events.
     * Test data             - Multiple event data instances.
     * Test expected result  - All events published successfully.
     * Test type             - Positive.
     */
    @Test
    void publishEventMultipleEventsAllPublished() throws Exception {
        // GIVEN: Multiple events
        TestEventData event1 = new TestEventData("event-1", RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED);
        TestEventData event2 = new TestEventData("event-2", RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED);
        when(objectMapper.writeValueAsString(event1)).thenReturn("{\"eventId\":\"event-1\"}");
        when(objectMapper.writeValueAsString(event2)).thenReturn("{\"eventId\":\"event-2\"}");

        // WHEN: Events are published
        boolean result1 = publisher.publishEvent(event1);
        boolean result2 = publisher.publishEvent(event2);

        // THEN: Both should succeed
        assertTrue(result1);
        assertTrue(result2);
        verify(redisTemplate, times(2)).convertAndSend(eq(TEST_CHANNEL), anyString());
    }

    /**
     * Test event data class for testing.
     */
    private static class TestEventData extends AbstractEventData {
        private final RouteEventType eventType;

        public TestEventData(String eventId, RouteEventType eventType) {
            super();
            this.eventType = eventType;
        }

        @Override
        public RouteEventType getEventType() {
            return eventType;
        }
    }
}
