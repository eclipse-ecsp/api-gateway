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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.utils.RegistryCommonTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springdoc.core.customizers.SpringDocCustomizers;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.SpringDocProviders;
import org.springdoc.core.service.AbstractRequestService;
import org.springdoc.core.service.GenericResponseService;
import org.springdoc.core.service.OpenAPIService;
import org.springdoc.core.service.OperationService;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Test class for ApiRoutesLoader.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ApiRoutesConfig.class)
class ApiRoutesLoaderTest {

    GroupedOpenApi groupedOpenApi = Mockito.mock(GroupedOpenApi.class);
    ObjectFactory<?> objectFactory = Mockito.mock(ObjectFactory.class);
    OpenAPIService openApiService = Mockito.mock(OpenAPIService.class);
    AbstractRequestService abstractRequestService = Mockito.mock(AbstractRequestService.class);
    GenericResponseService genericResponseService = Mockito.mock(GenericResponseService.class);
    OperationService operationService = Mockito.mock(OperationService.class);
    SpringDocConfigProperties springDocConfigProperties = Mockito.mock(SpringDocConfigProperties.class);
    SpringDocProviders springDocProviders = Mockito.mock(SpringDocProviders.class);
    SpringDocCustomizers springDocCustomizers = Mockito.mock(SpringDocCustomizers.class);

    ObjectFactory<OpenAPIService> openApiServiceObjectFactory = () -> openApiService;

    @InjectMocks
    private ApiRoutesLoader apiRoutesLoader;

    /**
     * getSchemaObj.
     *
     * @return returns Schema
     */
    private static Schema<?> getSchemaObj() {
        Schema<?> schema = new Schema<>();
        schema.set$ref("#/components/schemas/Test");
        return schema;
    }

    /**
     * before methos get executed before each test case.
     */
    @BeforeEach
    void before() {
        SpringDocConfigProperties.ApiDocs apiDocs = new SpringDocConfigProperties.ApiDocs();
        apiDocs.setVersion(SpringDocConfigProperties.ApiDocs.OpenApiVersion.OPENAPI_3_0);
        Mockito.when(springDocConfigProperties.getApiDocs()).thenReturn(apiDocs);
        OpenAPI openApi = getOpenApiObj();
        Mockito.when(openApiService.build(Locale.getDefault())).thenReturn(openApi);
        Mockito.when(openApiService.getContext()).thenReturn(Mockito.mock(ApplicationContext.class));
        Mockito.when(springDocProviders.jsonMapper()).thenReturn(new ObjectMapper());
        ApiRoutesConfig apiRouteConfig = new ApiRoutesConfig();
        apiRouteConfig.setRoutes(List.of());
        apiRoutesLoader = new ApiRoutesLoader(List.of(groupedOpenApi), openApiServiceObjectFactory,
                abstractRequestService, genericResponseService, operationService, springDocConfigProperties,
                springDocProviders, springDocCustomizers, apiRouteConfig);
    }

    @Test
    void testGetRoutes() throws Exception {
        ReflectionTestUtils.setField(apiRoutesLoader, "contextPath", "test-api");
        ApiRoutesConfig apiRoutesConfig = new ApiRoutesConfig();
        apiRoutesConfig.setRoutes(List.of(RegistryCommonTestUtil.getRouteDefination()));
        ReflectionTestUtils.setField(apiRoutesLoader, "apiRoutesConfig", apiRoutesConfig);
        Map<String, List<String>> scopeMap = new HashMap<>();
        apiRoutesLoader.getApiRoutes();
        scopeMap.put("test-controller-create", List.of("SelfManage", "IgniteSystem"));
        apiRoutesLoader.setScopesMap(scopeMap);
        Assertions.assertEquals(apiRoutesLoader.getScopesMap(), scopeMap);
        apiRoutesLoader.getApiRoutes();
    }

    @Test
    void testPrivateValidate() throws Exception {
        ApiRoutesConfig apiRoutesConfig = new ApiRoutesConfig();
        apiRoutesConfig.setRoutes(List.of(RegistryCommonTestUtil.getRouteDefination()));
        List<RouteDefinition> route = apiRoutesConfig.getRoutes();
        Method privateMethod = ApiRoutesLoader.class.getDeclaredMethod("validate", RouteDefinition.class);
        privateMethod.setAccessible(true);
        ReflectionTestUtils.invokeMethod(apiRoutesLoader, "validate", route.get(0));
        Assertions.assertNotNull(route.get(0));
    }

    @Test
    void testInvalidRoute() throws Exception {
        try {
            Method privateMethod = ApiRoutesLoader.class.getDeclaredMethod("validate", RouteDefinition.class);
            privateMethod.setAccessible(true);
            RouteDefinition invalidRoute = null;
            ReflectionTestUtils.invokeMethod(apiRoutesLoader, "validate", invalidRoute);
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals(IllegalArgumentException.class, ex.getClass());
        }
    }

    @Test
    void testInvalidRouteConfig() throws Exception {
        try {
            Method privateMethod = ApiRoutesLoader.class.getDeclaredMethod("validate", RouteDefinition.class);
            privateMethod.setAccessible(true);
            RouteDefinition invalidRouteConfig = new RouteDefinition();
            ReflectionTestUtils.invokeMethod(apiRoutesLoader, "validate", invalidRouteConfig);
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals(IllegalArgumentException.class, ex.getClass());
        }
    }

    @Test
    void testInvalidRouteUri() throws Exception {
        try {
            Method privateMethod = ApiRoutesLoader.class.getDeclaredMethod("validate", RouteDefinition.class);
            privateMethod.setAccessible(true);
            RouteDefinition invalidRouteUri = new RouteDefinition();
            invalidRouteUri.setId("test-id");
            ReflectionTestUtils.invokeMethod(apiRoutesLoader, "validate", invalidRouteUri);
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals(IllegalArgumentException.class, ex.getClass());
        }
    }

    @Test
    void testGetServerUrl() {
        Assertions.assertNotNull(apiRoutesLoader.getServerUrl(null, null));
    }

    private OpenAPI getOpenApiObj() {
        OpenAPI openApi = new OpenAPI();
        Paths paths = getPathsObj();
        openApi.setPaths(paths);
        Components components = new Components();
        @SuppressWarnings("rawtypes")
        Map<String, Schema> schemaMap = new HashMap<>();
        schemaMap.put("Test", getSchemaObj());
        components.setSchemas(schemaMap);
        openApi.setComponents(components);
        return openApi;
    }

    private Paths getPathsObj() {
        Paths paths = new Paths();
        paths.addPathItem("POST", new PathItem().post(getOperationObj()));
        paths.addPathItem("GET", new PathItem().get(getOperationObj()));
        paths.addPathItem("PUT", new PathItem().put(getOperationObj()));
        paths.addPathItem("DELETE", new PathItem().delete(getOperationObj()));
        paths.addPathItem("PATCH", new PathItem().patch(getOperationObj()));
        paths.addPathItem("OPTIONS", new PathItem().options(getOperationObj()));
        return paths;
    }

    private Operation getOperationObj() {
        Operation operation = new Operation();
        operation.setTags(List.of("test-controller", "get"));
        operation.setOperationId("create");
        operation.setRequestBody(getRequestBodyObj());
        operation.setSecurity(List.of(new SecurityRequirement().addList("JwtAuthValidator", "SelfManage")));
        return operation;
    }

    private RequestBody getRequestBodyObj() {
        RequestBody requestBody = new RequestBody();
        Content content = new Content();
        MediaType mediaType = new MediaType();
        Schema<?> schema = getSchemaObj();
        mediaType.setSchema(schema);
        content.put("application/json", mediaType);
        requestBody.setContent(content);
        return requestBody;
    }

    @Test
    void testExtension() {
        Operation operation = getOperationObj();
        operation.setTags(List.of("GET"));
        operation.setOperationId("route123");
        operation.setExtensions(Map.of("cacheSize", "100", "cacheKey", "key123", "cacheTll", "100"));
        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.put("filterName", List.of("ABCScope", "ABDScope"));
        operation.setSecurity(List.of(securityRequirement));
        apiRoutesLoader.setScopesMap(Map.of("GET-route123", List.of("ABCScope")));
        ReflectionTestUtils.setField(apiRoutesLoader, "isOverrideScopeEnabled", true);
        ReflectionTestUtils.invokeMethod(apiRoutesLoader, "setOperation", HttpMethod.GET, "/v2/users", operation);
        @SuppressWarnings("unchecked")
        List<RouteDefinition> apiRotes = (List<RouteDefinition>)
                ReflectionTestUtils.getField(apiRoutesLoader, "apiRoutes");
        Assertions.assertFalse(apiRotes.isEmpty());
        Assertions.assertTrue(apiRotes.stream().anyMatch(r -> r.getId().equals("GET-route123")));
    }

    @Test
    void testHeaderMetadata() {
        Operation operation = getOperationObj();
        operation.setTags(List.of("GET"));
        operation.setOperationId("testHeaderMetadata");
        operation.parameters(
                List.of(
                        new Parameter().name("dummyHeader").in("header").required(true)
                                .schema(new Schema<>().type("string")),
                        new Parameter().name("dummyHeader2").in("header").required(null)
                                .schema(new Schema<>().type("string"))));
        operation.setExtensions(Map.of("cacheSize", "100", "cacheKey", "key123", "cacheTll", "100"));
        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.put("filterName", List.of("ABCScope", "ABDScope"));
        operation.setSecurity(List.of(securityRequirement));
        apiRoutesLoader.setScopesMap(Map.of("GET-testHeaderMetadata", List.of("ABCScope")));
        ReflectionTestUtils.setField(apiRoutesLoader, "isOverrideScopeEnabled", true);
        ReflectionTestUtils.invokeMethod(apiRoutesLoader, "setOperation", HttpMethod.GET, "/v2/users", operation);
        @SuppressWarnings("unchecked")
        List<RouteDefinition> apiRotes = (List<RouteDefinition>)
                ReflectionTestUtils.getField(apiRoutesLoader, "apiRoutes");
        Assertions.assertNotNull(apiRotes);
        Assertions.assertFalse(apiRotes.isEmpty());
        Optional<RouteDefinition> route = apiRotes.stream()
                .filter(r -> r.getId().equals("GET-testHeaderMetadata"))
                .findFirst();
        Assertions.assertTrue(route.isPresent());
        Assertions.assertTrue(route.get().getMetadata().containsKey("headers"));
    }

    @Test
    void testHeaderOptionalMetadata() {
        Operation operation = getOperationObj();
        operation.setTags(List.of("GET"));
        operation.setOperationId("testHeaderOptionalMetadata");
        operation.parameters(
                List.of(
                        new Parameter().name("dummyHeader").in("header").required(true)
                                .schema(new Schema<>().type("string")),
                        new Parameter().name("dummyHeader2").in("header").required(null)
                                .schema(new Schema<>().type("string"))
                ));
        operation.setExtensions(Map.of("cacheSize", "100", "cacheKey", "key123", "cacheTll", "100"));
        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.put("filterName", List.of("ABCScope", "ABDScope"));
        operation.setSecurity(List.of(securityRequirement));
        apiRoutesLoader.setScopesMap(Map.of("GET-testHeaderOptionalMetadata", List.of("ABCScope")));
        ReflectionTestUtils.setField(apiRoutesLoader, "isOverrideScopeEnabled", true);
        ReflectionTestUtils.invokeMethod(apiRoutesLoader, "setOperation", HttpMethod.GET, "/v2/users", operation);
        @SuppressWarnings("unchecked")
        List<RouteDefinition> apiRotes = (List<RouteDefinition>)
                ReflectionTestUtils.getField(apiRoutesLoader, "apiRoutes");
        Assertions.assertNotNull(apiRotes);
        Assertions.assertFalse(apiRotes.isEmpty());
        Optional<RouteDefinition> route = apiRotes.stream()
                .filter(r -> r.getId().equals("GET-testHeaderOptionalMetadata"))
                .findFirst();
        Assertions.assertTrue(route.isPresent());
        Assertions.assertTrue(route.get().getMetadata().containsKey("headers"));
    }

    @Test
    void testEmptyHeaders() {
        Operation operation = getOperationObj();
        operation.setTags(List.of("GET"));
        operation.setOperationId("testEmptyHeaders");
        operation.parameters(List.of());
        operation.setExtensions(Map.of("cacheSize", "100", "cacheKey", "key123", "cacheTll", "100"));
        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.put("filterName", List.of("ABCScope", "ABDScope"));
        operation.setSecurity(List.of(securityRequirement));
        apiRoutesLoader.setScopesMap(Map.of("GET-testEmptyHeaders", List.of("ABCScope")));
        ReflectionTestUtils.setField(apiRoutesLoader, "isOverrideScopeEnabled", true);
        ReflectionTestUtils.invokeMethod(apiRoutesLoader, "setOperation", HttpMethod.GET, "/v2/users", operation);
        @SuppressWarnings("unchecked")
        List<RouteDefinition> apiRotes = (List<RouteDefinition>)
                ReflectionTestUtils.getField(apiRoutesLoader, "apiRoutes");
        Assertions.assertNotNull(apiRotes);
        Assertions.assertFalse(apiRotes.isEmpty());
        Optional<RouteDefinition> route = apiRotes.stream()
                .filter(r -> r.getId().equals("GET-testEmptyHeaders"))
                .findFirst();
        Assertions.assertTrue(route.isPresent());
        Assertions.assertFalse(route.get().getMetadata().containsKey("headers"));
    }

    @Test
    void testRouteWithNoFiltersConfigured() {
        // Create an operation without any filters (no security, no request body, no caching, no custom filters)
        Operation operation = new Operation();
        operation.setTags(List.of("test-controller"));
        operation.setOperationId("no-filters-route");
        operation.setRequestBody(null); // No request body to avoid request body filters
        operation.setSecurity(null); // No security to avoid security filters
        operation.setExtensions(null); // No extensions to avoid caching and custom filters
        operation.setParameters(null); // No parameters
        
        // Set contextPath to null to avoid rewrite path filter
        ReflectionTestUtils.setField(apiRoutesLoader, "contextPath", null);
        
        // Set required fields for route creation
        try {
            ReflectionTestUtils.setField(apiRoutesLoader, "serviceUrl", 
                    new java.net.URI("http://test-service:8080/"));
        } catch (java.net.URISyntaxException e) {
            Assertions.fail("Failed to set serviceUrl: " + e.getMessage());
        }
        ReflectionTestUtils.setField(apiRoutesLoader, "appName", "test-app");
        
        // Get initial route count
        @SuppressWarnings("unchecked")
        List<RouteDefinition> apiRoutesBefore = (List<RouteDefinition>)
                ReflectionTestUtils.getField(apiRoutesLoader, "apiRoutes");
        int initialRouteCount = apiRoutesBefore.size();
        
        // Invoke setOperation method
        ReflectionTestUtils.invokeMethod(apiRoutesLoader, "setOperation", HttpMethod.GET, "/v2/no-filters", operation);
        
        // Verify the route was added despite having no filters
        @SuppressWarnings("unchecked")
        List<RouteDefinition> apiRoutesAfter = (List<RouteDefinition>)
                ReflectionTestUtils.getField(apiRoutesLoader, "apiRoutes");
        Assertions.assertEquals(initialRouteCount + 1, apiRoutesAfter.size());
        
        // Find the specific route that was added
        Optional<RouteDefinition> route = apiRoutesAfter.stream()
                .filter(r -> r.getId().equals("test-controller-no-filters-route"))
                .findFirst();
        
        Assertions.assertTrue(route.isPresent());
        
        // Verify that the route has no filters configured
        RouteDefinition routeDefinition = route.get();
        Assertions.assertTrue(routeDefinition.getFilters().isEmpty(), 
                "Route should have no filters configured");
        
        // Verify route has the correct predicates (method and path)
        final int expectedPredicateCount = 2;
        Assertions.assertEquals(expectedPredicateCount, routeDefinition.getPredicates().size(), 
                "Route should have method and path predicates");
        
        // Verify the route has correct basic properties
        Assertions.assertEquals("test-controller-no-filters-route", routeDefinition.getId());
        Assertions.assertNotNull(routeDefinition.getUri());
    }
}
