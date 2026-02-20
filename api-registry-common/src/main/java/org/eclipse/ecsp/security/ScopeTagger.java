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

package org.eclipse.ecsp.security;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Operation;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ScopeTagger to load the scopes dynamically.
 */
@Component
@ConfigurationProperties(prefix = "scopes")
@Getter
@Setter
public class ScopeTagger implements OperationCustomizer {
    /**
     * Default constructor.
     */
    public ScopeTagger() {
        // Default constructor
    }


    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ScopeTagger.class);
    /**
     * Property holds scopesMap.
     * {@link Map} of routeId and list of scopes.
     */
    private Map<String, List<String>> scopesMap;

    /**
     * Property holds overrideScopeEnabled.
     */
    @Value("${scopes.override.enabled:false}")
    private boolean isOverrideScopeEnabled;

    @Override
    public Operation customize(final Operation operation, HandlerMethod handlerMethod) {
        operation.setOperationId(operation.getOperationId().replace("_", "-"));
        String routeId = operation.getTags().get(0) + "-" + operation.getOperationId();
        LOGGER.info("Route id: " + routeId);
        if (operation.getSummary() == null) {
            operation.setSummary(operation.getOperationId() + " api call");
        }
        if (operation.getDescription() == null) {
            operation.setDescription(" ");
        }

        //API-RouteId Display
        operation.description(operation.getDescription() + "<br>" + routeId);

        // Load from configuration
        SecurityRequirement annotation = handlerMethod.getMethodAnnotation(SecurityRequirement.class);
        if (annotation == null) {
            operation.setDescription(operation
                    .getDescription() + "<p style='color:red;'> WARNING!! - Security Config Not Found </p>");
            return operation;
        }

        if (annotation.scopes() != null && annotation.scopes().length >= 1) {
            // extract scopes from annotation
            operation.description(operation
                    .getDescription()
                    + "<p style='color:red;'>SCOPE: "
                    + Arrays.toString(annotation.scopes())
                    + "</p>");
            // override scope config
            if (isOverrideScopeEnabled && scopesMap != null
                    && (scopesMap.get(routeId) != null || scopesMap.get(routeId.toLowerCase()) != null)) {
                List<String> scopesList =
                        scopesMap.get(routeId) != null ? scopesMap.get(routeId) : scopesMap.get(routeId.toLowerCase());
                LOGGER.debug("Override Scopes Map Config: " + scopesMap);
                operation.description(operation
                        .getDescription() + "<p style='color:blue;'>OVERRIDE_SCOPE: " + scopesList + "</p>");
            }
        } else {
            operation.description(operation.getDescription() + "<p style='color:red;'>SCOPE: EMPTY </p>");
        }
        return operation;
    }

}
