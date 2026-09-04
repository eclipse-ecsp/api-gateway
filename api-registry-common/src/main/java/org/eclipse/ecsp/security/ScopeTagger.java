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
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ScopeTagger to load the scopes dynamically.
 */
@Component
public class ScopeTagger implements OperationCustomizer {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ScopeTagger.class);

    private final ScopeOverrideProperties scopeOverrideProperties;

    /**
     * Constructor for ScopeTagger.
     *
     * @param scopeOverrideProperties the scope-override configuration properties
     */
    public ScopeTagger(ScopeOverrideProperties scopeOverrideProperties) {
        this.scopeOverrideProperties = scopeOverrideProperties;
        // log the override configuration

        LOGGER.info("Scope override is enabled? : {}, config: {}",
             scopeOverrideProperties.getOverride().isEnabled(), 
             scopeOverrideProperties.getScopesMap());
    }

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
            Map<String, List<String>> scopesMap = scopeOverrideProperties.getScopesMap();
            List<String> overrideScope = getOverrideScopesForRoutes(scopesMap, routeId);
            if (scopeOverrideProperties.getOverride().isEnabled() && !overrideScope.isEmpty()) {
                LOGGER.info("Overriding default scopes for routeId: {} with scopes : {}", routeId, overrideScope);
                updateNewScopes(operation, routeId, overrideScope);
            }
        } else {
            operation.description(operation.getDescription() + "<p style='color:red;'>SCOPE: EMPTY </p>");
        }
        return operation;
    }

    private void updateNewScopes(final Operation operation, String routeId, List<String> scopes) {
        LOGGER.debug("Override Scopes Map Config: {}, for routeId: {}" + scopes, routeId);
        // Replace the scopes in the OpenAPI operation model so that ApiRoutesLoader
        // picks up the overridden scopes when it reads operation.getSecurity().
        // Java annotations are immutable; the mutable OpenAPI model must be updated instead.
        if (operation.getSecurity() != null) {
            operation.getSecurity().forEach(sr ->
                    sr.replaceAll((name, existingScopes) -> scopes));
        }
        operation.description(operation
                .getDescription() + "<p style='color:blue;'>OVERRIDE_SCOPE: " + scopes + "</p>");
    }

    /**
     * get the override scope for the route.
     *
     * @param scopesMap override scope config map
     * @param routeId the api routeId
     * @return list of override scopes, empty if not found
     */
    private List<String> getOverrideScopesForRoutes(Map<String, List<String>> scopesMap, String routeId) {
        // normalize the route by removing whitespace is the routeid
        // checks if scope is available with the route Id scopesMap.get(routeId)
        // else if checks the scope with routeId.toLowerCase
        if (scopesMap == null || scopesMap.isEmpty()) {
            return List.of();
        }
        String normalizedRouteId = routeId.replace(" ", "");
        if (scopesMap.containsKey(normalizedRouteId)) {
            return scopesMap.get(normalizedRouteId);
        } 
        String lowerCaseRouteId = normalizedRouteId.toLowerCase();
        if (scopesMap.containsKey(lowerCaseRouteId)) {
            return scopesMap.get(lowerCaseRouteId);
        }
        return List.of();
    }

}
