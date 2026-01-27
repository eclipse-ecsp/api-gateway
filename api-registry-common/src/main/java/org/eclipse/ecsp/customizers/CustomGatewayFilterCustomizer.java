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

package org.eclipse.ecsp.customizers;

import io.swagger.v3.oas.models.Operation;
import org.eclipse.ecsp.annotations.CustomGatewayFilter;
import org.eclipse.ecsp.annotations.CustomGatewayFilters;
import org.eclipse.ecsp.utils.NumericConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CustomGatewayFilterCustomizer is a Spring component that customizes OpenAPI operations.
 * by adding request filters defined in the @CustomGatewayFilter and @CustomGatewayFilters annotations.
 */
@Component
public class CustomGatewayFilterCustomizer implements OperationCustomizer {
    /**
     * The extension key used to store custom gateway filters in the OpenAPI operation.
     */
    public static final String FILTERS_EXTENSION = "x-filter";

    /**
     * The equals sign used to separate filter arguments in the annotation.
     */
    public static final String EQUALS_SIGN = "=";
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(CustomGatewayFilterCustomizer.class);

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        List<CustomGatewayFilter> requestFilterList = new ArrayList<>();
        // Collect CustomGatewayFilters from @CustomGatewayFilters annotation
        if (handlerMethod.hasMethodAnnotation(CustomGatewayFilters.class)) {
            for (CustomGatewayFilters reqFilter : handlerMethod.getMethod()
                    .getAnnotationsByType(CustomGatewayFilters.class)) {
                CustomGatewayFilter[] filtersArr =
                        reqFilter.filters().length > 0 ? reqFilter.filters() : reqFilter.value();
                if (filtersArr != null) {
                    LOGGER.debug("Found CustomGatewayFilters annotation: {}", Arrays.toString(filtersArr));
                    requestFilterList.addAll(Arrays.asList(filtersArr));
                }
            }
        }
        // Collect CustomGatewayFilter from @CustomGatewayFilter annotation
        if (handlerMethod.hasMethodAnnotation(CustomGatewayFilter.class)) {
            CustomGatewayFilter[] reqFilters = handlerMethod.getMethod()
                    .getAnnotationsByType(CustomGatewayFilter.class);
            LOGGER.debug("Found CustomGatewayFilter annotations: {}", Arrays.toString(reqFilters));
            requestFilterList.addAll(Arrays.asList(reqFilters));
        }

        if (requestFilterList.isEmpty()) {
            LOGGER.info("No CustomGatewayFilter(s) annotations found for operation: {}", operation.getOperationId());
            return operation;
        }

        List<String> filters = new ArrayList<>();

        // Process each CustomGatewayFilter and add them to the operation extensions
        for (CustomGatewayFilter filter : requestFilterList) {
            processFilterAnnotation(operation, filter, filters);
        }
        String descriptionBuilder = operation.getDescription() + "<br><b>Custom Gateway Filters:</b> "
                + filters.stream()
                .map(filter -> "<span style='color:blue;'>" + filter + "</span>")
                .collect(Collectors.joining(", "));
        operation.setDescription(descriptionBuilder);

        LOGGER.info("CustomGatewayFilters applied for operationId: {}, filters: {}",
                operation.getOperationId(),
                filters.toArray());
        return operation;
    }

    private void processFilterAnnotation(Operation operation, CustomGatewayFilter filter, List<String> filters) {
        if (filter == null) {
            return;
        }
        String filterName = filter.name().isEmpty() ? filter.value() : filter.name();
        if (filterName.isEmpty()) {
            LOGGER.error("Filter name is empty for filter: {}", filter);
            throw new IllegalArgumentException("Filter name cannot be empty, "
                    + "please configure the filter with a valid name");
        }
        filters.add(filterName);
        Map<String, String> args = extractFilterArguments(filter);
        operation.addExtension(FILTERS_EXTENSION + "-" + filterName, args);
    }

    private Map<String, String> extractFilterArguments(CustomGatewayFilter filter) {
        Map<String, String> args = new HashMap<>();
        String filterName = filter.name().isEmpty() ? filter.value() : filter.name();
        for (var singleArg : filter.args()) {
            String[] parts = singleArg.split(EQUALS_SIGN, NumericConstants.TWO);
            if (parts.length == NumericConstants.TWO) {
                args.put(parts[0].trim(), parts[1].trim());
                LOGGER.debug("Added filter argument: {}={} for filter: {}",
                        parts[0].trim(), parts[1].trim(), filterName);
            } else {
                LOGGER.error("Invalid filter argument format: {}", singleArg);
                throw new IllegalArgumentException("Invalid filter argument: "
                        + singleArg + ", please configure the args as key=value");
            }
        }
        return args;
    }
}