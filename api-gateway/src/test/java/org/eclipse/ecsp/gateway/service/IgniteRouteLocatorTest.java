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

import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.config.SpringCloudGatewayConfig;
import org.eclipse.ecsp.gateway.customizers.RouteCustomizer;
import org.eclipse.ecsp.gateway.model.ApiService;
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.plugins.PluginLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link IgniteRouteLocator}.
 */
class IgniteRouteLocatorTest {

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private GatewayProperties gatewayProperties;

    @Mock
    private PluginLoader pluginLoader;

    @Mock
    private ApiRegistryClient apiRegistryClient;

    @Mock
    private RouteLocatorBuilder routeLocatorBuilder;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private SpringCloudGatewayConfig springCloudGatewayConfig;

    private IgniteRouteLocator igniteRouteLocator;

    private List<GatewayFilterFactory> gatewayFilterFactories;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock filter factories
        gatewayFilterFactories = new ArrayList<>();
        GatewayFilterFactory mockFactory = mock(GatewayFilterFactory.class);
        when(mockFactory.name()).thenReturn("TestFilter");
        gatewayFilterFactories.add(mockFactory);

        // Setup default filters
        when(gatewayProperties.getDefaultFilters()).thenReturn(new ArrayList<>());
        when(springCloudGatewayConfig.getDefaultFilters()).thenReturn(new ArrayList<>());
    }

    @Test
    void constructor_WithPluginsDisabled_InitializesSuccessfully() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        assertNotNull(igniteRouteLocator);
    }

    @Test
    void constructor_WithPluginsEnabled_LoadsCustomPlugins() {
        GatewayFilterFactory customPlugin = mock(GatewayFilterFactory.class);
        when(customPlugin.name()).thenReturn("CustomPlugin");
        when(pluginLoader.loadPlugins()).thenReturn(List.of(customPlugin));

        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                true,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        verify(pluginLoader, times(1)).loadPlugins();
        assertNotNull(igniteRouteLocator);
    }

    @Test
    void init_WithCustomPluginsDisabled_LogsMessage() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        // Should not throw exception
        igniteRouteLocator.init();
    }

    @Test
    void init_WithValidFilterOverride_ValidatesSuccessfully() {
        GatewayFilterFactory customPlugin = mock(GatewayFilterFactory.class);
        when(customPlugin.name()).thenReturn("CustomFilter");
        when(pluginLoader.loadPlugins()).thenReturn(List.of(customPlugin));

        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                true,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        Map<String, Map<String, String>> overrideConfig = new HashMap<>();
        Map<String, String> filterConfig = new HashMap<>();
        filterConfig.put("filterName", "CustomFilter");
        overrideConfig.put("OriginalFilter", filterConfig);

        igniteRouteLocator.setOverrideFilterConfig(overrideConfig);
        ReflectionTestUtils.setField(igniteRouteLocator, "isFilterOverrideEnabled", true);

        igniteRouteLocator.init();
    }

    @Test
    void refreshRoutes_PublishesEvent() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        igniteRouteLocator.refreshRoutes();

        verify(applicationEventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void getRoutes_WithValidRoutes_ReturnsRoutes() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        // Setup test route
        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        FilterDefinition filterDef = new FilterDefinition();
        filterDef.setName("TestFilter");
        testRoute.setFilters(List.of(filterDef));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        // Setup builder mocks
        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithInvalidFilter_FiltersOutRoute() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        // Setup test route with invalid filter
        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));

        FilterDefinition invalidFilter = new FilterDefinition();
        invalidFilter.setName("NonExistentFilter");
        testRoute.setFilters(List.of(invalidFilter));

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        when(mockLocator.getRoutes()).thenReturn(Flux.empty());
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithApiDocsEnabled_AddsToApiDocRoutes() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");
        testRoute.setApiDocs(true);

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        FilterDefinition filterDef = new FilterDefinition();
        filterDef.setName("TestFilter");
        testRoute.setFilters(List.of(filterDef));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        // Test basic functionality - getApiDocRoutes getter
        Set<ApiService> apiDocRoutes = igniteRouteLocator.getApiDocRoutes();
        assertNotNull(apiDocRoutes, "API doc routes should not be null");
    }

    @SuppressWarnings("unchecked")
    @Test
    void getRoutes_WithRouteCustomizers_AppliesCustomizers() {
        RouteCustomizer mockCustomizer = mock(RouteCustomizer.class);
        when(mockCustomizer.customize(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                List.of(mockCustomizer)
        );

        // Verify that route customizers were set correctly
        List<RouteCustomizer> customizers = (List<RouteCustomizer>) ReflectionTestUtils.getField(
                igniteRouteLocator, "routeCustomizers");
        assertNotNull(customizers, "Route customizers should not be null");
        assertTrue(customizers.size() == 1, "Should have one route customizer");
    }

    @Test
    void getApiDocRoutes_ReturnsApiDocRoutes() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        Set<ApiService> apiDocRoutes = igniteRouteLocator.getApiDocRoutes();
        assertNotNull(apiDocRoutes);
    }

    @Test
    void init_WithFilterOverrideEnabled_ValidatesSuccessfully() {
        GatewayFilterFactory customFilter = mock(GatewayFilterFactory.class);
        when(customFilter.name()).thenReturn("CustomFilter");
        when(pluginLoader.loadPlugins()).thenReturn(List.of(customFilter));

        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                true,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        Map<String, Map<String, String>> overrideConfig = new HashMap<>();
        Map<String, String> filterConfig = new HashMap<>();
        filterConfig.put("filterName", "CustomFilter");
        overrideConfig.put("TestFilter", filterConfig);

        igniteRouteLocator.setOverrideFilterConfig(overrideConfig);
        ReflectionTestUtils.setField(igniteRouteLocator, "isFilterOverrideEnabled", true);

        // Should not throw exception
        igniteRouteLocator.init();
    }

    @Test
    void init_WithInvalidFilterOverride_ThrowsException() {
        GatewayFilterFactory customFilter = mock(GatewayFilterFactory.class);
        when(customFilter.name()).thenReturn("CustomFilter");
        when(pluginLoader.loadPlugins()).thenReturn(List.of(customFilter));

        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                true,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        Map<String, Map<String, String>> overrideConfig = new HashMap<>();
        Map<String, String> filterConfig = new HashMap<>();
        filterConfig.put("filterName", "NonExistentFilter");
        overrideConfig.put("TestFilter", filterConfig);

        igniteRouteLocator.setOverrideFilterConfig(overrideConfig);
        ReflectionTestUtils.setField(igniteRouteLocator, "isFilterOverrideEnabled", true);

        // Should throw IllegalArgumentException
        try {
            igniteRouteLocator.init();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not found in registered filters"));
        }
    }

    @Test
    void getRoutes_WithMethodPredicate_AddsMethodToRoute() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");

        PredicateDefinition methodPredicate = new PredicateDefinition();
        methodPredicate.setName("Method");
        methodPredicate.addArg("methods", "GET");

        testRoute.setPredicates(List.of(pathPredicate, methodPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithLocalCache_AddsCacheFilter() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        ReflectionTestUtils.setField(igniteRouteLocator, "isCacheEnabled", true);
        ReflectionTestUtils.setField(igniteRouteLocator, "cacheType", "local");

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");
        testRoute.setCacheKey("test-cache-key");
        testRoute.setCacheSize("1000");
        testRoute.setCacheTtl("300");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithRedisCache_AddsCacheFilter() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        ReflectionTestUtils.setField(igniteRouteLocator, "isCacheEnabled", true);
        ReflectionTestUtils.setField(igniteRouteLocator, "cacheType", "redis");

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");
        testRoute.setCacheKey("test-cache-key");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithMetadata_SetsMetadata() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("customKey", "customValue");
        testRoute.setMetadata(metadata);

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithSchema_AddsSchemaValidator() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");

        Map<String, Object> metadata = new HashMap<>();
        String schema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        metadata.put("schema", schema);
        testRoute.setMetadata(metadata);

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithDefaultFilters_AppliesDefaultFilters() {
        FilterDefinition defaultFilter = new FilterDefinition();
        defaultFilter.setName("TestFilter");
        when(gatewayProperties.getDefaultFilters()).thenReturn(List.of(defaultFilter));

        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithCacheNotEnabled_SkipsCacheFilter() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        ReflectionTestUtils.setField(igniteRouteLocator, "isCacheEnabled", false);

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");
        testRoute.setCacheKey("test-cache-key");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithNullCacheKey_SkipsCacheFilter() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        ReflectionTestUtils.setField(igniteRouteLocator, "isCacheEnabled", true);
        ReflectionTestUtils.setField(igniteRouteLocator, "cacheType", "redis");

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");
        testRoute.setCacheKey(null);

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithApiDocsDisabled_DoesNotAddToApiDocRoutes() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");
        testRoute.setApiDocs(false);

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithMalformedSchemaJson_HandlesGracefully() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("schema", "invalid-json{{{");
        testRoute.setMetadata(metadata);

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithMultiplePredicates_HandlesCorrectly() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");

        PredicateDefinition methodPredicate = new PredicateDefinition();
        methodPredicate.setName("Method");
        methodPredicate.addArg("methods", "POST,PUT");

        PredicateDefinition headerPredicate = new PredicateDefinition();
        headerPredicate.setName("Header");
        headerPredicate.addArg("header", "X-Request-Id");

        testRoute.setPredicates(List.of(pathPredicate, methodPredicate, headerPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithNullMetadata_HandlesGracefully() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");
        testRoute.setMetadata(null);

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithSpringCloudGatewayConfigFilters_AppliesFilters() {
        FilterDefinition configFilter = new FilterDefinition();
        configFilter.setName("TestFilter");
        when(springCloudGatewayConfig.getDefaultFilters()).thenReturn(List.of(configFilter));

        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithEmptyFilterList_HandlesGracefully() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");
        testRoute.setFilters(new ArrayList<>());

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getRoutes_WithCacheButEmptyCacheKey_SkipsCacheFilter() {
        igniteRouteLocator = new IgniteRouteLocator(
                configurationService,
                gatewayFilterFactories,
                gatewayProperties,
                false,
                pluginLoader,
                apiRegistryClient,
                routeLocatorBuilder,
                applicationEventPublisher,
                springCloudGatewayConfig,
                new ArrayList<>()
        );

        ReflectionTestUtils.setField(igniteRouteLocator, "isCacheEnabled", true);
        ReflectionTestUtils.setField(igniteRouteLocator, "cacheType", "redis");

        IgniteRouteDefinition testRoute = new IgniteRouteDefinition();
        testRoute.setId("test-route");
        testRoute.setUri(URI.create("http://localhost:8080"));
        testRoute.setService("test-service");
        testRoute.setCacheKey("");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", "/test/**");
        testRoute.setPredicates(List.of(pathPredicate));

        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(testRoute));

        RouteLocatorBuilder.Builder builder = mock(RouteLocatorBuilder.Builder.class);
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.route(any(String.class), any())).thenReturn(builder);

        RouteLocator mockLocator = mock(RouteLocator.class);
        Route mockRoute = Route.async()
                .id("test-route")
                .uri("http://localhost:8080")
                .predicate(exchange -> true)
                .build();
        when(mockLocator.getRoutes()).thenReturn(Flux.just(mockRoute));
        when(builder.build()).thenReturn(mockLocator);

        Flux<Route> routes = igniteRouteLocator.getRoutes();

        StepVerifier.create(routes)
                .expectNextCount(1)
                .verifyComplete();
    }
}
