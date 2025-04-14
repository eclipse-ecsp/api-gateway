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
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.ecsp.registry.utils.RegistryConstants;
import org.eclipse.ecsp.registry.utils.RegistryUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

/**
 * HttpServerObservationConvention overrides DefaultServerRequestObservationConvention.
 * enhances http server metrics.
 *
 * @author Abhishek Kumar
 */
@Component
@ConditionalOnProperty(name = "api-registry.metrics.enabled", matchIfMissing = true, havingValue = "true")
public class HttpServerObservationConvention extends DefaultServerRequestObservationConvention {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(HttpServerObservationConvention.class);

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        // Make sure that KeyValues entries are already sorted by name for better performance
        return KeyValues.of(
                exception(context),
                method(context),
                outcomeStatus(context),
                status(context),
                url(context));
    }

    /**
     * create {@link KeyValue} with request url.
     *
     * @param context server request observation context
     * @return KeyValue with request url.
     */
    protected KeyValue url(ServerRequestObservationContext context) {
        KeyValue url = uri(context);
        if (url.getValue().equals(RegistryConstants.UNKNOWN)) {
            url = KeyValue.of("uri", context.getCarrier().getRequestURI());
        }
        return url;
    }

    /**
     * create {@link KeyValue} with response outcome.
     *
     * @param context server request observation context
     * @return KeyValue with outcome.
     */
    protected KeyValue outcomeStatus(ServerRequestObservationContext context) {
        KeyValue outcome = outcome(context);
        if (outcome.getValue().equals(RegistryConstants.UNKNOWN) && context.getResponse() != null) {
            outcome = KeyValue.of("outcome", context.getError() != null ? "ERROR" : getStatus(context.getResponse()));
        }
        return outcome;
    }

    /**
     * create {@link KeyValue} with response status.
     *
     * @param response response
     * @return KeyValue with response status.
     */
    private String getStatus(HttpServletResponse response) {
        String status;
        try {
            HttpStatusCode statusCode = HttpStatusCode.valueOf(response.getStatus());
            status = RegistryUtils.getOutcomeFromHttpStatus(statusCode);
        } catch (Exception e) {
            status = "UNKNOWN";
        }
        return status;
    }
}
