package org.eclipse.ecsp.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for refreshing client access control configurations in the gateway.
 *
 * <p>
 * Dual-mode operation:
 * 1. Event-driven (default): Subscribe to Redis Pub/Sub events for real-time refresh
 * 2. Polling fallback: Periodic full cache reload when Redis unavailable
 *
 * <p>
 * Mode transitions:
 * - Startup: Attempt Redis connection → event-driven mode
 * - Runtime Redis failure: Switch to polling mode
 * - Redis restored: Switch back to event-driven mode
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientAccessControlConfigRefreshService implements MessageListener {

    private static final String CHANNEL = "api-gateway:config:events";
    private static final long EVENT_DEDUP_TTL_MS = 60_000; // 1 minute

    private final ClientAccessControlCacheService cacheService;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ClientAccessControlProperties properties;
    private final ObjectMapper objectMapper;

    // Event deduplication: Track processed event IDs with timestamp
    private final Map<String, Instant> processedEvents = new ConcurrentHashMap<>();
    
    // Mode tracking
    private final AtomicBoolean eventDrivenMode = new AtomicBoolean(false);
    private final AtomicBoolean redisHealthy = new AtomicBoolean(false);

    /**
     * Initialize Redis subscriber at startup.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing client access control config refresh service");
        
        if (properties.isEnabled()) {
            subscribeToRedis();
        } else {
            log.info("Client access control is disabled, skipping Redis subscription");
        }
    }

    /**
     * Subscribe to Redis Pub/Sub channel.
     */
    private void subscribeToRedis() {
        try {
            ChannelTopic topic = new ChannelTopic(CHANNEL);
            redisMessageListenerContainer.addMessageListener(this, topic);
            
            eventDrivenMode.set(true);
            redisHealthy.set(true);
            
            log.info("[REDIS] Subscribed to Redis channel: {} - Event-driven mode active", CHANNEL);
        } catch (Exception e) {
            log.error("[REDIS] Failed to subscribe to Redis channel - Falling back to polling mode: {}",
                    e.getMessage(), e);
            eventDrivenMode.set(false);
            redisHealthy.set(false);
        }
    }

    /**
     * Handle incoming Redis Pub/Sub messages.
     *
     * @param message Redis message
     * @param pattern Channel pattern (unused)
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            ClientAccessControlEventDto event = objectMapper.readValue(json, ClientAccessControlEventDto.class);

            log.debug("Received event: eventId={}, operation={}, clientIds={}", 
                    event.getEventId(), event.getOperation(), event.getClientIds());

            // Event deduplication
            if (isDuplicate(event.getEventId())) {
                log.debug("Duplicate event ignored: eventId={}", event.getEventId());
                return;
            }

            // Mark as healthy if we're receiving events
            if (!redisHealthy.get()) {
                log.info("[REDIS] Redis connection restored - Switching from polling to event-driven mode");
                redisHealthy.set(true);
                eventDrivenMode.set(true);
            }

            // Refresh cache for affected clients
            cacheService.refresh(event.getClientIds()).subscribe(
                    count -> log.info(
                            "Cache refreshed successfully: {} clients updated via Redis event", count),
                    error -> log.error("[REDIS] Cache refresh failed for event: {} - Error: {}",
                            event.getEventId(), error.getMessage(), error)
            );

            // Track event for deduplication
            trackEvent(event.getEventId());

        } catch (Exception e) {
            log.error("[REDIS] Failed to process Redis event - Event will be skipped: {}", e.getMessage(), e);
        }
    }

    /**
     * Polling fallback: Periodic full cache reload when Redis unavailable.
     *
     * <p>
     * Runs every N seconds (configurable via polling-interval-seconds).
     * Only executes when NOT in event-driven mode.
     */
    @Scheduled(fixedDelayString = "${api.gateway.client-access-control.polling-interval-seconds:30}000")
    public void pollingFallback() {
        if (!properties.isEnabled()) {
            return;
        }

        // Only poll if NOT in event-driven mode
        if (eventDrivenMode.get()) {
            // Try Redis health check
            try {
                redisMessageListenerContainer.getConnectionFactory().getConnection().ping();
                redisHealthy.set(true);
            } catch (Exception e) {
                log.error("[REDIS] Redis health check failed - "
                        + "Switching from event-driven to polling mode: {}", e.getMessage());
                redisHealthy.set(false);
                eventDrivenMode.set(false);
            }
            return;
        }

        log.debug("Polling mode: Reloading all client configurations");

        cacheService.loadAllConfigurations().subscribe(
                count -> log.info("Polling refresh completed: {} clients loaded", count),
                error -> {
                    log.error("[REDIS] Polling refresh failed - Will retry on next schedule: {}",
                            error.getMessage(), error);
                    // Attempt to reconnect to Redis
                    attemptRedisReconnect();
                }
        );

        // Cleanup old deduplication entries
        cleanupProcessedEvents();
    }

    /**
     * Attempt to reconnect to Redis.
     */
    private void attemptRedisReconnect() {
        try {
            redisMessageListenerContainer.getConnectionFactory().getConnection().ping();
            log.info("[REDIS] Redis reconnected successfully - Switching from polling to event-driven mode");
            redisHealthy.set(true);
            eventDrivenMode.set(true);
        } catch (Exception e) {
            log.debug("[REDIS] Redis still unavailable, continuing polling mode");
        }
    }

    /**
     * Check if event has already been processed.
     *
     * @param eventId Event UUID
     * @return true if duplicate
     */
    private boolean isDuplicate(String eventId) {
        return processedEvents.containsKey(eventId);
    }

    /**
     * Track processed event for deduplication.
     *
     * @param eventId Event UUID
     */
    private void trackEvent(String eventId) {
        processedEvents.put(eventId, Instant.now());
    }

    /**
     * Cleanup old processed events (older than TTL).
     */
    private void cleanupProcessedEvents() {
        Instant cutoff = Instant.now().minusMillis(EVENT_DEDUP_TTL_MS);
        processedEvents.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }

    /**
     * Get current refresh mode.
     *
     * @return "EVENT_DRIVEN" or "POLLING"
     */
    public String getCurrentMode() {
        return eventDrivenMode.get() ? "EVENT_DRIVEN" : "POLLING";
    }

    /**
     * Get Redis health status.
     *
     * @return true if healthy
     */
    public boolean isRedisHealthy() {
        return redisHealthy.get();
    }

    /**
     * Cleanup on shutdown.
     */
    @PreDestroy
    public void destroy() {
        log.info("Shutting down config refresh service");
        processedEvents.clear();
    }

    /**
     * DTO for Redis event deserialization.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ClientAccessControlEventDto {
        private String eventId;
        private Instant timestamp;
        private String eventType;
        private String operation;
        private List<String> clientIds;
    }
}
