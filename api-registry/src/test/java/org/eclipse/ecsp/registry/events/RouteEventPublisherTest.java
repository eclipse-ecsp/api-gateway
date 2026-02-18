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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for RouteEventPublisher.
 */
@ExtendWith(MockitoExtension.class)
class RouteEventPublisherTest {

    @Mock
    private RouteEventThrottler throttler;

    private RouteEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RouteEventPublisher(throttler);
    }

    /**
     * Test purpose          - Verify application ready event triggers initial publish.
     * Test data             - ApplicationReadyEvent.
     * Test expected result  - Throttler sendEvent called with "all" services.
     * Test type             - Positive.
     */
    @Test
    void testOnApplicationReady_PublishesInitialEvent() {
        // Arrange
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        // Act
        publisher.onApplicationReady(event);

        // Assert
        verify(throttler, times(1)).sendEvent(
                any(org.eclipse.ecsp.registry.events.data.RouteChangeEventData.class));
    }

    /**
     * Test purpose          - Verify constructor initializes publisher.
     * Test data             - Mock throttler.
     * Test expected result  - Publisher created successfully.
     * Test type             - Positive.
     */
    @Test
    void testConstructor_InitializesSuccessfully() {
        // Arrange & Act
        RouteEventPublisher newPublisher = new RouteEventPublisher(throttler);

        // Assert
        assertThat(newPublisher).isNotNull();
    }
}

