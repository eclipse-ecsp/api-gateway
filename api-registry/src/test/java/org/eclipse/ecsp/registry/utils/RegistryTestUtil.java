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

package org.eclipse.ecsp.registry.utils;

import org.eclipse.ecsp.register.model.FilterDefinition;
import org.eclipse.ecsp.register.model.PredicateDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * test class RegistryUtils.
 */
@SuppressWarnings({"PMD", "checkstyle:hideutilityclassconstructor"})
public class RegistryTestUtil {

    /**
     * getApiRouteEntity returns ApiRouteEntity.
     *
     * @return ApiRouteEntity
     */
    public static ApiRouteEntity getApiRouteEntity() {
        ApiRouteEntity apiRouteEntity = new ApiRouteEntity();
        apiRouteEntity.setId("VehicleProfileController-get");
        apiRouteEntity.setRoute(getRouteDefination());
        apiRouteEntity.setService("vehicle-profile");
        apiRouteEntity.setActive(Boolean.TRUE);
        apiRouteEntity.setContextPath("/test-contextPath");
        return apiRouteEntity;
    }

    /**
     * getRouteDefination returns RouteDefinition.
     *
     * @return RouteDefinition
     */
    public static RouteDefinition getRouteDefination() {
        RouteDefinition routeDefinition = new RouteDefinition();

        List<PredicateDefinition> predicateDefinitions = new ArrayList<>();
        predicateDefinitions.add(getPredicateDefinition());

        List<FilterDefinition> filterDefinitions = new ArrayList<>();
        filterDefinitions.add(getFilterDefinition());

        routeDefinition.setId("TestController-get");
        routeDefinition.setPredicates(predicateDefinitions);
        routeDefinition.setFilters(filterDefinitions);
        routeDefinition.setUri(URI.create("http://test-int-svc:8080/"));
        routeDefinition.setMetadata(new HashMap<>());
        routeDefinition.setService("test");
        routeDefinition.setApiDocs(Boolean.TRUE);
        routeDefinition.setOrder(0);
        return routeDefinition;
    }

    /**
     * getPredicateDefinition returns PredicateDefinition.
     *
     * @return PredicateDefinition
     */
    public static PredicateDefinition getPredicateDefinition() {
        PredicateDefinition predicateDefinition = new PredicateDefinition();

        Map<String, String> args = new HashMap<>();
        args.put("_genkey_0", "/v1.0/vehicleProfiles/**");

        predicateDefinition.setName("Path");
        return predicateDefinition;
    }

    /**
     * getFilterDefinition returns FilterDefinition.
     *
     * @return FilterDefinition
     */
    public static FilterDefinition getFilterDefinition() {
        FilterDefinition filterDefinition = new FilterDefinition();

        Map<String, String> args = new HashMap<>();
        args.put("scope", "SelfManage");

        filterDefinition.setName("JwtAuthValidator");
        filterDefinition.setArgs(args);
        return filterDefinition;
    }
}
