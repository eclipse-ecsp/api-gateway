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

import io.micrometer.common.KeyValues;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.cloud.gateway.filter.headers.observation.DefaultGatewayObservationConvention;
import org.springframework.cloud.gateway.filter.headers.observation.GatewayContext;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.util.CollectionUtils;

/**
 * ApiGatewayObservationConvention overrides DefaultGatewayObservationConvention.
 * to includes service name and requestUrl to metrics.
 *
 * @author Abhishek Kumar
 */
@NoArgsConstructor
public class ApiGatewayObservationConvention extends DefaultGatewayObservationConvention {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiGatewayObservationConvention.class);

    private String metricsName = "http.downstream.requests";

    /**
     * Constructor to initialize ApiGatewayObservationConvention with given metrics prefix.
     *
     * @param metricsName override default metrics name, default metrics name is http.downstream.requests.
     */
    public ApiGatewayObservationConvention(String metricsName) {
        if (StringUtils.isNotEmpty(metricsName)) {
            this.metricsName = metricsName;
        }
    }

    @NotNull
    @Override
    public KeyValues getLowCardinalityKeyValues(GatewayContext context) {
        KeyValues keyValues = INSTANCE.getLowCardinalityKeyValues(context);
        LOGGER.debug("Before ApiGatewayObservationConvention:  {}", keyValues);
        Route route = context.getServerWebExchange().getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        keyValues = keyValues
                .and("requestUrl", context.getServerWebExchange().getRequest().getPath().value());
        if (route != null && !CollectionUtils.isEmpty(route.getMetadata())
                && route.getMetadata().containsKey(GatewayConstants.SERVICE_NAME)) {
            keyValues = keyValues.and(GatewayConstants.SERVICE,
                    (String) route.getMetadata().get(GatewayConstants.SERVICE_NAME));
        } else {
            keyValues = keyValues.and(GatewayConstants.SERVICE, GatewayConstants.UNKNOWN);
        }
        LOGGER.debug("After ApiGatewayObservationConvention:  {}", keyValues);
        return keyValues;
    }

    @NotNull
    @Override
    public String getName() {
        return this.metricsName;
    }
}
