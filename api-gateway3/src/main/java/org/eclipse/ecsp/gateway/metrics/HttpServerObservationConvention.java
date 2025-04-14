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

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.GatewayUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.util.CollectionUtils;

/**
 * HttpServerObservationConvention overrides DefaultServerRequestObservationConvention.
 * enhances http.server.metrics with service name and url.
 */
@NoArgsConstructor
public class HttpServerObservationConvention extends DefaultServerRequestObservationConvention {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(HttpServerObservationConvention.class);

    private String metricsName = "http.server.requests";

    /**
     * Constructor to initialize HttpServerObservationConvention.
     *
     * @param metricsName override metrics name, if not provided http.server.requests will be used.
     */
    public HttpServerObservationConvention(String metricsName) {
        if (StringUtils.isNotEmpty(metricsName)) {
            this.metricsName = metricsName;
        }
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        // Make sure that KeyValues entries are already sorted by name for better performance
        return KeyValues.of(exception(context),
                method(context),
                outcomeStatus(context),
                requestUrl(context),
                serviceName(context),
                status(context));
    }

    /**
     * create KeyValue for with service name.
     *
     * @param context ServerRequestObservationContext
     * @return KeyValue with service name.
     */
    protected KeyValue serviceName(ServerRequestObservationContext context) {
        String serviceName = GatewayConstants.UNKNOWN;
        Route route = (Route) context.getAttributes().get(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null && !CollectionUtils.isEmpty(route.getMetadata())
                && route.getMetadata().containsKey(GatewayConstants.SERVICE_NAME)) {
            serviceName = (String) route.getMetadata().get(GatewayConstants.SERVICE_NAME);
        }
        LOGGER.debug("Service name appended to http request metrics: {}", serviceName);
        return KeyValue.of(GatewayConstants.SERVICE, serviceName);
    }

    /**
     * create KeyValue for with request url.
     * create with value UNKNOWN if url is not available.
     *
     * @param context ServerRequestObservationContext
     * @return KeyValue with request url.
     */
    protected KeyValue requestUrl(ServerRequestObservationContext context) {
        KeyValue url = uri(context);
        if (url.getValue().equals(GatewayConstants.UNKNOWN)) {
            url = KeyValue.of("uri", context.getCarrier().getPath().toString());
        }
        LOGGER.debug("appended to http request url to metrics: {}", url.getValue());
        return url;
    }

    /**
     * create KeyValue for with response outcome.
     * create with value UNKNOWN if response is not available.
     *
     * @param context ServerRequestObservationContext
     * @return KeyValue with response outcome.
     */
    protected KeyValue outcomeStatus(ServerRequestObservationContext context) {
        KeyValue outcome = outcome(context);
        if (outcome.getValue().equals(GatewayConstants.UNKNOWN) && context.getResponse() != null) {
            String outcomeStatus = getStatus(context.getResponse());
            outcome = KeyValue.of("outcome", outcomeStatus != null ? outcomeStatus : GatewayConstants.UNKNOWN);
        }
        return outcome;
    }

    /**
     * create KeyValue for with response status.
     * create with value UNKNOWN if response status is not available.
     *
     * @param response ServerHttpResponse
     * @return KeyValue with response status.
     */
    private String getStatus(ServerHttpResponse response) {
        String status;
        try {
            HttpStatusCode statusCode = response.getStatusCode();
            status = GatewayUtils.getOutcomeFromHttpStatus(statusCode);
        } catch (Exception e) {
            status = GatewayConstants.UNKNOWN;
        }
        return status;
    }

    @Override
    public String getName() {
        return metricsName;
    }
}
