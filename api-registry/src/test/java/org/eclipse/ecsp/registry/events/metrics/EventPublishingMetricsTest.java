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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.ecsp.registry.events.RouteEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for EventPublishingMetrics.
 *
 * <p>Tests metrics recording for event publishing operations.
 */
@ExtendWith(MockitoExtension.class)
class EventPublishingMetricsTest {

    private MeterRegistry meterRegistry;
    private EventPublishingMetrics metrics;

    @BeforeEach
    void setUp() {
        // GIVEN: Simple meter registry and metrics component
        meterRegistry = new SimpleMeterRegistry();
        metrics = new EventPublishingMetrics(meterRegistry);
    }

    /**
     * Test purpose          - Verify recordSuccess increments success counter.
     * Test data             - RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED
     * Test expected result  - Success counter incremented to 1.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void recordSuccess_ClientAccessControlEvent_IncrementSuccessCounter() {
        // GIVEN: Event type
        RouteEventType eventType = RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED;

        // WHEN: Record success
        metrics.recordSuccess(eventType);

        // THEN: Success counter should be incremented
        Counter counter = meterRegistry.find("events.published.success")
                .tag("eventType", eventType.name())
                .counter();

        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    /**
     * Test purpose          - Verify recordSuccess increments counter multiple times.
     * Test data             - Same event type called 3 times.
     * Test expected result  - Success counter equals 3.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void recordSuccess_CalledMultipleTimes_IncrementCounterCorrectly() {
        // GIVEN: Event type
        RouteEventType eventType = RouteEventType.RATE_LIMIT_CONFIG_CHANGE;

        // WHEN: Record success 3 times
        metrics.recordSuccess(eventType);
        metrics.recordSuccess(eventType);
        metrics.recordSuccess(eventType);

        // THEN: Success counter should be 3
        Counter counter = meterRegistry.find("events.published.success")
                .tag("eventType", eventType.name())
                .counter();

        assertNotNull(counter);
        assertEquals(3.0, counter.count());
    }

    /**
     * Test purpose          - Verify recordFailure increments failure counter.
     * Test data             - RouteEventType.ROUTE_CHANGE
     * Test expected result  - Failure counter incremented to 1.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void recordFailure_RouteChangeEvent_IncrementFailureCounter() {
        // GIVEN: Event type
        RouteEventType eventType = RouteEventType.ROUTE_CHANGE;

        // WHEN: Record failure
        metrics.recordFailure(eventType);

        // THEN: Failure counter should be incremented
        Counter counter = meterRegistry.find("events.published.failure")
                .tag("eventType", eventType.name())
                .counter();

        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    /**
     * Test purpose          - Verify recordFailure increments counter multiple times.
     * Test data             - Same event type called 2 times.
     * Test expected result  - Failure counter equals 2.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void recordFailure_CalledMultipleTimes_IncrementCounterCorrectly() {
        // GIVEN: Event type
        RouteEventType eventType = RouteEventType.SERVICE_HEALTH_CHANGE;

        // WHEN: Record failure 2 times
        metrics.recordFailure(eventType);
        metrics.recordFailure(eventType);

        // THEN: Failure counter should be 2
        Counter counter = meterRegistry.find("events.published.failure")
                .tag("eventType", eventType.name())
                .counter();

        assertNotNull(counter);
        assertEquals(2.0, counter.count());
    }

    /**
     * Test purpose          - Verify recordPublish with successful operation records success.
     * Test data             - Supplier returning true.
     * Test expected result  - Success counter incremented, timer recorded, returns true.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void recordPublish_SuccessfulOperation_RecordSuccessAndReturnTrue() {
        // GIVEN: Event type and successful operation
        RouteEventType eventType = RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED;
        Supplier<Boolean> operation = () -> true;

        // WHEN: Record publish
        boolean result = metrics.recordPublish(eventType, operation);

        // THEN: Should return true and record success
        assertTrue(result);

        Counter successCounter = meterRegistry.find("events.published.success")
                .tag("eventType", eventType.name())
                .counter();
        assertNotNull(successCounter);
        assertEquals(1.0, successCounter.count());

        Timer timer = meterRegistry.find("events.published.duration")
                .tag("eventType", eventType.name())
                .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
    }

    /**
     * Test purpose          - Verify recordPublish with failed operation records failure.
     * Test data             - Supplier returning false.
     * Test expected result  - Failure counter incremented, timer recorded, returns false.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void recordPublish_FailedOperation_RecordFailureAndReturnFalse() {
        // GIVEN: Event type and failed operation
        RouteEventType eventType = RouteEventType.RATE_LIMIT_CONFIG_CHANGE;
        Supplier<Boolean> operation = () -> false;

        // WHEN: Record publish
        boolean result = metrics.recordPublish(eventType, operation);

        // THEN: Should return false and record failure
        assertFalse(result);

        Counter failureCounter = meterRegistry.find("events.published.failure")
                .tag("eventType", eventType.name())
                .counter();
        assertNotNull(failureCounter);
        assertEquals(1.0, failureCounter.count());

        Timer timer = meterRegistry.find("events.published.duration")
                .tag("eventType", eventType.name())
                .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
    }

    /**
     * Test purpose          - Verify recordPublish executes operation and measures duration.
     * Test data             - Supplier that sets flag to true.
     * Test expected result  - Operation executed, flag set, metrics recorded.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void recordPublish_OperationExecution_ExecuteAndMeasureDuration() {
        // GIVEN: Event type and operation that sets a flag
        RouteEventType eventType = RouteEventType.ROUTE_CHANGE;
        AtomicBoolean operationExecuted = new AtomicBoolean(false);
        Supplier<Boolean> operation = () -> {
            operationExecuted.set(true);
            return true;
        };

        // WHEN: Record publish
        boolean result = metrics.recordPublish(eventType, operation);

        // THEN: Operation should be executed
        assertTrue(result);
        assertTrue(operationExecuted.get());

        Timer timer = meterRegistry.find("events.published.duration")
                .tag("eventType", eventType.name())
                .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS) > 0);
    }

    /**
     * Test purpose          - Verify recordPublish with Runnable records success when no exception.
     * Test data             - Runnable that completes normally.
     * Test expected result  - Success counter incremented, timer recorded.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void recordPublish_RunnableSuccessful_RecordSuccess() {
        // GIVEN: Event type and successful runnable
        RouteEventType eventType = RouteEventType.SERVICE_HEALTH_CHANGE;
        AtomicInteger executionCount = new AtomicInteger(0);
        Runnable operation = executionCount::incrementAndGet;

        // WHEN: Record publish with runnable
        metrics.recordPublish(eventType, operation);

        // THEN: Should record success and execute operation
        assertEquals(1, executionCount.get());

        Counter successCounter = meterRegistry.find("events.published.success")
                .tag("eventType", eventType.name())
                .counter();
        assertNotNull(successCounter);
        assertEquals(1.0, successCounter.count());

        Timer timer = meterRegistry.find("events.published.duration")
                .tag("eventType", eventType.name())
                .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
    }

    /**
     * Test purpose          - Verify recordPublish with Runnable records failure when exception thrown.
     * Test data             - Runnable that throws RuntimeException.
     * Test expected result  - Failure counter incremented, exception propagated.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void recordPublish_RunnableThrowsException_RecordFailureAndPropagateException() {
        // GIVEN: Event type and runnable that throws exception
        RouteEventType eventType = RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED;
        Runnable operation = () -> {
            throw new RuntimeException("Test exception");
        };

        // WHEN/THEN: Should throw exception and record failure
        assertThrows(RuntimeException.class, () -> metrics.recordPublish(eventType, operation));

        Counter failureCounter = meterRegistry.find("events.published.failure")
                .tag("eventType", eventType.name())
                .counter();
        assertNotNull(failureCounter);
        assertEquals(1.0, failureCounter.count());

        Timer timer = meterRegistry.find("events.published.duration")
                .tag("eventType", eventType.name())
                .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
    }

    /**
     * Test purpose          - Verify different event types create separate metrics.
     * Test data             - Two different event types.
     * Test expected result  - Separate counters for each event type.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void recordSuccess_DifferentEventTypes_CreateSeparateMetrics() {
        // GIVEN: Two different event types
        RouteEventType eventType1 = RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED;
        RouteEventType eventType2 = RouteEventType.RATE_LIMIT_CONFIG_CHANGE;

        // WHEN: Record success for both
        metrics.recordSuccess(eventType1);
        metrics.recordSuccess(eventType1);
        metrics.recordSuccess(eventType2);

        // THEN: Should have separate counters
        Counter counter1 = meterRegistry.find("events.published.success")
                .tag("eventType", eventType1.name())
                .counter();
        assertNotNull(counter1);
        assertEquals(2.0, counter1.count());

        Counter counter2 = meterRegistry.find("events.published.success")
                .tag("eventType", eventType2.name())
                .counter();
        assertNotNull(counter2);
        assertEquals(1.0, counter2.count());
    }
}
