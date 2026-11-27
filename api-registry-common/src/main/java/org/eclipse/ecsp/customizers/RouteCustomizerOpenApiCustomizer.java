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

package org.eclipse.ecsp.customizers;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.eclipse.ecsp.config.RouteCustomizerProperties;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enriches OpenAPI {@link Operation} definitions with filters configured via {@link RouteCustomizerProperties},
 * using the same {@code x-filter-<name>} extension and description markup as {@link CustomGatewayFilterCustomizer}.
 */
@Component
@ConditionalOnProperty(value = RegistryCommonConstants.API_REGISTRY_ROUTE_CUSTOMIZER_ENABLED,
        havingValue = "true", matchIfMissing = false)
public class RouteCustomizerOpenApiCustomizer implements GlobalOpenApiCustomizer {

    /**
     * HTML prefix used in OpenAPI operation descriptions to display custom gateway filters.
     */
    public static final String CUSTOM_GATEWAY_FILTERS_LABEL = "<br><b>Custom Gateway Filters:</b> ";

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteCustomizerOpenApiCustomizer.class);

    private final RouteCustomizerProperties properties;

    /**
     * Creates a new OpenAPI customizer backed by the route customizer properties.
     *
     * @param properties the route customizer configuration properties
     */
    public RouteCustomizerOpenApiCustomizer(RouteCustomizerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi == null || !properties.isEnabled() || properties.getMappings().isEmpty()) {
            return;
        }
        Paths paths = openApi.getPaths();
        if (paths == null || paths.isEmpty()) {
            return;
        }
        paths.forEach(this::customizePathItem);
    }

    private void customizePathItem(String path, PathItem pathItem) {
        if (pathItem == null) {
            return;
        }
        customizeOperation(path, HttpMethod.POST, pathItem.getPost());
        customizeOperation(path, HttpMethod.GET, pathItem.getGet());
        customizeOperation(path, HttpMethod.PUT, pathItem.getPut());
        customizeOperation(path, HttpMethod.DELETE, pathItem.getDelete());
        customizeOperation(path, HttpMethod.PATCH, pathItem.getPatch());
        customizeOperation(path, HttpMethod.OPTIONS, pathItem.getOptions());
    }

    private void customizeOperation(String path, HttpMethod method, Operation operation) {
        if (operation == null || alreadyCustomized(operation)) {
            return;
        }
        String routeId = resolveRouteId(operation);
        List<RouteCustomizerProperties.FilterOverride> matchedFilters = new ArrayList<>();
        for (RouteCustomizerProperties.RouteMapping mapping : properties.getMappings()) {
            if (mapping.matches(routeId, path, method.name())) {
                matchedFilters.addAll(mapping.getFilters());
            }
        }
        if (matchedFilters.isEmpty()) {
            return;
        }
        List<String> filterNames = new ArrayList<>(matchedFilters.size());
        for (RouteCustomizerProperties.FilterOverride filter : matchedFilters) {
            filterNames.add(filter.getName());
            addFilterExtension(operation, filter);
        }
        appendFiltersToDescription(operation, filterNames);
        LOGGER.info("Configured CustomGatewayFilters applied for operationId: {}, filters: {}",
                operation.getOperationId(), filterNames);
    }

    static String resolveRouteId(Operation operation) {
        if (operation.getTags() == null || operation.getTags().isEmpty()
                || !StringUtils.hasText(operation.getOperationId())) {
            return null;
        }
        return operation.getTags().get(0) + "-" + operation.getOperationId().replace("_", "-");
    }

    private static boolean alreadyCustomized(Operation operation) {
        Map<String, Object> extensions = operation.getExtensions();
        return extensions != null && extensions.values().stream().anyMatch(ConfiguredFilterArgs.class::isInstance);
    }

    private static void addFilterExtension(Operation operation,
                                           RouteCustomizerProperties.FilterOverride filter) {
        String extensionKey = CustomGatewayFilterCustomizer.FILTERS_EXTENSION + "-" + filter.getName();
        Map<String, Object> extensions = operation.getExtensions();
        if (extensions != null && extensions.containsKey(extensionKey)
                && !(extensions.get(extensionKey) instanceof ConfiguredFilterArgs)) {
            return;
        }
        operation.addExtension(extensionKey, new ConfiguredFilterArgs(filter.getArgs()));
    }

    private static void appendFiltersToDescription(Operation operation, List<String> filterNames) {
        String formatted = filterNames.stream()
                .map(filter -> "<span style='color:blue;'>" + filter + "</span>")
                .collect(Collectors.joining(", "));
        String existing = operation.getDescription() == null ? "" : operation.getDescription();
        if (existing.contains(CUSTOM_GATEWAY_FILTERS_LABEL)) {
            operation.setDescription(existing + ", " + formatted);
        } else {
            operation.setDescription(existing + CUSTOM_GATEWAY_FILTERS_LABEL + formatted);
        }
    }

    /**
     * Marker map for {@code x-filter-*} extensions populated from {@link RouteCustomizerProperties}, serialized as a
     * standard JSON object in OpenAPI specs while allowing {@code ApiRoutesLoader} to avoid double-adding filters.
     */
    public static final class ConfiguredFilterArgs extends LinkedHashMap<String, String> {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Creates an extension map copied from the configured filter arguments.
         *
         * @param source the configured filter arguments
         */
        public ConfiguredFilterArgs(Map<String, String> source) {
            super(source);
        }
    }
}
