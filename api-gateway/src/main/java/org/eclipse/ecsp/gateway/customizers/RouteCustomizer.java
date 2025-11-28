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

package org.eclipse.ecsp.gateway.customizers;

import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;

/**
 * Contract for customising route definitions with additional gateway behaviour.
 */
public interface RouteCustomizer {
    /**
     * Applies the customisation to the provided route definition.
     *
     * @param routeDefinition the Spring Cloud Gateway route definition to mutate
     * @param igniteRouteDefinition the Ignite route metadata used to drive customisation
     * @return the updated route definition instance
     */
    RouteDefinition customize(RouteDefinition routeDefinition, IgniteRouteDefinition igniteRouteDefinition);
}
