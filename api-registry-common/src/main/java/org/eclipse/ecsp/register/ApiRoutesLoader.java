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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.register.model.FilterDefinition;
import org.eclipse.ecsp.register.model.PredicateDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.security.Security;
import org.eclipse.ecsp.utils.Constants;
import org.eclipse.ecsp.utils.ObjectMapperUtil;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springdoc.core.customizers.SpringDocCustomizers;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.SpringDocProviders;
import org.springdoc.core.service.AbstractRequestService;
import org.springdoc.core.service.GenericResponseService;
import org.springdoc.core.service.OpenAPIService;
import org.springdoc.core.service.OperationService;
import org.springdoc.webmvc.api.OpenApiResource;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * This class is the load the api-routes from Swagger Annotations.
 * This class loads the API routes from Swagger Annotations.
 *
 * <p>It extends the OpenApiResource class and uses SpringDoc to generate OpenAPI documentation.
 * It also supports conditional loading based on the 'api.registry.enabled' property.
 *
 * <p>The class reads API routes from both configuration and Swagger annotations, validates them,
 * and registers them with the API registry service.
 *
 * <p>It also sets various filters and predicates for the routes, including request body filters,
 * caching filters, security filters, and header metadata.
 *
 * <p>The class is annotated with @Service, @ConfigurationProperties, and @ConditionalOnProperty.
 *
 * @author SBala2
 */
@Service
@ConfigurationProperties(prefix = "scopes")
@ConditionalOnProperty(value = "api.registry.enabled", havingValue = "true", matchIfMissing = false)
public class ApiRoutesLoader extends OpenApiResource {
    /**
     * Constants for multipart/form-data.
     */
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    /**
     * Constants for request body schema logger.
     */
    public static final String REQUEST_BODY_SCHEMA = "===> RequestBody Schema : {}";
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiRoutesLoader.class);
    private final List<GroupedOpenApi> groupedOpenApis;
    private final List<RouteDefinition> apiRoutes;

    /**
     * API Routes configuration.
     */
    protected ApiRoutesConfig apiRoutesConfig;
    @Getter
    @Setter
    private Map<String, List<String>> scopesMap;
    @Value("${spring.application.name}")
    private String appName;
    @Value("${spring.application.servicename}")
    private String serviceName;
    @Value("${api.context-path:}")
    private String contextPath;
    @Value("${server.port}")
    private String port;
    @Value("${scopes.override.enabled:false}")
    private boolean isOverrideScopeEnabled;
    @Value("${api_gateway_caching_ttl:10m}")
    private String timeToLive;
    @Value("${api_gateway_cachesize:50MB}")
    private String cacheSize;
    private URI serviceUrl;
    private Components components = null;

    /**
     * Constructor for ApiRoutesLoader.
     *
     * @param groupedOpenApis             List of GroupedOpenApi objects.
     * @param openApiBuilderObjectFactory ObjectFactory for OpenAPIService.
     * @param requestBuilder              AbstractRequestService for building requests.
     * @param responseBuilder             GenericResponseService for building responses.
     * @param operationParser             OperationService for parsing operations.
     * @param springDocConfigProperties   SpringDocConfigProperties for configuration.
     * @param springDocProviders          SpringDocProviders for providing SpringDoc services.
     * @param springDocCustomizers        SpringDocCustomizers for customizing SpringDoc.
     * @param apiRoutesConfig             apiRoutesConfig
     */
    @Autowired
    public ApiRoutesLoader(final List<GroupedOpenApi> groupedOpenApis,
                           ObjectFactory<OpenAPIService> openApiBuilderObjectFactory,
                           AbstractRequestService requestBuilder,
                           GenericResponseService responseBuilder, OperationService operationParser,
                           SpringDocConfigProperties springDocConfigProperties,
                           SpringDocProviders springDocProviders, SpringDocCustomizers springDocCustomizers,
                           ApiRoutesConfig apiRoutesConfig) {
        super(openApiBuilderObjectFactory, requestBuilder, responseBuilder, operationParser, springDocConfigProperties,
                springDocProviders, springDocCustomizers);
        this.apiRoutes = new LinkedList<>();
        this.groupedOpenApis = groupedOpenApis;
        this.apiRoutesConfig = apiRoutesConfig;
    }

    /**
     * Retrieves the list of API routes.
     *
     * <p>This method initializes the API routes by calling the `prepareApiRoutes` and `prepareApiDocRoutes` methods.
     * It logs the application start and returns the list of API routes.
     *
     * @return List of `RouteDefinition` objects representing the API routes.
     * @throws URISyntaxException if there is an error in the URI syntax.
     */
    public List<RouteDefinition> getApiRoutes() throws URISyntaxException {
        LOGGER.info("Application started");
        prepareApiRoutes();
        prepareApiDocRoutes();
        return apiRoutes;
    }

    /**
     * Prepares the API routes by loading them from configuration and Swagger annotations.
     *
     * <p>This method first loads the API routes from the configuration,
     * validates them, and adds them to the list of API routes.
     * It then loads the API routes from Swagger annotations, sets the service URL, and adds the routes to the list.
     *
     * @throws URISyntaxException if there is an error in the URI syntax.
     */
    private void prepareApiRoutes() throws URISyntaxException {
        // Load from configuration
        LOGGER.debug("Scopes Map config: " + scopesMap);
        LOGGER.debug("Routes List: " + apiRoutesConfig.getRoutes());
        if (apiRoutesConfig.getRoutes() != null && !apiRoutesConfig.getRoutes().isEmpty()) {
            LOGGER.info("Read API Routes from OpenApi Configurations...");
            for (RouteDefinition route : apiRoutesConfig.getRoutes()) {
                validate(route);
                // set service to current micro-service name
                route.setService(appName);
                route.setContextPath(this.contextPath);
                apiRoutes.add(route);
                LOGGER.info("Route: {} is valid and added to register", route.getId());
            }
        }
        // Load from Swagger Annotations
        LOGGER.info("Read API Routes from OpenApi (Swagger) Annotations...");
        this.serviceUrl = new URI("http://" + serviceName + ":" + port + Constants.PATH_DELIMITER);
        LOGGER.info("OpenApi ---> ServiceUrl = {}", this.serviceUrl);
        OpenAPI api = super.getOpenApi(Locale.getDefault());
        if (api == null) {
            return;
        }
        Paths paths = api.getPaths();
        if (paths == null || paths.isEmpty()) {
            return;
        }
        components = api.getComponents();
        paths.keySet().forEach(k -> {
            PathItem pi = paths.get(k);
            if (pi != null) {
                try {
                    if (pi.getPost() != null) {
                        setOperation(HttpMethod.POST, k, pi.getPost());
                    }
                    if (pi.getGet() != null) {
                        setOperation(HttpMethod.GET, k, pi.getGet());
                    }
                    if (pi.getPut() != null) {
                        setOperation(HttpMethod.PUT, k, pi.getPut());
                    }
                    if (pi.getDelete() != null) {
                        setOperation(HttpMethod.DELETE, k, pi.getDelete());
                    }
                    if (pi.getPatch() != null) {
                        setOperation(HttpMethod.PATCH, k, pi.getPatch());
                    }
                    if (pi.getOptions() != null) {
                        setOperation(HttpMethod.OPTIONS, k, pi.getOptions());
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception occurred: " + e);
                }
            }
        });
    }

    /**
     * Validates the given RouteDefinition.
     *
     * <p>This method checks if the provided RouteDefinition is valid.
     * It ensures that the route is not null, has a non-blank ID, and a valid URI.
     *
     * @param route the RouteDefinition to validate
     * @throws IllegalArgumentException if the route is invalid
     */
    private void validate(RouteDefinition route) {
        if (route == null) {
            throw new IllegalArgumentException("Invalid Route found.");
        }
        if (route.getId() == null || route.getId().isBlank()) {
            throw new IllegalArgumentException("Invalid Route ID found.");
        }
        URI uri = route.getUri();
        // Each micro-service can route the APIs only to itself
        if (uri == null) {
            throw new IllegalArgumentException("Invalid URI in Route: " + route.getId());
        }
    }

    /**
     * Sets the operation for the given HTTP method and path.
     *
     * <p>This method creates a RouteDefinition for the given HTTP method and path,
     * sets various filters and predicates, and adds it to the list of API routes.
     *
     * @param method    the HTTP method (GET, POST, etc.)
     * @param path      the API path
     * @param operation the OpenAPI Operation object
     * @throws JsonProcessingException if there is an error in JSON processing
     */
    private void setOperation(HttpMethod method, String path, Operation operation) throws JsonProcessingException {
        try {
            LOGGER.debug("API path: " + path);
            RouteDefinition route = createRouteDefinition(method, path, operation);
            setRequestBodyFilters(operation, route);
            setCachingFilters(method, operation, route);
            setSecurityFilters(operation, route);
            setHeaderMetadata(operation, route);
            setRewritePathFilter(route);
            if (!route.getFilters().isEmpty()) {
                apiRoutes.add(route);
            }
            LOGGER.debug("API Route : {}", route);
        } catch (Exception ex) {
            LOGGER.error("Error registering route with method: {}, "
                    + "path: {}, operation: {}, error: {}", method, path, operation, ex);
        }
    }

    /**
     * Creates a RouteDefinition for the given HTTP method and path.
     *
     * <p>This method sets the route ID, service name, URI, context path, and predicates for the route.
     *
     * @param method    the HTTP method (GET, POST, etc.)
     * @param path      the API path
     * @param operation the OpenAPI Operation object
     * @return the created RouteDefinition
     */
    private RouteDefinition createRouteDefinition(HttpMethod method, String path, Operation operation) {
        RouteDefinition route = new RouteDefinition();
        operation.setOperationId(operation.getOperationId().replace("_", "-"));
        route.setId(operation.getTags().get(0) + "-" + operation.getOperationId());
        LOGGER.info("Route id: " + route.getId());
        route.setService(appName);
        route.setUri(serviceUrl);
        route.setContextPath(this.contextPath);
        LOGGER.debug("Route: {}", route.toString());
        setRoutePredicates(method, path, route);
        return route;
    }

    /**
     * Sets the predicates for the route.
     *
     * <p>This method sets the HTTP method and path predicates for the route.
     *
     * @param method the HTTP method (GET, POST, etc.)
     * @param path   the API path
     * @param route  the RouteDefinition object
     */
    private void setRoutePredicates(HttpMethod method, String path, RouteDefinition route) {
        PredicateDefinition methodPredicate = new PredicateDefinition();
        methodPredicate.setName(Constants.METHOD);
        methodPredicate.getArgs().put(Constants.KEY_0, method.name());
        route.getPredicates().add(methodPredicate);

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName(Constants.PATH);
        pathPredicate.getArgs().put(Constants.KEY_0, path);
        route.getPredicates().add(pathPredicate);

        LOGGER.debug("Predicates added to the route: "
                        + "method-pred: {} path-pred: {}",
                methodPredicate.toString(),
                pathPredicate.toString());
    }

    /**
     * Sets the request body filters for the route.
     *
     * <p>This method checks if the request body is multipart/form-data or application/json,
     * and sets the appropriate filters for the route.
     *
     * @param operation the OpenAPI Operation object
     * @param route     the RouteDefinition object
     * @throws JsonProcessingException if there is an error in JSON processing
     */
    private void setRequestBodyFilters(Operation operation, RouteDefinition route) throws JsonProcessingException {
        RequestBody request = operation.getRequestBody();
        if (request != null) {
            MediaType mt = (request.getContent().get(MULTIPART_FORM_DATA) != null)
                    ? (request.getContent().get(MULTIPART_FORM_DATA)) :
                    (request.getContent().get("application/json"));
            LOGGER.debug("Media-Type extracted from schema: {}", mt);
            if (mt.getSchema().get$ref() != null) {
                String schemaName = mt.getSchema().get$ref().replace("#/components/schemas/", "");
                LOGGER.debug(REQUEST_BODY_SCHEMA, schemaName);
                if (!schemaName.isBlank() && components != null
                        && components.getSchemas() != null
                        && components.getSchemas().get(schemaName) != null) {
                    String schemaStr = ObjectMapperUtil.getObjectMapper()
                            .writeValueAsString(components.getSchemas().get(schemaName));
                    LOGGER.info(REQUEST_BODY_SCHEMA, schemaStr);
                    route.getMetadata().put(Constants.SCHEMA, schemaStr);
                    addRequestBodyFilters(route);
                }
            }
        }
    }

    /**
     * Adds request body filters to the route.
     *
     * <p>This method adds filters for caching the request body and validating the request body.
     *
     * @param route the RouteDefinition object
     */
    private void addRequestBodyFilters(RouteDefinition route) {
        FilterDefinition filter = new FilterDefinition();
        filter.setName(Security.Fields.CacheRequestBody);
        filter.getArgs().put(Constants.BODY_CLASS, Constants.STRING);
        route.getFilters().add(filter);

        filter = new FilterDefinition();
        filter.setName(Security.Fields.RequestBodyValidator);
        route.getFilters().add(filter);
    }

    /**
     * Sets the caching filters for the route.
     *
     * <p>This method checks if the operation has caching extensions and sets the appropriate filters for caching.
     *
     * @param method    the HTTP method (GET, POST, etc.)
     * @param operation the OpenAPI Operation object
     * @param route     the RouteDefinition object
     */
    private void setCachingFilters(HttpMethod method, Operation operation, RouteDefinition route) {
        LOGGER.info("operation.getExtensions() {}", operation.getExtensions());
        if (operation.getExtensions() != null && method.name().equalsIgnoreCase("GET")) {
            LOGGER.info("caching extensions --" + operation.getExtensions());
            String extensionCacheSize = (String) operation.getExtensions().get("cacheSize");
            String cacheKey = (String) operation.getExtensions().get("cacheKey");
            String cachettl = (String) operation.getExtensions().get("cacheTll");
            route.setCacheKey(cacheKey);
            route.setCacheSize(extensionCacheSize);
            route.setCacheTtl(cachettl);
            LOGGER.info("Caching required for route {} ", route.getId());
        }
    }

    /**
     * Sets the security filters for the route.
     *
     * <p>This method checks if the operation has security requirements and sets the appropriate filters for security.
     *
     * @param operation the OpenAPI Operation object
     * @param route     the RouteDefinition object
     */
    private void setSecurityFilters(Operation operation, RouteDefinition route) {
        if (operation.getSecurity() != null && !operation.getSecurity().isEmpty()) {
            for (SecurityRequirement sr : operation.getSecurity()) {
                FilterDefinition filter = new FilterDefinition();
                sr.forEach((name, scopes) -> {
                    filter.setName(name);
                    if (scopes != null && !scopes.isEmpty()) {
                        enabledOverrideScope(route, scopes);
                        filter.getArgs().put(Constants.SCOPE, String.join(",", scopes));
                        LOGGER.info("Final scope config: " + scopes);
                    }
                });
                route.getFilters().add(filter);
                LOGGER.info("Token Validation Filter: " + filter);
            }
        }
    }

    /**
     * Enables override scope for the route.
     *
     * <p>This method checks if the override scope is enabled and adds the scopes to the route.
     *
     * @param route  the RouteDefinition object
     * @param scopes the list of scopes
     */
    private void enabledOverrideScope(RouteDefinition route, List<String> scopes) {
        String routeId = route.getId();
        if (isOverrideScopeEnabled && scopesMap != null
                && (scopesMap.get(routeId) != null || scopesMap.get(routeId.toLowerCase()) != null)) {
            List<String> scopesList = scopesMap.get(routeId) != null
                    ? scopesMap.get(routeId) : scopesMap.get(routeId.toLowerCase());
            scopes = Stream.concat(scopes.stream(), scopesList.stream()).distinct().toList();
            LOGGER.info("Extended Scope Config:" + scopes);
        }
    }

    /**
     * Sets the rewrite path filter for the route.
     *
     * <p>This method checks if the context path is not null or blank and sets the rewrite path filter for the route.
     *
     * @param route the RouteDefinition object
     */
    private void setRewritePathFilter(RouteDefinition route) {
        if (this.contextPath != null && !this.contextPath.isBlank()) {
            FilterDefinition filter = new FilterDefinition();
            filter.setName(Constants.REWRITE_PATH_FILTER);
            filter.getArgs().put(Constants.REGEX, Constants.REGEX_SEGMENT);
            filter.getArgs().put(Constants.REPLACEMENT, this.contextPath + Constants.REPLACEMENT_REGEX);
            route.getFilters().add(filter);
            LOGGER.info("Rewrite Path Filter: " + filter);
        }
    }

    /**
     * Prepares the API documentation routes.
     *
     * <p>This method registers the API documentation routes with the API registry service.
     * It creates a RouteDefinition for each grouped OpenAPI and sets the appropriate filters and predicates.
     */
    private void prepareApiDocRoutes() {
        LOGGER.info("Register api-doc routes with api-registry service...");
        if (groupedOpenApis != null) {
            groupedOpenApis.forEach(group -> {
                RouteDefinition route = new RouteDefinition();
                // Set Route Details
                route.setId(group.getGroup() + "-docs");
                route.setService(appName);
                route.setUri(serviceUrl);
                route.setContextPath(this.contextPath);

                // Set Route Predicates
                PredicateDefinition methodPred = new PredicateDefinition();
                methodPred.setName(Constants.METHOD);
                methodPred.getArgs().put(Constants.KEY_0, HttpMethod.GET.name());
                route.getPredicates().add(methodPred);
                PredicateDefinition pathPred = new PredicateDefinition();
                pathPred.setName(Constants.PATH);
                pathPred.getArgs().put(Constants.KEY_0, "/v3/api-docs/" + group.getGroup() + "/**");
                route.getPredicates().add(pathPred);
                // set Filters
                final LinkedList<FilterDefinition> filters = new LinkedList<>();
                if (this.contextPath != null && !this.contextPath.isBlank()) {
                    // Set Rewrite Filter to append context-path at the beginning
                    FilterDefinition filter = new FilterDefinition();
                    filter.setName(Constants.REWRITE_PATH_FILTER);
                    filter.getArgs().put(Constants.REGEX, Constants.REGEX_SEGMENT);
                    filter.getArgs().put(Constants.REPLACEMENT, this.contextPath + Constants.REPLACEMENT_REGEX);
                    filters.add(filter);
                }
                if (!filters.isEmpty()) {
                    route.setFilters(filters);
                }
                // Set documentation as true
                route.setApiDocs(true);
                LOGGER.debug("API-Doc Route : {}", route);
                apiRoutes.add(route);
            });
        }
    }

    /**
     * Dummy implementation, not required in the current flow.
     */
    @Override
    protected String getServerUrl(HttpServletRequest request, String apiDocsUrl) {
        return "";
    }

    /**
     * Sets the header metadata for the given route.
     *
     * <p>This method extracts header parameters from the OpenAPI operation and adds them to the route's metadata.
     * It logs the operation ID and the parameters found. If there are header parameters, it creates a list of maps
     * containing the header name and whether the header is mandatory, and adds this list to the route's metadata.
     *
     * @param operation the OpenAPI Operation object
     * @param route     the RouteDefinition object
     * @throws JsonProcessingException if there is an error in JSON processing
     */
    private void setHeaderMetadata(Operation operation, RouteDefinition route) throws JsonProcessingException {
        if (CollectionUtils.isEmpty(operation.getParameters())) {
            LOGGER.debug("No parameters found in the operation: {}", operation.getOperationId());
            return;
        }
        LOGGER.debug("routeId: {}, parameters: {}", route.getId(), operation.getParameters());
        List<Parameter> headers = operation.getParameters().stream()
                .filter(p -> p.getIn().equalsIgnoreCase("header"))
                .toList();
        if (!headers.isEmpty()) {
            List<Map<String, Object>> headerList = new ArrayList<>();
            for (Parameter header : headers) {
                LOGGER.debug("adding header metadata to route {} definition header name: {}, is header mandatory : {}",
                        route.getId(), header.getName(), header.getRequired());
                headerList.add(Map.of("name", header.getName(), "required",
                        header.getRequired() != null && header.getRequired()));
            }
            route.getMetadata().put("headers", ObjectMapperUtil.getObjectMapper().writeValueAsString(headerList));
        }
    }
}
