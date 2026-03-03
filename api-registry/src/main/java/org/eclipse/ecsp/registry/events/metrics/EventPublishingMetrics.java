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

package org.eclipse.ecsp.registry.events.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.eclipse.ecsp.registry.events.RouteEventType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Reusable component for tracking event publishing metrics.
 * Provides centralized metrics management for all event publishing strategies.
 */
@Component
public class EventPublishingMetrics {
    
    private static final String EVENT_TYPE_TAG = "eventType";
    
    private final MeterRegistry meterRegistry;
    private final Map<RouteEventType, Counter> successCounters = new ConcurrentHashMap<>();
    private final Map<RouteEventType, Counter> failureCounters = new ConcurrentHashMap<>();
    private final Map<RouteEventType, Timer> publishTimers = new ConcurrentHashMap<>();
    
    /**
     * Constructor for EventPublishingMetrics.
     *
     * @param meterRegistry meter registry for creating metrics
     */
    public EventPublishingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Get or create success counter for an event type.
     *
     * @param eventType type of event
     * @return success counter
     */
    private Counter getSuccessCounter(RouteEventType eventType) {
        return successCounters.computeIfAbsent(eventType, type ->
            Counter.builder("events.published.success")
                .tag(EVENT_TYPE_TAG, type.name())
                .description("Number of successfully published events")
                .register(meterRegistry)
        );
    }
    
    /**
     * Get or create failure counter for an event type.
     *
     * @param eventType type of event
     * @return failure counter
     */
    private Counter getFailureCounter(RouteEventType eventType) {
        return failureCounters.computeIfAbsent(eventType, type ->
            Counter.builder("events.published.failure")
                .tag(EVENT_TYPE_TAG, type.name())
                .description("Number of failed event publishes")
                .register(meterRegistry)
        );
    }
    
    /**
     * Get or create publish duration timer for an event type.
     *
     * @param eventType type of event
     * @return publish timer
     */
    private Timer getPublishTimer(RouteEventType eventType) {
        return publishTimers.computeIfAbsent(eventType, type ->
            Timer.builder("events.published.duration")
                .tag(EVENT_TYPE_TAG, type.name())
                .description("Time taken to publish events")
                .register(meterRegistry)
        );
    }
    
    /**
     * Record a successful event publish.
     *
     * @param eventType type of event
     */
    public void recordSuccess(RouteEventType eventType) {
        getSuccessCounter(eventType).increment();
    }
    
    /**
     * Record a failed event publish.
     *
     * @param eventType type of event
     */
    public void recordFailure(RouteEventType eventType) {
        getFailureCounter(eventType).increment();
    }
    
    /**
     * Execute and measure the duration of a publish operation.
     * Records success or failure based on the result.
     * Note: Supplier&lt;Boolean&gt; is used instead of BooleanSupplier because
     * Timer.record() requires a Supplier&lt;T&gt; for returning values.
     *
     * @param eventType The type of event being published
     * @param publishOperation The publish operation to execute
     * @return true if publish succeeded, false otherwise
     */
    @SuppressWarnings("squid:S4276")
    public boolean recordPublish(RouteEventType eventType, Supplier<Boolean> publishOperation) {
        return Boolean.TRUE.equals(getPublishTimer(eventType).record(() -> {
            boolean success = publishOperation.get();
            if (success) {
                recordSuccess(eventType);
            } else {
                recordFailure(eventType);
            }
            return success;
        }));
    }
    
    /**
     * Execute and measure the duration of a publish operation (void return).
     * Assumes success if no exception is thrown.
     *
     * @param eventType type of event
     * @param publishOperation publish operation to execute
     */
    public void recordPublish(RouteEventType eventType, Runnable publishOperation) {
        getPublishTimer(eventType).record(() -> {
            try {
                publishOperation.run();
                recordSuccess(eventType);
            } catch (Exception e) {
                recordFailure(eventType);
                throw e;
            }
        });
    }
}
