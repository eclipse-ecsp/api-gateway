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

package org.eclipse.ecsp.registry.config;

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import java.util.List;

/**
 * RegistryConfig.
 */
@Configuration
@ConditionalOnProperty(name = "api-registry.metrics.enabled", matchIfMissing = true, havingValue = "true")
public class RegistryMetricsConfig {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RegistryMetricsConfig.class);

    private static final List<String> EXPOSE_ENDPOINTS = List.of("health", "prometheus");

    /**
     * constructor to create instance of RegistryMetricsConfig.
     *
     * @param configurableEnvironment environment config.
     */
    public RegistryMetricsConfig(ConfigurableEnvironment configurableEnvironment) {
        LOGGER.info("RegistryMetricsConfig initialized with expose endpoints: {}", EXPOSE_ENDPOINTS);
        //configurableEnvironment.getSystemProperties().put("management.endpoints.access.default", "none");
        configurableEnvironment.getSystemProperties().put("management.endpoints.web.exposure.include",
                String.join(",", EXPOSE_ENDPOINTS));
        configurableEnvironment.getSystemProperties().put("management.endpoints.jmx.exposure.exclude", "*");
        configurableEnvironment.getSystemProperties().put("management.endpoints.web.exposure.exclude",
                "gateway, beans, cache, conditions, configprops, auditevents, env, flyway, httpexchanges, "
                        + "info, integrationgraph, loggers, liquibase, metrics, mappings, quartz, scheduledtasks, "
                        + "sessions, shutdown, startup, threaddump, heapdump, logfile");

        // disable all the metrics export by default
        configurableEnvironment.getSystemProperties().put("management.defaults.metrics.export.enabled" , "false");
    }

    /**
     * EndpointFilter to restrict exposing endpoints other than defined in exposeEndpoints.
     *
     * @return instance of {@link EndpointFilter}
     */
    @Bean
    public EndpointFilter<ExposableWebEndpoint> registryEndpointFilter() {
        return (endpoint -> {
            LOGGER.debug("RegistryMetricsConfig registryEndpointFilter called for endpoint: {}", endpoint.getEndpointId());
            boolean endpointToBeExposed = EXPOSE_ENDPOINTS.contains(endpoint.getEndpointId().toLowerCaseString());
            LOGGER.info("RegistryMetricsConfig {} endpoint enabled is {}",
                    endpoint.getEndpointId().toLowerCaseString(), endpointToBeExposed);
            return endpointToBeExposed;
        });
    }
}
