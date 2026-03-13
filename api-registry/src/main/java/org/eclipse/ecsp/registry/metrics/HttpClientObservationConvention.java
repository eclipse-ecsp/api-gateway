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

package org.eclipse.ecsp.registry.metrics;


import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import jakarta.annotation.Nonnull;
import org.eclipse.ecsp.registry.utils.RegistryConstants;
import org.eclipse.ecsp.registry.utils.RegistryUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.stereotype.Component;

/**
 * HttpClientObservationConvention overrides DefaultClientRequestObservationConvention.
 * include request url and status in the metrics.
 *
 * @author Abhishek Kumar
 */
@Component
@ConditionalOnProperty(name = "api-registry.metrics.enabled", matchIfMissing = true, havingValue = "true")
public class HttpClientObservationConvention extends DefaultClientRequestObservationConvention {
    /**
     * Default constructor.
     */
    public HttpClientObservationConvention() {
        // Default constructor
    }

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(HttpClientObservationConvention.class);

    @Override
    public KeyValues getLowCardinalityKeyValues(ClientRequestObservationContext context) {
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
     * create {@link KeyValue} with request url.
     *
     * @param context client request observation context
     * @return {@link KeyValue} with request url
     */
    protected KeyValue url(ClientRequestObservationContext context) {
        KeyValue url = uri(context);
        if (url.getValue().equals("UNKNOWN")) {
            ClientHttpRequest request = context.getCarrier();
            if (request != null && request.getURI().getPath() != null) {
                url = KeyValue.of("uri", request.getURI().getPath());
            }
        }
        return url;
    }

    /**
     * create {@link KeyValue} with response outcome.
     *
     * @param context client request observation context
     * @return {@link KeyValue} with response outcome
     */
    protected KeyValue outcomeStatus(@Nonnull ClientRequestObservationContext context) {
        KeyValue outcome = outcome(context);
        if (outcome.getValue().equals(RegistryConstants.UNKNOWN)) {
            ClientHttpResponse response = context.getResponse();
            Throwable error = context.getError();
            String status = response != null ? getStatus(response) : RegistryConstants.UNKNOWN;
            outcome = KeyValue.of("outcome", error != null ? "ERROR" : status);
        }
        return outcome;
    }

    /**
     * create {@link KeyValue} with response status.
     *
     * @param response response
     * @return {@link KeyValue} with response status
     */
    private String getStatus(ClientHttpResponse response) {
        String status;
        try {
            HttpStatusCode statusCode = response.getStatusCode();
            status = RegistryUtils.getOutcomeFromHttpStatus(statusCode);
        } catch (Exception e) {
            status = RegistryConstants.UNKNOWN;
        }
        return status;
    }
}
