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

package org.eclipse.ecsp.register.customization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the opt-in route-path-exposure / filter-customization overlay.
 *
 * <p>Bound from {@code api.registry.route-customizer.*} and gated behind
 * {@code api.registry.route-customizer.enabled} so a microservice that does not configure this
 * feature sees route derivation unchanged from before this feature existed.
 */
@Configuration
@ConfigurationProperties(RouteCustomizerProperties.PREFIX)
@ConditionalOnProperty(value = "api.registry.route-customizer.enabled", havingValue = "true", matchIfMissing = false)
@Validated
@Getter
@Setter
@NoArgsConstructor
public class RouteCustomizerProperties {

    /** Prefix for the configuration properties. */
    public static final String PREFIX = "api.registry.route-customizer";

    @NotNull
    @Valid
    private List<RouteMapping> mappings = new ArrayList<>();

    /** One override entry: match key + {@code exposeAs} target path(s) + {@code replaceOriginal} + filters. */
    @ValidRouteMapping
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RouteMapping {

        /**
         * Preferred match key: the route id api-registry stores and api-gateway uses
         * ({@code tag-operationId}), computed via {@link RouteIdUtils#computeRouteId}.
         */
        private String routeId;

        /** Fallback match key when {@link #routeId} is not given: the original OpenAPI path. */
        private String path;

        /** Optional HTTP method; only meaningful together with {@link #path}. */
        @Pattern(regexp = "(?i)GET|POST|PUT|DELETE|PATCH|OPTIONS", message = "must be a supported HTTP method")
        private String method;

        /** One or more target paths; may contain {@code ${property}} placeholders. */
        @NotNull
        private List<String> exposeAs = new ArrayList<>();

        /** {@code true} removes the original path/operation after fan-out; {@code false} (default) keeps both. */
        private boolean replaceOriginal = false;

        /** Additional filters merged into every route produced from this mapping. */
        @NotNull
        @Valid
        private List<FilterOverride> filters = new ArrayList<>();
    }

    /** One configured filter: {@code name} + {@code args} map, the same shape as {@code FilterDefinition}. */
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
