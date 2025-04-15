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

package org.eclipse.ecsp.gateway.plugins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.gateway.plugins.filters.RequestBodyFilter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * RequestBodyValidator Filter Validates the Request body.
 */
@Component
public class RequestBodyValidator extends AbstractGatewayFilterFactory<RequestBodyValidator.Config> {
    /**
     * Constructor to initialize RequestBodyValidator.
     */
    public RequestBodyValidator() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new RequestBodyFilter(config);
    }

    /**
     * Confing class to pass configurations to filter.
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class Config {
    }
}

