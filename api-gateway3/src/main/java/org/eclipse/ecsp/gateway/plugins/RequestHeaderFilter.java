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

package org.eclipse.ecsp.gateway.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.plugins.RequestHeaderFilter.Config;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.ObjectMapperUtil;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.config.conditional.ConditionalOnEnabledFilter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.HtmlUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gateway filter to validate request headers.
 *
 * @author Abhishek Kumar
 */
@Setter
@Component
@ConfigurationProperties("api.gateway.request-header-filter")
@ConditionalOnEnabledFilter(RequestHeaderFilter.class)
public class RequestHeaderFilter extends AbstractGatewayFilterFactory<Config> {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RequestHeaderFilter.class);
    private static final String HEADERS = "headers";
    private List<GlobalHeaderConfig> globalHeaders;

    /**
     * Constructor to initialize the filter with the configuration class.
     */
    public RequestHeaderFilter() {
        super(Config.class);
    }

    /**
     * Gets the global or default configuration for the filter.
     *
     * @return the configuration
     */
    private static <T> T getOrDefault(T configValue, T defaultValue) {
        return (configValue != null) ? configValue : defaultValue;
    }

    /**
     * Includes headers in the request if they are missing.
     *
     * @param config       the configuration for the filter
     * @param exchange     the server web exchange
     * @param uri          the request URI
     * @param allowHeaders the set of allowed headers
     * @return the modified server web exchange
     */
    private static ServerWebExchange includeHeaderIfMissing(Config config,
                                                            ServerWebExchange exchange,
                                                            String uri,
                                                            Set<String> allowHeaders) {
        LOGGER.debug("Starting header append check for URI: {}, headers to check : {}",
                uri,
                config.getAppendHeadersIfMissing());
        Map<String, String> requestHeaders = exchange.getRequest().getHeaders().toSingleValueMap();
        Builder builder = exchange.getRequest().mutate();
        config.getAppendHeadersIfMissing().forEach(header -> {
            if (getHeader(header, requestHeaders).isEmpty()) {
                String headerValue = UUID.randomUUID().toString();
                LOGGER.debug("Adding missing header {} : {} to the request {}", header, headerValue, uri);
                builder.header(header, headerValue);
                allowHeaders.add(header);
            }
        });
        exchange = exchange.mutate().request(builder.build()).build();
        LOGGER.debug("Append check completed for URI: {}, headers added : {}", uri, config.getAppendHeadersIfMissing());
        return exchange;
    }

    /**
     * Validates if a mandatory header is present in the request.
     *
     * @param headerConfig   the global header configuration
     * @param requestHeaders the request headers
     * @param uri            the request URI
     * @return true if the header is valid, false otherwise
     */
    private static boolean validateMandatory(GlobalHeaderConfig headerConfig,
                                             Map<String, String> requestHeaders,
                                             String uri) {
        return validateMandatory(headerConfig.getName(), headerConfig.isRequired(), requestHeaders, uri);
    }

    /**
     * Validates if a mandatory header is present in the request.
     *
     * @param name           the header name
     * @param isRequired     whether the header is required
     * @param requestHeaders the request headers
     * @param uri            the request URI
     * @return true if the header is valid, false otherwise
     */
    private static boolean validateMandatory(String name,
                                             boolean isRequired,
                                             Map<String, String> requestHeaders,
                                             String uri) {
        if (isRequired && getHeader(name, requestHeaders).isEmpty()) {
            LOGGER.error("required header {} is missing in the request {}", name, uri);
            throw new ApiGatewayException(HttpStatus.BAD_REQUEST,
                    "api.gateway.error.header.invalid",
                    "Missing required header " + name + " in the request");
        }
        return true;
    }

    /**
     * Checks if a header is present in the set of headers.
     *
     * @param name    the header name
     * @param headers the set of headers
     * @return true if the header is present, false otherwise
     */
    private static boolean containsHeader(String name, Set<String> headers) {
        return headers.stream().anyMatch(h -> h.equalsIgnoreCase(name));
    }

    /**
     * Retrieves a header from the request headers.
     *
     * @param name           the header name
     * @param requestHeaders the request headers
     * @return an optional containing the header if present, empty otherwise
     */
    private static Optional<String> getHeader(String name, Map<String, String> requestHeaders) {
        return requestHeaders.keySet().stream().filter(h -> h.equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Validates if a header value matches the configured regex.
     *
     * @param globalHeaderConfig the global header configuration
     * @param requestHeaders     the request headers
     * @param uri                the request URI
     * @return true if the header value matches the regex, false otherwise
     */
    private static boolean validateRegex(GlobalHeaderConfig globalHeaderConfig,
                                         Map<String, String> requestHeaders,
                                         String uri) {
        Optional<String> reqHeader = getHeader(globalHeaderConfig.getName(), requestHeaders);
        if (reqHeader.isPresent() && StringUtils.isNotEmpty(globalHeaderConfig.getRegex())) {
            String headerValue = requestHeaders.get(reqHeader.get());
            if (!headerValue.matches(globalHeaderConfig.getRegex())) {
                LOGGER.error("{} header value is not matching the regex {} for the request {}",
                        globalHeaderConfig.getName(),
                        globalHeaderConfig.getRegex(),
                        uri);
                throw new ApiGatewayException(HttpStatus.BAD_REQUEST,
                        "api.gateway.error.header.invalid",
                        "Invalid " + globalHeaderConfig.getName() + " header value: " + headerValue);
            }
        }
        return true;
    }

    /**
     * Initializes the filter and logs the global headers.
     */
    @PostConstruct
    public void init() {
        LOGGER.info("RequestHeaderValidatorGatewayFilterFactory initialized with global headers: {}",
                this.globalHeaders);
    }

    /**
     * Applies the filter to validate request headers.
     *
     * @param config the configuration for the filter
     * @return the gateway filter
     */
    @Override
    public GatewayFilter apply(Config config) {
        List<GlobalHeaderConfig> globalHeaderConfigs = getOrDefault(this.globalHeaders, config.getGlobalHeaders());
        LOGGER.debug("globalHeaderConfigs: {}", globalHeaderConfigs);
        return new OrderedGatewayFilter((exchange, chain) -> {
            String uri = exchange.getRequest().getURI().getPath();

            // Skip validation for specific APIs
            if (!CollectionUtils.isEmpty(config.skipValidationForApis) && shouldSkipValidation(config, uri)) {
                LOGGER.debug("Skipping header validation for the request {}", uri);
                return chain.filter(exchange);
            }
            LOGGER.debug("Request Header validation started for {}", uri);

            //include headers if missing in the request
            Set<String> allowedHeaders = new HashSet<>(config.getAllowHeaders());
            if (!CollectionUtils.isEmpty(config.getAppendHeadersIfMissing())) {
                exchange = includeHeaderIfMissing(config, exchange, uri, allowedHeaders);
            }

            Map<String, String> requestHeaders = exchange.getRequest().getHeaders().toSingleValueMap();
            // Validate global headers
            if (!CollectionUtils.isEmpty(globalHeaderConfigs)) {
                Set<String> validatedGlobalHeaders = validateGlobalHeaders(globalHeaderConfigs, requestHeaders, uri);
                allowedHeaders.addAll(validatedGlobalHeaders);
            }


            // Validate route headers
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if (route != null && route.getMetadata() != null && route.getMetadata().get(HEADERS) != null) {
                LOGGER.debug("validation route headers started for {}", uri);
                validateRouteHeaders(route, requestHeaders, allowedHeaders, uri);
                LOGGER.debug("validation route headers completed for {}", uri);
            }

            // Remove unknown headers if configured
            if (config.removeUnknownHeaders && !CollectionUtils.isEmpty(allowedHeaders)) {
                LOGGER.debug("removing unknown route headers for {}", uri);
                exchange = removeUnknownHeaders(exchange, requestHeaders, allowedHeaders);
            }

            //sanitize all the request headers
            exchange = sanitizeHeaders(exchange);

            LOGGER.debug("Request Header validation completed for {}", uri);
            return chain.filter(exchange);
        }, GatewayConstants.REQUEST_HEADER_FILTER_ORDER);
    }

    /**
     * Checks if the validation should be skipped for the given URI.
     *
     * @param config the configuration for the filter
     * @param uri    the request URI
     * @return true if validation should be skipped, false otherwise
     */
    private boolean shouldSkipValidation(Config config, String uri) {
        return config.skipValidationForApis.stream().anyMatch(pattern -> uri.matches(pattern.replace("**", ".*")));
    }

    /**
     * Validates the global headers based on the configuration.
     *
     * @param globalHeaderConfigs the configuration for the filter
     * @param requestHeaders      the request headers
     * @param uri                 the request URI
     * @return a set of allowed headers
     */
    private Set<String> validateGlobalHeaders(List<GlobalHeaderConfig> globalHeaderConfigs,
                                              Map<String, String> requestHeaders,
                                              String uri) {
        return globalHeaderConfigs.stream()
                .filter(h -> validateMandatory(h, requestHeaders, uri))
                .filter(h -> validateRegex(h, requestHeaders, uri))
                .map(GlobalHeaderConfig::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Validates the route-specific headers.
     *
     * @param route          the route information
     * @param requestHeaders the request headers
     * @param allowedHeaders the set of allowed headers
     * @param uri            the request URI
     */
    private void validateRouteHeaders(Route route,
                                      Map<String, String> requestHeaders,
                                      Set<String> allowedHeaders,
                                      String uri) {
        if (!CollectionUtils.isEmpty(route.getMetadata()) && route.getMetadata().get(HEADERS) != null) {
            LOGGER.debug("Route Header validation started for {}", uri);
            try {
                List<Map<String, Object>> routeHeaders = ObjectMapperUtil.getObjectMapper()
                        .readValue((String) route.getMetadata().get(HEADERS),
                                new TypeReference<List<Map<String, Object>>>() {
                                });

                routeHeaders.stream()
                        .filter(headerMetadata -> headerMetadata.get("name") != null)
                        .forEach(h -> {
                            String headerName = (String) h.get("name");
                            if (StringUtils.isNotEmpty(headerName)) {
                                validateMandatory(headerName,
                                        (boolean) h.getOrDefault("required", false),
                                        requestHeaders,
                                        uri);
                                allowedHeaders.add(headerName);
                            }
                        });
                LOGGER.debug("Route Header validation completed for {}", uri);
            } catch (JsonProcessingException e) {
                LOGGER.error("Error while parsing route headers for the request {} "
                        + "skipping route header validation, "
                        + "please check route {} metadata headers, error: {}", uri, route.getId(), e);
            }

        }
    }

    /**
     * Removes unknown headers from the request if configured.
     *
     * @param exchange       the server web exchange
     * @param requestHeaders the request headers
     * @param includeHeaders the set of headers to include
     * @return the modified server web exchange
     */
    ServerWebExchange removeUnknownHeaders(ServerWebExchange exchange,
                                           Map<String, String> requestHeaders,
                                           Set<String> includeHeaders) {
        String uri = exchange.getRequest().getURI().getPath();
        Builder builder = exchange.getRequest().mutate();
        Set<String> unknownHeaders = requestHeaders.keySet().stream()
                .filter(h -> !containsHeader(h, includeHeaders))
                .collect(Collectors.toSet());
        LOGGER.debug("Unknown headers {} to be removed from request: {}", unknownHeaders, uri);
        builder.headers(httpHeaders -> unknownHeaders.forEach(httpHeaders::remove));
        ServerHttpRequest request = builder.build();
        LOGGER.debug("Headers {} after removing unknown headers from request {}", request.getHeaders(), uri);
        return exchange.mutate().request(request).build();
    }

    /**
     * Sanitize all the headers in the request.
     *
     * @param exchange the server web exchange
     * @return updated exchange object
     */
    private ServerWebExchange sanitizeHeaders(ServerWebExchange exchange) {
        Builder requestHeaders = exchange.getRequest().mutate();
        exchange.getRequest().getHeaders()
                .forEach((headerName, headerValue) ->
                        requestHeaders.header(headerName,
                                headerValue.stream().map(this::sanitizeValue).toArray(String[]::new)));
        return exchange.mutate().request(requestHeaders.build()).build();
    }

    /**
     * Sanitize given string.
     *
     * @param value original value
     * @return sanitized value
     */
    private String sanitizeValue(String value) {
        return HtmlUtils.htmlEscape(value);
    }

    /**
     * Configuration class for the RequestHeaderValidatorFilter.
     */
    @Getter
    @Setter
    @ToString
    public static class Config {
        private boolean removeUnknownHeaders = false;
        private Set<String> allowHeaders;
        private Set<String> skipValidationForApis;
        private Set<String> appendHeadersIfMissing;
        private List<GlobalHeaderConfig> globalHeaders;
    }

    /**
     * Configuration class for global headers.
     */
    @Setter
    @Getter
    @ToString
    static class GlobalHeaderConfig {
        private String name;
        private boolean required = false;
        private String regex;
    }
}