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

package org.eclipse.ecsp.gateway.metrics;

import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent.RefreshType;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent.Trigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PublicKeyRefreshEventHandlerTest {

    @Mock
    private PublicKeyRefreshMetricsRecorder metricsRecorder;

    @Mock
    private PublicKeyCacheMetricsRegistrar cacheMetricsRegistrar;

    private PublicKeyRefreshEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new PublicKeyRefreshEventHandler(metricsRecorder, cacheMetricsRegistrar);
    }

    @Test
    @DisplayName("Should handle null event gracefully")
    void testHandleMetricsEventsNullEvent() {
        assertDoesNotThrow(() -> eventHandler.handleMetricsEvents(null));
        verifyNoInteractions(metricsRecorder, cacheMetricsRegistrar);
    }

    @Test
    @DisplayName("Should handle ALL_KEYS refresh event")
    void testHandleAllKeysRefreshEvent() {
        PublicKeyRefreshEvent event = new PublicKeyRefreshEvent(RefreshType.ALL_KEYS, "source-all");
        eventHandler.handleMetricsEvents(event);

        verify(cacheMetricsRegistrar).invalidateKeySourcesCache();
        verifyNoInteractions(metricsRecorder);
    }

    @Test
    @DisplayName("Should record metrics for UNKNOWN_KID trigger with success outcome")
    void testHandleUnknownKidSuccess() {
        PublicKeyRefreshEvent event = new PublicKeyRefreshEvent(
                RefreshType.PUBLIC_KEY, "source-jwks", Trigger.UNKNOWN_KID, "success");
        eventHandler.handleMetricsEvents(event);

        verify(metricsRecorder).recordUnknownKid("source-jwks");
        verify(metricsRecorder).recordForcedRefresh("source-jwks", "success");
        verify(metricsRecorder).recordKidRecovered("source-jwks");
        verify(metricsRecorder, never()).recordSourceRefresh(anyString());
    }

    @Test
    @DisplayName("Should record metrics for UNKNOWN_KID trigger with failure outcome")
    void testHandleUnknownKidFailure() {
        PublicKeyRefreshEvent event = new PublicKeyRefreshEvent(
                RefreshType.PUBLIC_KEY, "source-jwks", Trigger.UNKNOWN_KID, "failure");
        eventHandler.handleMetricsEvents(event);

        verify(metricsRecorder).recordUnknownKid("source-jwks");
        verify(metricsRecorder).recordForcedRefresh("source-jwks", "failure");
        verify(metricsRecorder, never()).recordKidRecovered(anyString());
    }

    @Test
    @DisplayName("Should record metrics for UNKNOWN_KID trigger with suppressed outcome")
    void testHandleUnknownKidSuppressed() {
        PublicKeyRefreshEvent event = new PublicKeyRefreshEvent(
                RefreshType.PUBLIC_KEY, "source-jwks", Trigger.UNKNOWN_KID, "suppressed");
        eventHandler.handleMetricsEvents(event);

        verify(metricsRecorder).recordUnknownKid("source-jwks");
        verify(metricsRecorder).recordForcedRefresh("source-jwks", "suppressed");
        verify(metricsRecorder, never()).recordKidRecovered(anyString());
    }

    @Test
    @DisplayName("Should record scheduled source refresh when trigger is not UNKNOWN_KID")
    void testHandleScheduledSourceRefresh() {
        PublicKeyRefreshEvent event = new PublicKeyRefreshEvent(
                RefreshType.PUBLIC_KEY, "source-jwks", Trigger.SCHEDULED, "success");
        eventHandler.handleMetricsEvents(event);

        verify(metricsRecorder).recordSourceRefresh("source-jwks");
        verify(metricsRecorder, never()).recordUnknownKid(anyString());
        verify(metricsRecorder, never()).recordForcedRefresh(anyString(), anyString());
    }

    @Test
    @DisplayName("Should ignore PUBLIC_KEY refresh event when sourceId is null")
    void testHandlePublicKeyRefreshEventWithoutSourceId() {
        PublicKeyRefreshEvent event = new PublicKeyRefreshEvent(RefreshType.PUBLIC_KEY, null);
        eventHandler.handleMetricsEvents(event);

        verifyNoInteractions(metricsRecorder);
    }

    @Test
    @DisplayName("Should handle event with null refresh type gracefully")
    void testHandleEventWithNullRefreshType() {
        PublicKeyRefreshEvent event = mock(PublicKeyRefreshEvent.class);
        eventHandler.handleMetricsEvents(event);

        verifyNoInteractions(metricsRecorder, cacheMetricsRegistrar);
    }

    @Test
    @DisplayName("Should handle exception thrown during event handling gracefully")
    void testHandleEventExceptionHandling() {
        doThrow(new RuntimeException("Invalidation error"))
                .when(cacheMetricsRegistrar).invalidateKeySourcesCache();

        PublicKeyRefreshEvent event = new PublicKeyRefreshEvent(RefreshType.ALL_KEYS, "source-all");
        assertDoesNotThrow(() -> eventHandler.handleMetricsEvents(event));
    }
}
