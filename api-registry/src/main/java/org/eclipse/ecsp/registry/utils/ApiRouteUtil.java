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

import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Utility class for ApiRouteUtil.
 */
public class ApiRouteUtil {
    /**
     * Private constructor to prevent instantiation.
     */
    private ApiRouteUtil() {
    }

    /**
     * Returns the List of RouteDefinition.
     *
     * @param entities List of ApiRouteEntity
     * @return returns List of RouteDefinition
     */
    public static List<RouteDefinition> convertActive(Iterable<ApiRouteEntity> entities) {
        TreeMap<String, RouteDefinition> routes = new TreeMap<>();
        if (entities != null) {
            entities.forEach(entity -> {
                if (Boolean.TRUE.equals(entity.getActive())) {
                    routes.put(entity.getId(), convert(entity));
                }
            });
        }
        return new ArrayList<>(routes.values());
    }

    /**
     * Converts the RouteDefinition to ApiRouteEntity.
     *
     * @param model define the RouteDefinition
     * @return returns ApiRouteEntity entity
     */
    public static ApiRouteEntity convert(RouteDefinition model) {
        return convert(model, new ApiRouteEntity());
    }

    /**
     * Convert the RouteDefinition tp ApiRouteEntity.
     *
     * @param model  define the RouteDefinition
     * @param entity defines the ApiRouteEntity
     * @return returns ApiRouteEntity entity
     */
    public static ApiRouteEntity convert(RouteDefinition model, ApiRouteEntity entity) {
        entity.setId(model.getId());
        entity.setRoute(model);
        entity.setService(model.getService());
        entity.setApiDocs(model.getApiDocs());
        entity.setActive(Boolean.TRUE);
        return entity;
    }

    /**
     * Converts the List of ApiRouteEntity to List of RouteDefinition.
     *
     * @param entities takes list of ApiRouteEntity
     * @return returns List of RouteDefinition
     */
    public static List<RouteDefinition> convert(Iterable<ApiRouteEntity> entities) {
        List<RouteDefinition> list = new ArrayList<>();
        if (entities != null) {
            entities.forEach(entity -> list.add(convert(entity)));
        }
        return list;
    }

    /**
     * Converts the ApiRouteEntity to RouteDefinition.
     *
     * @param entity takes ApiRouteEntity
     * @return returns RouteDefinition
     */
    public static RouteDefinition convert(ApiRouteEntity entity) {
        return entity.getRoute();
    }

}
