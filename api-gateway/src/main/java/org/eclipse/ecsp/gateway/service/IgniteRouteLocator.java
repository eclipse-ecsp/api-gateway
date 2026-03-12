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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.config.SpringCloudGatewayConfig;
import org.eclipse.ecsp.gateway.customizers.RouteCustomizer;
import org.eclipse.ecsp.gateway.model.ApiService;
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.plugins.PluginLoader;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.ObjectMapperUtil;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.openapi4j.schema.validator.v3.SchemaValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.event.FilterArgsEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.Route.AbstractBuilder;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.BooleanSpec;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.cloud.gateway.support.HasRouteId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dynamically load routes from api-registry.
 */
@Service
@SuppressWarnings({"rawtypes", "unchecked"})
@ConditionalOnProperty(value = "api.dynamic.routes.enabled", havingValue = "true", matchIfMissing = false)
@ConfigurationProperties(prefix = "api")
public class IgniteRouteLocator implements RouteLocator {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(IgniteRouteLocator.class);
    private static final String LOCAL_RESPONSE_FILTER = "LocalResponseCache";
    private static final String CACHE_FILTER = "CacheFilter";
    private static final String TIME_TO_LIVE = "timeToLive";
    private static final String CACHE_SIZE = "size";
    private static final String REDIS_CACHE = "redis";
    private static final String LOCAL_CACHE = "local";
    private static final String OVERRIDE_FILTER_NAME_KEY = "filterName";
    @Getter
    private final Set<ApiService> apiDocRoutes = new TreeSet<>();
    private final ConfigurationService configurationService;
    private final Map<String, GatewayFilterFactory> gatewayFilterFactories = new HashMap<>();
    private final GatewayProperties gatewayProperties;
    @Getter
    @Setter
    Map<String, Map<String, String>> overrideFilterConfig;
    @Value("${api.caching.enabled:false}")
    private boolean isCacheEnabled;

    @Value("${api.caching.type:local}")
    private String cacheType;

    private final ApiRegistryClient apiRegistryClient;

    private final RouteLocatorBuilder routeLocatorBuilder;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final SpringCloudGatewayConfig springCloudGatewayConfig;

    @Value("${api.isFilterOverrideEnabled}")
    private boolean isFilterOverrideEnabled;

    private boolean isCustomPluginsEnabled = false;

    List<RouteCustomizer> routeCustomizers;

    /**
     * Constructor to initialize the IgniteRouteLocator with required dependencies.
     *
     * @param configurationService   the configuration service
     * @param gatewayFilterFactories the list of gateway filter factories
     * @param gatewayProperties      the gateway properties
     * @param pluginEnabled          flag to enable custom plugins
     * @param pluginLoader           the plugin loader
     * @param apiRegistryClient    the registry route loader
     * @param routeLocatorBuilder    the route locator builder
     * @param applicationEventPublisher the application event publisher
     * @param springCloudGatewayConfig  the Spring Cloud Gateway configuration
     * @param routeCustomizers       the list of route customizers
     */
    public IgniteRouteLocator(ConfigurationService configurationService,
                              List<GatewayFilterFactory> gatewayFilterFactories,
                              GatewayProperties gatewayProperties,
                              @Value("${plugin.enabled}") boolean pluginEnabled,
                              PluginLoader pluginLoader,
                              ApiRegistryClient apiRegistryClient,
                              RouteLocatorBuilder routeLocatorBuilder,
                              ApplicationEventPublisher applicationEventPublisher,
                              SpringCloudGatewayConfig springCloudGatewayConfig,
                              List<RouteCustomizer> routeCustomizers) {
        this.configurationService = configurationService;
        this.apiRegistryClient = apiRegistryClient;
        this.routeLocatorBuilder = routeLocatorBuilder;
        this.applicationEventPublisher = applicationEventPublisher;
        this.springCloudGatewayConfig = springCloudGatewayConfig;
        this.routeCustomizers = routeCustomizers;
        gatewayFilterFactories.forEach(factory -> this.gatewayFilterFactories.put(factory.name(), factory));
        if (pluginEnabled) {
            isCustomPluginsEnabled = true;
            LOGGER.debug("Loading custom plugins...");
            List<Object> plugins = pluginLoader.loadPlugins();
            plugins.forEach(p -> {
                GatewayFilterFactory plugin = (GatewayFilterFactory) p;
                LOGGER.info("Registering Filter factory with custom filter with name: {}", plugin.name());
                this.gatewayFilterFactories.put(plugin.name(), plugin);
            });
            LOGGER.debug("Loaded custom plugins...");
        }
        this.gatewayFilterFactories.keySet().forEach(factory ->
            LOGGER.info("Registered filter name : {}", factory)
        );
        this.gatewayProperties = gatewayProperties;
    }

    /**
     * validate the filter override configuration and log the details.
     */
    @PostConstruct
    public void init() {
        if (isCustomPluginsEnabled) {
            LOGGER.info("Custom plugins are enabled...");
            if (isFilterOverrideEnabled && overrideFilterConfig != null && !overrideFilterConfig.isEmpty()) {
                LOGGER.info("Filter override configuration is enabled, validating filter overrides configuration...");
                overrideFilterConfig.forEach((filterName, filterConfig) -> {
                    LOGGER.debug("validating filter override for: {}, filter config: {}", filterName, filterConfig);
                    Map<String, String> overrideFilterMap = overrideFilterConfig.get(filterName);
                    String customFilterName = overrideFilterMap.get(OVERRIDE_FILTER_NAME_KEY);
                    if (gatewayFilterFactories.containsKey(customFilterName)) {
                        LOGGER.info("Overriding filter: {} with configuration: {} is validated",
                                filterName, filterConfig);
                    } else {
                        LOGGER.error("Overriding filter {} not found in registered filters.",
                                customFilterName);
                        throw new IllegalArgumentException("Overriding filter "
                                + customFilterName + " not found in registered filters"
                                + ", please check the filter override configuration.");
                    }
                });
            }
        } else {
            LOGGER.info("Custom plugins are not enabled, using default filters.");
        }
    }

    /**
     * publish refresh route event.
     */
    public void refreshRoutes() {
        LOGGER.debug("Reloading the Routes");
        applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }

    /**
     * Loading all Default Filters.
     */
    private void loadDefaultFilters() {
        LOGGER.debug("Loading Default Filters");
        if (springCloudGatewayConfig != null && springCloudGatewayConfig.getDefaultFilters() != null) {
            this.gatewayProperties.setDefaultFilters(springCloudGatewayConfig.getDefaultFilters());
        }
        LOGGER.debug("Default Filters: {}", this.gatewayProperties.getDefaultFilters());
    }

    @Override
    public Flux<Route> getRoutes() {
        LOGGER.debug("get Routes");
        apiDocRoutes.clear();
        // Load Default filter applicable for all the routes
        loadDefaultFilters();
        // Load the ApiRoutes from api-registry
        RouteLocatorBuilder.Builder routesBuilder = routeLocatorBuilder.routes();
        Flux<IgniteRouteDefinition> apiRoutes = apiRegistryClient.getRoutes();
        // Build api-gateway routes on the ApiRoutes received from api-registry
        Flux<Route> routes = apiRoutes
                .filter(r -> validateRouteFilters(r, r.getFilters()))
                .map(apiRoute -> routesBuilder.route(apiRoute.getId(),
                        predicateSpec -> setPredicateSpec(apiRoute, predicateSpec)))
                .collectList().flatMapMany(builders -> routesBuilder.build().getRoutes());
        LOGGER.debug("Loaded Routes...!");
        return routes;
    }

    private boolean validateRouteFilters(IgniteRouteDefinition route, List<FilterDefinition> filters) {
        String filterName =  filters.stream().map(FilterDefinition::getName)
                .filter(f -> !gatewayFilterFactories.containsKey(f))
                .findFirst().orElse(null);

        if (filterName != null) {
            LOGGER.error("Route ID: {} has an invalid filter: {}, will not be exposed via api-gateway",
                    route.getId(), filterName);
            return false;
        }
        return true;
    }

    /**
     * Set the predicate spec.
     *
     * @param apiRoute      RouteDefinition object
     * @param predicateSpec PredicateSpec object
     * @return returns {@link Buildable} of {@link Route}
     */
    private Buildable<Route> setPredicateSpec(IgniteRouteDefinition apiRoute,
                                              PredicateSpec predicateSpec) {
        LOGGER.debug("set predicate spec {} ", predicateSpec);
        LOGGER.debug("ApiRoute -> {} ,{}", apiRoute.getId(), apiRoute.getPredicates());
        //set api doc route
        setApiDocRoute(apiRoute);

        // set the service name to metadata
        apiRoute.getMetadata().put(GatewayConstants.SERVICE_NAME, apiRoute.getService());

        BooleanSpec booleanSpec = predicateSpec.path(RouteUtils.getRoutePath(apiRoute.getPredicates()));
        String method = RouteUtils.getRouteMethod(apiRoute.getPredicates());
        LOGGER.debug("method ---{}", method);
        if (StringUtils.hasLength(method)) {
            booleanSpec.and().method(method);
        }
        LOGGER.debug("method name after booleanspec---{}", booleanSpec.toString());
        List<GatewayFilter> filters = new ArrayList<>();
        if (!this.gatewayProperties.getDefaultFilters().isEmpty()) {
            filters.addAll(
                    loadGatewayFilters(apiRoute.getId(), new ArrayList<>(this.gatewayProperties.getDefaultFilters())));
        }
        
        // apply route customizers if any
        if (routeCustomizers != null && !routeCustomizers.isEmpty()) {
            LOGGER.debug("Applying route customizers...");
            for (RouteCustomizer customizer : routeCustomizers) {
                apiRoute = (IgniteRouteDefinition) customizer.customize(apiRoute, apiRoute);
            }
        }
        
        Buildable<Route> route = booleanSpec.uri(apiRoute.getUri());
        LOGGER.debug("route---{}", route);
        if (apiRoute.getFilters() != null && !apiRoute.getFilters().isEmpty()) {
            setCacheFilter(apiRoute);

            filters.addAll(getFilters(apiRoute));
            LOGGER.debug("Gateway Filters: " + filters);
        }
        
        // add the configured filters
        ((AbstractBuilder) route).filters(filters);
        if (apiRoute.getMetadata() != null) {
            // Add the schema validator, if configured
            // this schema validator is used by RequestBodyValidatorFilter
            Object schemaObj = apiRoute.getMetadata().get(GatewayConstants.SCHEMA);
            if (schemaObj != null) {
                try {
                    JsonNode schemaNode = ObjectMapperUtil.getObjectMapper().readTree((String) schemaObj);
                    SchemaValidator schemaValidator = new SchemaValidator(null, schemaNode);
                    apiRoute.getMetadata().put(GatewayConstants.SCHEMA_VALIDATOR, schemaValidator);
                    LOGGER.debug("Request Body SchemaValidator added for route: {}", apiRoute.getId());
                } catch (Exception e) {
                    LOGGER.warn("Error while adding schema for route: {}", apiRoute.getId(), e);
                }
            }
            // set the metadata if configured
            ((AbstractBuilder) route).metadata(apiRoute.getMetadata());
        }
        LOGGER.debug("route in setPredicates {} ", route);
        return route;
    }

    private void setCacheFilter(IgniteRouteDefinition apiRoute) {
        FilterDefinition fd = new FilterDefinition();
        LOGGER.debug("Enabled Cache Type {}", cacheType);
        if (apiRoute.getCacheKey() != null && isCacheEnabled) {
            if (cacheType.equalsIgnoreCase(REDIS_CACHE)) {
                LOGGER.debug("cache Key---- {}", apiRoute.getCacheKey());
                fd.setName(CACHE_FILTER);
                String cacheKey = apiRoute.getCacheKey();
                fd.addArg("cacheKey", cacheKey);
            } else if (cacheType.equalsIgnoreCase(LOCAL_CACHE)) {
                LOGGER.debug("cache filter---- {}", LOCAL_RESPONSE_FILTER);
                fd.setName(LOCAL_RESPONSE_FILTER);
                fd.addArg(CACHE_SIZE, apiRoute.getCacheSize());
                fd.addArg(TIME_TO_LIVE, apiRoute.getCacheTtl());
            }
            apiRoute.getFilters().add(fd);
            LOGGER.debug("added the caching filter {}", fd.toString());
        }
    }

    private void setApiDocRoute(IgniteRouteDefinition apiRoute) {
        if (Boolean.TRUE.equals(apiRoute.getApiDocs())) {
            String serviceName = apiRoute.getService();
            String path = "/v3/api-docs/" + apiRoute.getService();
            ApiService service = new ApiService(apiRoute.getService(), path, "Api-Docs of " + serviceName);
            apiDocRoutes.add(service);
            LOGGER.debug("API-Docs added: {}", getApiDocRoutes());
            LOGGER.debug("ApiDoc Route -> {}", service);
        }
    }

    /**
     * Get the filters for the route.
     *
     * @param apiRoute RouteDefinition object
     * @return List of GatewayFilter
     */
    private List<GatewayFilter> getFilters(RouteDefinition apiRoute) {
        LOGGER.debug("Fetching gateway filters");
        List<GatewayFilter> filters = new ArrayList<>();
        if (apiRoute.getFilters() != null && !apiRoute.getFilters().isEmpty()) {
            List<FilterDefinition> filterDefinitions = new ArrayList<>();
            apiRoute.getFilters().forEach(filter -> {
                FilterDefinition fd = new FilterDefinition();
                if (isFilterOverrideEnabled && overrideFilterConfig.containsKey(filter.getName())) {
                    Map<String, String> overrideFilterMap = overrideFilterConfig.get(filter.getName());
                    if (overrideFilterMap != null && overrideFilterMap.get(OVERRIDE_FILTER_NAME_KEY) != null) {
                        LOGGER.info("{} filter overridden successfully with filter: {}", filter.getName(),
                                overrideFilterMap.get(OVERRIDE_FILTER_NAME_KEY));
                        fd.setName(overrideFilterMap.get(OVERRIDE_FILTER_NAME_KEY));
                        fd.setArgs(filter.getArgs());
                    } else {
                        LOGGER.error("Filter {} is not overridden, check override filter configuration",
                                filter.getName());
                        fd.setName(filter.getName());
                        fd.setArgs(filter.getArgs());
                    }
                } else {
                    fd.setName(filter.getName());
                    fd.setArgs(filter.getArgs());
                }
                filterDefinitions.add(fd);
                LOGGER.debug("Filter loaded:" + filter);
            });
            filters.addAll(loadGatewayFilters(apiRoute.getId(), filterDefinitions));
        }

        AnnotationAwareOrderComparator.sort(filters);
        LOGGER.debug("Filters -> {}", filters);
        return filters;
    }

    /**
     * Load the gateway filters.
     *
     * @param id                Route ID
     * @param filterDefinitions List of FilterDefinition
     * @return List of GatewayFilter
     */
    private List<GatewayFilter> loadGatewayFilters(String id, List<FilterDefinition> filterDefinitions) {
        LOGGER.debug("Loading Gateway filters");
        ArrayList<GatewayFilter> ordered = new ArrayList<>(filterDefinitions.size());
        for (int i = 0; i < filterDefinitions.size(); i++) {
            FilterDefinition definition = filterDefinitions.get(i);
            LOGGER.debug("RouteDefinition: {} with filter :{}, args: {}", id, definition.getName(),
                    definition.getArgs());
            GatewayFilterFactory factory = this.gatewayFilterFactories.get(definition.getName());
            if (factory == null) {
                LOGGER.warn("Unable to find GatewayFilterFactory with name " + definition.getName());
                continue;
            }
            // @formatter:off
            Object configuration = this.configurationService.with(factory)
                .name(definition.getName())
                .properties(definition.getArgs())
                .eventFunction((bound, properties) -> new FilterArgsEvent(
                    // why explicit cast needed or java compile fails, check and fix later
                    IgniteRouteLocator.this, id, (Map<String, Object>) properties))
                .bind();
            // @formatter:on

            // some filters require routeId
            if (configuration instanceof HasRouteId hasRouteId) {
                hasRouteId.setRouteId(id);
            }

            GatewayFilter gatewayFilter = factory.apply(configuration);
            if (gatewayFilter instanceof Ordered) {
                ordered.add(gatewayFilter);
            } else {
                ordered.add(new OrderedGatewayFilter(gatewayFilter, i + 1));
            }
        }

        return ordered;
    }
}
