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

import org.eclipse.ecsp.register.model.FilterDefinition;
import org.eclipse.ecsp.register.model.PredicateDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteSetValidatorTest {

    private RouteDefinition route(String id, String method, String path) {
        RouteDefinition route = new RouteDefinition();
        route.setId(id);
        route.setUri(URI.create("http://svc:8080/"));
        List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition methodPredicate = new PredicateDefinition();
        methodPredicate.setName(RegistryCommonConstants.METHOD);
        methodPredicate.getArgs().put(RegistryCommonConstants.KEY_0, method);
        predicates.add(methodPredicate);
        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName(RegistryCommonConstants.PATH);
        pathPredicate.getArgs().put(RegistryCommonConstants.KEY_0, path);
        predicates.add(pathPredicate);
        route.setPredicates(predicates);
        route.setFilters(new ArrayList<FilterDefinition>());
        return route;
    }

    @Test
    void validatePassesForDistinctRoutes() {
        List<RouteDefinition> routes = List.of(
                route("Demo-doThis", "GET", "/v1/api/dothis"),
                route("Demo-doThis-alias-abc123456789", "GET", "/api/dothis"));
        assertDoesNotThrow(() -> RouteSetValidator.validate(routes));
    }

    @Test
    void validateAllowsSamePathDifferentMethod() {
        List<RouteDefinition> routes = List.of(
                route("Demo-get", "GET", "/api/dothis"),
                route("Demo-post", "POST", "/api/dothis"));
        assertDoesNotThrow(() -> RouteSetValidator.validate(routes));
    }

    @Test
    void validateRejectsDuplicateRouteIds() {
        List<RouteDefinition> routes = List.of(
                route("Demo-doThis", "GET", "/v1/api/dothis"),
                route("Demo-doThis", "GET", "/api/dothis"));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> RouteSetValidator.validate(routes));
        assertTrue(ex.getMessage().contains("duplicate route id"));
    }

    @Test
    void validateRejectsDuplicateMethodPathAcrossDifferentIds() {
        List<RouteDefinition> routes = List.of(
                route("Demo-doThis", "GET", "/api/dothis"),
                route("Static-route", "GET", "/api/dothis"));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> RouteSetValidator.validate(routes));
        assertTrue(ex.getMessage().contains("duplicate method/path"));
    }
}
