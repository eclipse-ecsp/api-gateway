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

package org.eclipse.ecsp.register;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is the load the api-routes from configuration.
 *
 * @author SBala2
 */
@Configuration
@ConfigurationProperties(ApiRoutesConfig.PREFIX)
@ConditionalOnProperty(value = "api.registry.enabled", havingValue = "true", matchIfMissing = false)
@Validated
@Setter
@Getter
public class ApiRoutesConfig {
    /**
     * Prefix for the configuration properties.
     */
    public static final String PREFIX = "api.gateway";
    @NotNull
    @Valid
    private List<RouteDefinition> routes = new ArrayList<>();

}