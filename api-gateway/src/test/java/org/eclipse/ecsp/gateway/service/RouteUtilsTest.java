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

package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for RouteUtils.
 */
@SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
class RouteUtilsTest {

    private RouteUtils routeUtils;

    @BeforeEach
    void setUp() {
        routeUtils = new RouteUtils();
        ReflectionTestUtils.setField(routeUtils, "apiGatewayUri", "http://localhost:8080");
    }

    @Test
    void getRoutePath_WithPathPredicate_ReturnsPath() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName(GatewayConstants.PATH);
        pathPredicate.getArgs().put("pattern", "/api/test/**");
        predicates.add(pathPredicate);

        // When
        String result = RouteUtils.getRoutePath(predicates);

        // Then
        assertEquals("/api/test/**", result);
    }

    @Test
    void getRoutePath_WithoutPathPredicate_ReturnsNull() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition methodPredicate = new PredicateDefinition();
        methodPredicate.setName(GatewayConstants.METHOD);
        methodPredicate.getArgs().put("methods", "GET");
        predicates.add(methodPredicate);

        // When
        String result = RouteUtils.getRoutePath(predicates);

        // Then
        assertNull(result);
    }

    @Test
    void getRoutePath_WithEmptyPredicates_ReturnsNull() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();

        // When
        String result = RouteUtils.getRoutePath(predicates);

        // Then
        assertNull(result);
    }

    @Test
    void getRoutePath_WithMultiplePredicates_ReturnsPathFromPathPredicate() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();

        PredicateDefinition methodPredicate = new PredicateDefinition();
        methodPredicate.setName(GatewayConstants.METHOD);
        methodPredicate.getArgs().put("methods", "POST");
        predicates.add(methodPredicate);

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName(GatewayConstants.PATH);
        pathPredicate.getArgs().put("pattern", "/api/users/**");
        predicates.add(pathPredicate);

        // When
        String result = RouteUtils.getRoutePath(predicates);

        // Then
        assertEquals("/api/users/**", result);
    }

    @Test
    void getRouteMethod_WithMethodPredicate_ReturnsMethod() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition methodPredicate = new PredicateDefinition();
        methodPredicate.setName(GatewayConstants.METHOD);
        methodPredicate.getArgs().put("methods", "POST");
        predicates.add(methodPredicate);

        // When
        String result = RouteUtils.getRouteMethod(predicates);

        // Then
        assertEquals("POST", result);
    }

    @Test
    void getRouteMethod_WithoutMethodPredicate_ReturnsNull() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName(GatewayConstants.PATH);
        pathPredicate.getArgs().put("pattern", "/api/test/**");
        predicates.add(pathPredicate);

        // When
        String result = RouteUtils.getRouteMethod(predicates);

        // Then
        assertNull(result);
    }

    @Test
    void getRouteMethod_WithEmptyPredicates_ReturnsNull() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();

        // When
        String result = RouteUtils.getRouteMethod(predicates);

        // Then
        assertNull(result);
    }

    @Test
    void getRouteMethod_WithMultiplePredicates_ReturnsMethodFromMethodPredicate() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName(GatewayConstants.PATH);
        pathPredicate.getArgs().put("pattern", "/api/test/**");
        predicates.add(pathPredicate);

        PredicateDefinition methodPredicate = new PredicateDefinition();
        methodPredicate.setName(GatewayConstants.METHOD);
        methodPredicate.getArgs().put("methods", "DELETE");
        predicates.add(methodPredicate);

        // When
        String result = RouteUtils.getRouteMethod(predicates);

        // Then
        assertEquals("DELETE", result);
    }

    @Test
    void getDummyRoute_CreatesValidDummyRoute() {
        // When
        IgniteRouteDefinition result = routeUtils.getDummyRoute();

        // Then
        assertNotNull(result);
        assertEquals("DUMMY", result.getId());
        assertEquals("http://localhost:8080", result.getUri().toString());
        assertEquals(1, result.getPredicates().size());

        PredicateDefinition pathPredicate = result.getPredicates().get(0);
        assertEquals(GatewayConstants.PATH, pathPredicate.getName());
        assertEquals("/**", pathPredicate.getArgs().get(GatewayConstants.KEY_0));
    }

    @Test
    void getDummyRoute_WithInvalidUri_HandlesException() {
        // Given
        ReflectionTestUtils.setField(routeUtils, "apiGatewayUri", "invalid://uri with spaces");

        // When
        IgniteRouteDefinition result = routeUtils.getDummyRoute();

        // Then
        assertNotNull(result);
        assertEquals("DUMMY", result.getId());
        // URI should be null due to exception
        assertNull(result.getUri());
    }

    @Test
    void getRoutePath_WithCaseInsensitivePathName_ReturnsPath() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("path"); // lowercase
        pathPredicate.getArgs().put("pattern", "/api/lower/**");
        predicates.add(pathPredicate);

        // When
        String result = RouteUtils.getRoutePath(predicates);

        // Then
        assertEquals("/api/lower/**", result);
    }

    @Test
    void getRouteMethod_WithCaseInsensitiveMethodName_ReturnsMethod() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition methodPredicate = new PredicateDefinition();
        methodPredicate.setName("method"); // lowercase
        methodPredicate.getArgs().put("methods", "PUT");
        predicates.add(methodPredicate);

        // When
        String result = RouteUtils.getRouteMethod(predicates);

        // Then
        assertEquals("PUT", result);
    }

    @Test
    void getRoutePath_WithMultipleArgsInPredicate_ReturnsFirstValue() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName(GatewayConstants.PATH);
        pathPredicate.getArgs().put("pattern", "/api/first/**");
        pathPredicate.getArgs().put("other", "/api/other/**");
        predicates.add(pathPredicate);

        // When
        String result = RouteUtils.getRoutePath(predicates);

        // Then
        assertNotNull(result);
        // Should return one of the values
        assertTrue(result.equals("/api/first/**") || result.equals("/api/other/**"));
    }

    @Test
    void getRouteMethod_WithMultipleArgsInPredicate_ReturnsFirstValue() {
        // Given
        List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition methodPredicate = new PredicateDefinition();
        methodPredicate.setName(GatewayConstants.METHOD);
        methodPredicate.getArgs().put("methods", "GET");
        methodPredicate.getArgs().put("other", "POST");
        predicates.add(methodPredicate);

        // When
        String result = RouteUtils.getRouteMethod(predicates);

        // Then
        assertNotNull(result);
        // Should return one of the values
        assertTrue(result.equals("GET") || result.equals("POST"));
    }

    private void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}
