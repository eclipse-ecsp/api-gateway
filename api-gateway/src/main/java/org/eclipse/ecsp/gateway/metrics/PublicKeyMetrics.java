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

import jakarta.annotation.PostConstruct;
import org.eclipse.ecsp.gateway.annotations.ConditionOnPublicKeyMetricsEnabled;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Main metrics coordinator for public key cache monitoring.
 * Delegates responsibilities to specialized components for better maintainability.
 *
 * @author Abhishek Kumar
 */
@Component
@ConditionOnPublicKeyMetricsEnabled
public class PublicKeyMetrics {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PublicKeyMetrics.class);

    private final PublicKeyCacheMetricsRegistrar cacheMetricsRegistrar;
    private final PublicKeyRefreshMetricsRecorder refreshMetricsRecorder;

    
    /**
     * Constructor with refactored dependencies.
     *
     * @param cacheMetricsRegistrar cache metrics registrar
     * @param refreshMetricsRecorder refresh metrics recorder
     */
    public PublicKeyMetrics(PublicKeyCacheMetricsRegistrar cacheMetricsRegistrar,
                            PublicKeyRefreshMetricsRecorder refreshMetricsRecorder) {
        this.cacheMetricsRegistrar = cacheMetricsRegistrar;
        this.refreshMetricsRecorder = refreshMetricsRecorder;
    }

    /**
     * Initialize metrics using specialized components.
     * Uses graceful fallback if some metrics fail to register.
     */
    @PostConstruct
    public void initializeMetrics() {
        LOGGER.info("Initializing public key cache metrics");

        boolean cacheMetricsRegistered = false;
        boolean refreshMetricsRegistered = false;

        try {
            cacheMetricsRegistrar.registerCacheMetrics();
            cacheMetricsRegistered = true;
            LOGGER.debug("Cache metrics registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register cache metrics, continuing with partial metrics", e);
        }

        try {
            refreshMetricsRecorder.registerRefreshMetrics();
            refreshMetricsRegistered = true;
            LOGGER.debug("Refresh metrics registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register refresh metrics, continuing with partial metrics", e);
        }

        if (cacheMetricsRegistered || refreshMetricsRegistered) {
            LOGGER.info("Public key cache metrics initialized (cache: {}, refresh: {})",
                    cacheMetricsRegistered, refreshMetricsRegistered);
        } else {
            LOGGER.warn("Failed to initialize any public key cache metrics - monitoring may be limited");
        }
    }

    /**
     * Public API for recording source refresh - delegates to specialized component.
     */
    public void recordSourceRefresh(String sourceId) {
        refreshMetricsRecorder.recordSourceRefresh(sourceId);
    }

    /**
     * Public API for invalidating cache - delegates to specialized component.
     */
    public void invalidateKeySourcesCache() {
        cacheMetricsRegistrar.invalidateKeySourcesCache();
    }
}
