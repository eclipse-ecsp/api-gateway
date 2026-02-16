package org.eclipse.ecsp.registry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlEventDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for publishing client access control events to Redis Pub/Sub.
 *
 * <p>
 * Published events notify api-gateway instances to refresh their caches.
 * Event channel: api-gateway:config:events
 *
 * <p>
 * Integration point: Called by CRUD service after successful database operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientAccessControlEventPublisher {

    private static final String CHANNEL = "api-gateway:config:events";
    private static final String EVENT_TYPE = "CLIENT_ACCESS_CONTROL_UPDATED";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish event for client configuration changes.
     *
     * @param operation Operation type (CREATE, UPDATE, DELETE)
     * @param clientIds List of affected client IDs
     */
    public void publishEvent(String operation, List<String> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) {
            log.warn("Attempted to publish event with empty clientIds");
            return;
        }

        try {
            ClientAccessControlEventDto event = ClientAccessControlEventDto.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .eventType(EVENT_TYPE)
                    .operation(operation)
                    .clientIds(clientIds)
                    .build();

            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL, json);

            log.info("Published event: operation={}, clientIds={}, eventId={}", 
                    operation, clientIds, event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish event: operation={}, clientIds={}", operation, clientIds, e);
            // Don't throw exception - event publishing failure should not break CRUD operations
        }
    }

    /**
     * Publish event for single client change.
     *
     * @param operation Operation type
     * @param clientId Affected client ID
     */
    public void publishEvent(String operation, String clientId) {
        publishEvent(operation, List.of(clientId));
    }
}
