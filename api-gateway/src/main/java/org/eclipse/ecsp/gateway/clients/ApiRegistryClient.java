package org.eclipse.ecsp.gateway.clients;

import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.service.RouteUtils;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Client for interacting with the API Registry to routes.
 * This client fetches route definitions from the API registry service.
 * It uses WebClient to make HTTP requests and retrieve route definitions.
 * The base URL for the API registry is configurable via application properties.
 *
 * @author Abhishek Kumar
 */
@Component
@ConditionalOnBooleanProperty(name = "api.registry.enabled")
public class ApiRegistryClient {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiRegistryClient.class);
    @Value("${api.registry.routes-endpoint:/api/v1/routes}")
    private String routesEndpoint;
    private final WebClient webClient;
    private final RouteUtils routeUtils;

    /**
     * Constructor for ApiRegistryClient.
     *
     * @param baseUrl the base URL of the API registry
     * @param webClientBuilder the WebClient builder to create the WebClient instance
     * @param routeUtils routeUtils for handling routes
     */
    public ApiRegistryClient(@Value("${api.registry.base-url:http://localhost:7000}") String baseUrl,
                             WebClient.Builder webClientBuilder,
                             RouteUtils routeUtils) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.routeUtils = routeUtils;
        LOGGER.info("ApiRegistryClient initialized with base URL: {}", baseUrl);
    }

    /**
     * Fetches routes from the API registry.
     *
     * @return list of routes from registry
     */
    public Flux<IgniteRouteDefinition> getRoutes() {
        LOGGER.debug("Loading API Routes");
        // @formatter:off
        return this.webClient.get().uri(routesEndpoint)
                .accept(MediaType.APPLICATION_JSON)
                .header(GatewayConstants.USER_ID, "1")
                .header(GatewayConstants.SCOPE, "SYSTEM_READ")
                .retrieve()
                .bodyToFlux(IgniteRouteDefinition.class)
                .doOnError(throwable -> LOGGER.error("Error while fetching routes from api-registry: {}",
                        throwable.getMessage()))
                .onErrorReturn(routeUtils.getDummyRoute());
        // @formatter:on
    }
}
