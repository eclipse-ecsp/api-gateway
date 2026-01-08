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

package org.eclipse.ecsp.registry.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.eclipse.ecsp.registry.config.EventProperties;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Event throttler that implements debouncing with timer reset.
 * Each new event resets the timer, ensuring events are only published
 * after a period of inactivity.
 */
@Component
@ConditionalOnProperty(name = "api-registry.events.enabled", havingValue = "true")
public class RouteEventThrottler {

    private static final int FIVE = 5;

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteEventThrottler.class);

    private final ScheduledExecutorService scheduler;
    private final Set<String> pendingServiceNames;
    private final long debounceDelayMs;
    private final RedisTemplate<String, String> redisTemplate;
    private final String channel;
    private final ObjectMapper objectMapper;
    private final AtomicReference<ScheduledFuture<?>> scheduledFlush = new AtomicReference<>();

    /**
     * Constructor for RouteEventThrottler.
     *
     * @param eventProperties configuration properties
     * @param redisTemplate   Redis template for publishing events
     * @param objectMapper    JSON object mapper
     */
    public RouteEventThrottler(EventProperties eventProperties,
                               RedisTemplate<String, String> redisTemplate,
                               ObjectMapper objectMapper) {
        this.debounceDelayMs = eventProperties.getRedis().getDebounceDelayMs();
        this.channel = eventProperties.getRedis().getChannel();
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "route-event-throttler");
            thread.setDaemon(true);
            return thread;
        });
        this.pendingServiceNames = ConcurrentHashMap.newKeySet();
        LOGGER.info("RouteEventThrottler initialized with debounce delay: {}ms, channel: {}",
                debounceDelayMs, channel);
    }

    /**
     * Schedule an event for a service. Resets the debounce timer.
     *
     * @param serviceName name of the service that changed
     */
    public void scheduleEvent(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            LOGGER.warn("Attempted to schedule event with null or empty service name");
            return;
        }

        pendingServiceNames.add(serviceName);
        resetDebounceTimer();
        LOGGER.debug("Scheduled event for service: {}, pending count: {}, timer reset",
                serviceName, pendingServiceNames.size());
    }

    /**
     * Reset the debounce timer. Cancels existing timer and schedules new flush.
     */
    private synchronized void resetDebounceTimer() {
        // Cancel existing timer if present
        ScheduledFuture<?> existing = scheduledFlush.get();
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
            LOGGER.trace("Cancelled existing debounce timer");
        }

        // Schedule new flush after debounce period
        scheduledFlush.set(scheduler.schedule(this::flush, debounceDelayMs, TimeUnit.MILLISECONDS));
        LOGGER.trace("Scheduled new flush in {}ms", debounceDelayMs);
    }

    /**
     * Flush pending events - publishes consolidated event to Redis.
     */
    private void flush() {
        if (pendingServiceNames.isEmpty()) {
            LOGGER.trace("No pending services to flush");
            return;
        }
        boolean sent = sendEvent(RouteEventType.ROUTE_CHANGE, List.copyOf(pendingServiceNames), List.of());
        if (sent) {
            pendingServiceNames.clear();
        }
    }

    /**
     * Send event immediately.
     *
     * @param eventType   type of route event
     * @param serviceName list of service names that changed
     */
    public boolean sendEvent(RouteEventType eventType, List<String> serviceName, List<String> routeIds) {
        try {
            // Create consolidated event with all pending services
            RouteChangeEvent event = new RouteChangeEvent(eventType, 
                                        serviceName, 
                                        routeIds);
            String eventJson = objectMapper.writeValueAsString(event);

            // Publish to Redis
            redisTemplate.convertAndSend(channel, eventJson);

            LOGGER.info("Published {} change event: eventId={}, serviceCount={}, services={}",
                    eventType, event.getEventId(), event.getServices().size(), event.getServices());
            return true;

        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize route change event", e);
        } catch (Exception e) {
            LOGGER.error("Failed to publish route change event to Redis", e);
        }
        return false;
    }

    /**
     * Shutdown the scheduler gracefully.
     */
    @PreDestroy
    public void shutdown() {
        LOGGER.info("Shutting down RouteEventThrottler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(FIVE, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get count of pending services (for testing).
     *
     * @return number of pending services
     */
    int getPendingServiceCount() {
        return pendingServiceNames.size();
    }
}
