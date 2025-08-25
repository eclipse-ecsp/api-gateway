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

package org.eclipse.ecsp.gateway.metrics;

import org.eclipse.ecsp.gateway.annotations.ConditionOnPublicKeyMetricsEnabled;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent.RefreshType;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Handles PublicKeyRefreshEvent processing for metrics updates.
 * Separates event handling logic from metrics registration.
 */
@Component
@ConditionOnPublicKeyMetricsEnabled
public class PublicKeyRefreshEventHandler {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PublicKeyRefreshEventHandler.class);

    private final PublicKeyRefreshMetricsRecorder metricsRecorder;
    private final PublicKeyCacheMetricsRegistrar cacheMetricsRegistrar;

    public PublicKeyRefreshEventHandler(PublicKeyRefreshMetricsRecorder metricsRecorder,
                                       PublicKeyCacheMetricsRegistrar cacheMetricsRegistrar) {
        this.metricsRecorder = metricsRecorder;
        this.cacheMetricsRegistrar = cacheMetricsRegistrar;
    }

    /**
     * Handle PublicKeyRefreshEvent to update metrics.
     */
    @EventListener(PublicKeyRefreshEvent.class)
    public void handleMetricsEvents(PublicKeyRefreshEvent publicKeyRefreshEvent) {
        if (publicKeyRefreshEvent == null) {
            LOGGER.warn("Received null PublicKeyRefreshEvent, ignoring");
            return;
        }

        LOGGER.info("Handling PublicKeyRefreshEvent: {}", publicKeyRefreshEvent);

        try {
            handleRefreshEventByType(publicKeyRefreshEvent);
        } catch (Exception e) {
            LOGGER.error("Error handling PublicKeyRefreshEvent: {}", publicKeyRefreshEvent, e);
        }
    }

    /**
     * Handle refresh event based on its type.
     */
    private void handleRefreshEventByType(PublicKeyRefreshEvent event) {
        RefreshType refreshType = event.getRefreshType();

        if (RefreshType.ALL_KEYS.equals(refreshType)) {
            // Full refresh metrics removed as they only happen at startup and provide no operational value
            LOGGER.debug("Received full refresh event - invalidating key sources cache only");
            cacheMetricsRegistrar.invalidateKeySourcesCache();
        } else if (RefreshType.PUBLIC_KEY.equals(refreshType)) {
            String sourceId = event.getSourceId();
            if (sourceId != null) {
                metricsRecorder.recordSourceRefresh(sourceId);
            } else {
                LOGGER.warn("Received source refresh event without source ID, ignoring");
            }
        } else {
            LOGGER.debug("Unhandled refresh type: {}", refreshType);
        }
    }
}
