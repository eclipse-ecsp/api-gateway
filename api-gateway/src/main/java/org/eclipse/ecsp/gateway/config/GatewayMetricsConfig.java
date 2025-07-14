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

package org.eclipse.ecsp.gateway.config;


import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import org.eclipse.ecsp.gateway.metrics.ApiGatewayObservationConvention;
import org.eclipse.ecsp.gateway.metrics.GatewayMeterFilter;
import org.eclipse.ecsp.gateway.metrics.GatewayMetricsProperties;
import org.eclipse.ecsp.gateway.metrics.HttpClientObservationConvention;
import org.eclipse.ecsp.gateway.metrics.HttpServerObservationConvention;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.headers.observation.GatewayObservationConvention;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.gateway.support.tagsprovider.GatewayTagsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ClientRequestObservationConvention;
import java.util.List;
import java.util.Objects;

/**
 * Configuration class for api gateway metrics.
 *
 * @author Abhishek Kumar
 */
@Configuration
@ConditionalOnProperty(name = "api.gateway.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class GatewayMetricsConfig {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(GatewayMetricsConfig.class);
    private static final List<String> EXPOSE_ENDPOINTS = List.of(GatewayConstants.HEALTH, GatewayConstants.PROMETHEUS);
    private final GatewayMetricsProperties gatewayMetricsProperties;


    /**
     * constructor to create instance of GatewayMetricsConfig.
     *
     * @param gatewayMetricsProperties metrics properties.
     * @param configurableEnvironment  environment config.
     */
    public GatewayMetricsConfig(GatewayMetricsProperties gatewayMetricsProperties,
                                ConfigurableEnvironment configurableEnvironment) {
        this.gatewayMetricsProperties = gatewayMetricsProperties;
        configurableEnvironment.getSystemProperties().put("management.endpoints.web.exposure.include",
                String.join(",", EXPOSE_ENDPOINTS));
        configurableEnvironment.getSystemProperties().put("management.endpoints.jmx.exposure.exclude", "*");
        configurableEnvironment.getSystemProperties().put("management.endpoints.web.exposure.exclude",
                "gateway, beans, cache, conditions, configprops, auditevents, env, flyway, httpexchanges, "
                        + "info, integrationgraph, loggers, liquibase, metrics, mappings, quartz, scheduledtasks, "
                        + "sessions, shutdown, startup, threaddump, heapdump, logfile");

        // disable all the metrics export by default
        configurableEnvironment.getSystemProperties().put("management.endpoints.access.default", "none");
        configurableEnvironment.getSystemProperties().put("management.defaults.metrics.export.enabled", "false");
    }


    /**
     * EndpointFilter to restrict exposing endpoints other than defined in exposeEndpoints.
     *
     * @return instance of {@link EndpointFilter}
     */
    @Bean
    EndpointFilter<ExposableWebEndpoint> gatewayEndpointFilter() {
        return (endpoint -> {
            LOGGER.debug("GatewayMetrics endpoint filter for endpoint: {}",
                    endpoint.getEndpointId().toLowerCaseString());
            boolean endpointToBeExposed = EXPOSE_ENDPOINTS.contains(endpoint.getEndpointId().toLowerCaseString());
            LOGGER.info("GatewayMetrics {} endpoint enabled is {}",
                    endpoint.getEndpointId().toLowerCaseString(),
                    endpointToBeExposed);
            return endpointToBeExposed;
        });
    }

    /**
     * add service tag in api gateway metrics using {@link GatewayTagsProvider}.
     *
     * @return instance of service tag provider.
     */
    @Bean
    @ConditionalOnProperty(name = "api.gateway.metrics.gateway-requests.enabled", havingValue = "true")
    public GatewayTagsProvider serviceNameTagProvider() {
        return (exchange -> {
            LOGGER.debug("apply service name in gateway metrics");
            // Get the route information from the exchange attributes
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if (route != null && !CollectionUtils.isEmpty(route.getMetadata())
                    && !Objects.isNull(route.getMetadata().get(GatewayConstants.SERVICE_NAME))) {
                LOGGER.debug("applied service name: {} in gateway metrics",
                        route.getMetadata().get(GatewayConstants.SERVICE_NAME));
                return Tags.of(
                        Tag.of("service",
                                String.valueOf(route.getMetadata().get(GatewayConstants.SERVICE_NAME))
                        ));
            }
            return Tags.empty();
        });
    }

    /**
     * add requestUrl tag in api gateway metrics using {@link GatewayTagsProvider}.
     *
     * @return instance of {@link GatewayTagsProvider}
     */
    @Bean
    @ConditionalOnProperty(name = "api.gateway.metrics.gateway-requests.enabled", havingValue = "true")
    public GatewayTagsProvider requestUrlTagProvider() {
        return (exchange -> {
            LOGGER.debug("apply route path in gateway metrics", exchange.getRequest().getPath());
            // Get the route information from the exchange attributes
            return Tags.of(Tags.of("requestUrl", exchange.getRequest().getPath().toString()));
        });
    }

    /**
     * Used by for each request to controller,below class with add serviceName for http.server.request metrics.
     *
     * @return instance of {@link HttpServerObservationConvention}.
     */
    @Bean
    @ConditionalOnProperty("api.gateway.metrics.server-requests.enabled")
    public ServerRequestObservationConvention httpServerObservationConvention() {
        LOGGER.info("HttpServerObservationConvention is enabled");
        return new HttpServerObservationConvention(gatewayMetricsProperties.getServerRequests().getPrefix());
    }

    /**
     * Used by webClient to add serviceName for http.client.request metrics.
     *
     * @return instance of {@link HttpClientObservationConvention}
     */
    @Bean
    @ConditionalOnProperty("api.gateway.metrics.http-client-requests.enabled")
    ClientRequestObservationConvention httpClientObservationConvention() {
        LOGGER.info("HttpClientObservationConvention is enabled");
        return new HttpClientObservationConvention(gatewayMetricsProperties.getHttpClientRequests().getPrefix());
    }

    /**
     * add backend metrics with httpClient used by NettyRoutingFilter.
     *
     * @return instance of {@link ApiGatewayObservationConvention}
     */
    @Bean
    @ConditionalOnProperty("api.gateway.metrics.backend-requests.enabled")
    GatewayObservationConvention apiGatewayObservationConvention() {
        LOGGER.info("ApiGatewayObservationConvention is enabled");
        return new ApiGatewayObservationConvention(gatewayMetricsProperties.getBackendRequests().getPrefix());
    }

    /**
     * {@link MeterFilter} for gateway metrics.
     *
     * @return instance of {@link GatewayMeterFilter}
     */
    @Bean
    MeterFilter gatewayMeterFilter() {
        LOGGER.info("GatewayMeterFilter for gateway Request is enabled");
        return new GatewayMeterFilter<>(gatewayMetricsProperties.getGatewayRequests());
    }

    /**
     * {@link MeterFilter} for server metrics.
     *
     * @return instance of {@link GatewayMeterFilter}
     */
    @Bean
    MeterFilter serverRequestMeterFilter() {
        LOGGER.info("GatewayMeterFilter for server Request is enabled");
        return new GatewayMeterFilter<>(gatewayMetricsProperties.getServerRequests());
    }

    /**
     * {@link MeterFilter} for backend requests metrics.
     *
     * @return instance of {@link GatewayMeterFilter}
     */
    @Bean
    MeterFilter backendRequestMeterFilter() {
        LOGGER.info("GatewayMeterFilter for backend Request is enabled");
        return new GatewayMeterFilter<>(gatewayMetricsProperties.getBackendRequests());
    }

    /**
     * {@link MeterFilter} for http client metrics.
     *
     * @return instance of {@link GatewayMeterFilter}
     */
    @Bean
    MeterFilter httpClientRequestMeterFilter() {
        LOGGER.info("GatewayMeterFilter for backend Request is enabled");
        return new GatewayMeterFilter<>(gatewayMetricsProperties.getHttpClientRequests());
    }

    /**
     * {@link MeterFilter} for route security metrics.
     *
     * @return instance of {@link GatewayMeterFilter}
     */
    @Bean
    MeterFilter securityMeterFilter() {
        LOGGER.info("GatewayMeterFilter for security metrics is enabled");
        return new GatewayMeterFilter<>(gatewayMetricsProperties.getSecurityMetrics());
    }
}
