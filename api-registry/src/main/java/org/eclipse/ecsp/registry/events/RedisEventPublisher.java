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
import org.eclipse.ecsp.registry.utils.RegistryConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementation of EventPublisher that publishes events to Redis.
 * Serializes EventData subclasses directly using polymorphic JSON serialization.
 */
@Component
@ConditionalOnProperty(name = RegistryConstants.REGISTRY_EVENT_ENABLED, havingValue = "true")
public class RedisEventPublisher implements EventPublisher {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RedisEventPublisher.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final String channel;

    /**
     * Constructor for RedisEventPublisher.
     *
     * @param redisTemplate Redis template for publishing
     * @param objectMapper JSON object mapper
     * @param eventProperties event configuration properties
     */
    public RedisEventPublisher(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            EventProperties eventProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.channel = eventProperties.getRedis().getChannel();
        LOGGER.info("RedisEventPublisher initialized with channel: {}", channel);
    }

    @Override
    public <T extends AbstractEventData> boolean publishEvent(T eventData) {
        try {
            // Directly serialize the EventData subclass
            // ObjectMapper will serialize all fields from the concrete EventData type
            String eventJson = objectMapper.writeValueAsString(eventData);

            // Publish to Redis
            redisTemplate.convertAndSend(channel, eventJson);

            LOGGER.info("Published {} event to Redis: eventId={}, channel={}, type={}",
                    eventData.getEventType(), eventData.getEventId(), channel, 
                    eventData.getClass().getSimpleName());
            return true;

        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize event. EventType: {}, EventId: {}, Class: {}",
                    eventData.getEventType(), eventData.getEventId(), 
                    eventData.getClass().getSimpleName(), e);
        } catch (Exception e) {
            LOGGER.error("Failed to publish event to Redis. EventType: {}, EventId: {}, Class: {}",
                    eventData.getEventType(), eventData.getEventId(), 
                    eventData.getClass().getSimpleName(), e);
        }
        return false;
    }
}
