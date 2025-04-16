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
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.GatewayUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientRequestObservationContext;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention;
import java.util.Objects;

/**
 * HttpClientObservationConvention overrides DefaultClientRequestObservationConvention.
 * enhances the http client metrics.
 */
public class HttpClientObservationConvention extends DefaultClientRequestObservationConvention {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(HttpClientObservationConvention.class);

    private String metricsName = "http.client.requests";

    /**
     * Constructor to initialize HttpClientObservationConvention.
     *
     * @param metricsName override metrics name, if null default metric name http.client.request will be used.
     */
    public HttpClientObservationConvention(String metricsName) {
        if (StringUtils.isNotEmpty(metricsName)) {
            this.metricsName = metricsName;
        }
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(@Nonnull ClientRequestObservationContext context) {
        LOGGER.debug("HttpClientObservationConvention append serviceName");
        // Make sure that KeyValues entries are already sorted by name for better performance
        return KeyValues.of(
                clientName(context),
                exception(context),
                method(context),
                outcomeStatus(context),
                status(context),
                url(context));
    }

    /**
     * create KeyValue for with request url.
     * create with value UNKNOWN if url is not available.
     *
     * @param context client request observation context.
     * @return KeyValue with request url.
     */
    protected KeyValue url(ClientRequestObservationContext context) {
        KeyValue url = super.uri(context);
        if (url.getValue().equals(GatewayConstants.UNKNOWN)) {
            ClientRequest request = context.getRequest();
            url = KeyValue.of("uri", Objects.nonNull(request) ? request.url().getPath() : "UNKNOWN");
        }
        return url;
    }

    /**
     * create KeyValue for with response outcome.
     * create with value UNKNOWN if response is not available.
     *
     * @param context client request observation context.
     * @return KeyValue with outcome.
     */
    protected KeyValue outcomeStatus(@Nonnull ClientRequestObservationContext context) {
        KeyValue outcome = outcome(context);
        if (outcome.getValue().equals(GatewayConstants.UNKNOWN)) {
            Throwable error = context.getError();
            ClientResponse response = context.getResponse();
            String status = response != null ? getStatus(response) : GatewayConstants.UNKNOWN;
            outcome = KeyValue.of("outcome", error != null ? "ERROR" : status);
        }
        return outcome;
    }

    /**
     * create KeyValue for with response status.
     * create with value UNKNOWN if response status is not available.
     *
     * @param response client response.
     * @return response status.
     */
    private String getStatus(ClientResponse response) {
        String status;
        try {
            HttpStatusCode statusCode = response.statusCode();
            status = GatewayUtils.getOutcomeFromHttpStatus(statusCode);
        } catch (Exception e) {
            status = GatewayConstants.UNKNOWN;
        }
        return status;
    }

    @Override
    public String getName() {
        return this.metricsName;
    }
}
