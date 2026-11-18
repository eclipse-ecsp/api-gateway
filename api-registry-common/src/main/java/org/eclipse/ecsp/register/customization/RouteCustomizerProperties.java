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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.register.customization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Route customizer configuration: gateway filters attached to auto-derived routes from configuration only.
 *
 * <p>The list is bound from {@code api.registry.route-customizer}. Each {@link RouteMapping} selects routes by
 * route id, or by path and an optional method, and lists the filters appended to the routes it selects. An absent
 * or empty list customizes nothing, so the feature needs no toggle of its own.
 *
 * <p>Like {@code ApiRoutesConfig}, it exists only when {@code api.registry.enabled} is {@code true}.
 */
@Configuration
@ConfigurationProperties(prefix = RegistryCommonConstants.API_REGISTRY_PREFIX)
@ConditionalOnProperty(value = "api.registry.enabled", havingValue = "true", matchIfMissing = false)
@Validated
@Getter
@Setter
@NoArgsConstructor
public class RouteCustomizerProperties {

    /**
     * Mappings bound from {@code api.registry.route-customizer}.
     */
    @NotNull
    @Valid
    private List<RouteMapping> routeCustomizer = new ArrayList<>();

    /**
     * One mapping: a route selector (route id, or path and optional method) and the filters to attach.
     */
    @ValidRouteMapping
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RouteMapping {

        /**
         * Preferred selector: the route id used by api-registry and api-gateway, {@code <first tag>-<operationId>}.
         */
        private String routeId;

        /**
         * Fallback selector when {@link #routeId} is absent: the exact OpenAPI path of the operation.
         */
        private String path;

        /**
         * Optional HTTP method that narrows a {@link #path} selector; meaningful only together with it.
         */
        @Pattern(regexp = "(?i)GET|POST|PUT|DELETE|PATCH|OPTIONS", message = "must be a supported HTTP method")
        private String method;

        /**
         * Filters appended to every route this mapping selects.
         */
        @NotNull
        @Valid
        private List<FilterOverride> filters = new ArrayList<>();

        /**
         * Tells whether this mapping selects the given route.
         *
         * <p>A mapping with a route id selects the route with exactly that id. Otherwise it selects the operations
         * at its path, narrowed to its method when one is set (case-insensitive).
         *
         * @param candidateRouteId the id of the route being registered
         * @param candidatePath    the OpenAPI path of the operation
         * @param candidateMethod  the HTTP method of the operation
         * @return {@code true} if this mapping applies to the route
         */
        public boolean matches(String candidateRouteId, String candidatePath, String candidateMethod) {
            if (StringUtils.hasText(routeId)) {
                return routeId.equals(candidateRouteId);
            }
            return StringUtils.hasText(path) && path.equals(candidatePath)
                    && (!StringUtils.hasText(method) || method.equalsIgnoreCase(candidateMethod));
        }
    }

    /**
     * One configured filter: a name and its arguments, the same shape as a route's filter definition.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class FilterOverride {

        @NotBlank
        private String name;

        @NotNull
        private Map<String, String> args = new LinkedHashMap<>();
    }
}
