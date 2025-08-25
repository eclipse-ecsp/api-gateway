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

import jakarta.annotation.PostConstruct;
import org.eclipse.ecsp.gateway.exceptions.IgniteGlobalExceptionHandler;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.ObjectMapperUtil;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * AccessLog is a Global Filter.
 */
@Component
@ConditionalOnProperty(name = "api.gateway.accesslog.enabled", havingValue = "true", matchIfMissing = true)
public class AccessLog implements GlobalFilter, Ordered {
    /**
     * created a logger instance.
     */
    private static final IgniteLogger LOGGER =
            IgniteLoggerFactory.getLogger(AccessLog.class);
    private static final Predicate<MediaType> TEXT_MIME_TYPES = mediaType -> {
        String type = mediaType.getType();
        String subtype = mediaType.getSubtype();
        return (type.equalsIgnoreCase("text") || type.equalsIgnoreCase("application"))
                && (subtype.equalsIgnoreCase("json") || subtype.equalsIgnoreCase("xml")
                || subtype.equalsIgnoreCase("plain"));
    };
    @Value("${api.gateway.accesslog.enabled:true}")
    private boolean enableAccessLog;
    @Value("${api.gateway.accesslog.request-headers.enabled:true}")
    private boolean logRequestHeaders;
    @Value("${api.gateway.accesslog.request-headers.skip-headers:Authorization,User-Id}")
    private Set<String> skipLoggingRequestHeaders;
    @Value("${api.gateway.accesslog.response-headers.enabled:true}")
    private boolean logResponseHeaders;
    @Value("${api.gateway.accesslog.response-headers.skip-headers:}")
    private Set<String> skipLoggingResponseHeaders;
    @Value("${api.gateway.accesslog.response-body.enabled:true}")
    private boolean logErrorResponse;
    @Value("${api.gateway.accesslog.response-body.skip-for-routes:}")
    private Set<String> skipResponseLoggingForRoutes;

    private Set<String> skipRequestHeadersSet;
    private Set<String> skipResponseHeadersSet;

    /**
     * initialize and prepare AccessLog.
     */
    @PostConstruct
    public void initialize() {
        // Prepare lower-case skip header sets for performance
        skipRequestHeadersSet = (skipLoggingRequestHeaders != null && !skipLoggingRequestHeaders.isEmpty())
                ? skipLoggingRequestHeaders.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet())
                : Set.of();

        skipResponseHeadersSet = (skipLoggingResponseHeaders != null && !skipLoggingResponseHeaders.isEmpty())
                ? skipLoggingResponseHeaders.stream()
                .map(String::trim).map(String::toLowerCase)
                .collect(Collectors.toSet())
                : Set.of();

        skipResponseLoggingForRoutes = (skipResponseLoggingForRoutes != null && !skipResponseLoggingForRoutes.isEmpty())
                ? skipResponseLoggingForRoutes.stream().map(String::trim).collect(Collectors.toSet())
                : Set.of();
    }

    /**
     * Method intercept the request and add the logger.
     *
     * @param exchange ServerWebExchange object
     * @param chain    filter chain object
     * @return returns FilterChain object
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        return chain.filter(exchange.mutate()
                        .response(getServerHttpResponse(exchange, startTime))
                        .build())
                .doOnError(throwable -> accesslog(exchange,
                        startTime,
                        IgniteGlobalExceptionHandler.determineHttpStatus(throwable),
                        ObjectMapperUtil.toJson(IgniteGlobalExceptionHandler.prepareResponse(throwable))
                ));

    }

    /**
     * Method to log the access log.
     *
     * @param exchange      ServerWebExchange object
     * @param startTime     start time of the request
     * @param statusCode    HTTP status code of the response
     * @param responseBody  response body of the request
     */
    private void accesslog(ServerWebExchange exchange, long startTime, HttpStatusCode statusCode, Object responseBody) {
        if (enableAccessLog) {
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            StringBuilder logMsg = new StringBuilder();
            String routeId = route != null ? route.getId() : "UNKNOWN";
            logMsg.append("ACCESS_LOG: Request URL: ")
                    .append(exchange.getRequest().getMethod().name())
                    .append(" - ")
                    .append(exchange.getRequest().getURI())
                    .append(", RouteId: ")
                    .append(routeId)
                    .append(", Response status code:")
                    .append(statusCode)
                    .append(", Response Time: ")
                    .append(System.currentTimeMillis() - startTime).append(" ms");

            if (logRequestHeaders) {
                appendRequestHeaders(exchange, logMsg);
            }
            if (logResponseHeaders) {
                appendResponseHeaders(exchange, logMsg);
            }
            if (logErrorResponse) {
                appendErrorResponse(exchange, statusCode, responseBody, routeId, logMsg);
            }
            LOGGER.info(logMsg.toString());
        }
    }

    private void appendErrorResponse(ServerWebExchange exchange,
                                     HttpStatusCode statusCode,
                                     Object responseBody,
                                     String routeId,
                                     StringBuilder logMsg) {
        boolean isErrorResponse = statusCode != null && statusCode.isError();
        if (isErrorResponse && !skipResponseLoggingForRoutes.contains(routeId)) {
            LOGGER.debug("accesslog: for request {} response is error",
                    exchange.getRequest().getPath());
            if (responseBody != null) {
                logMsg.append(", Response Body: ").append(responseBody);
            } else {
                logMsg.append(", Response Body: [unavailable]");
            }
        }
    }

    private void appendResponseHeaders(ServerWebExchange exchange, StringBuilder logMsg) {
        logMsg.append(", Response Headers: {");
        String headersLog = exchange.getResponse().getHeaders().entrySet().stream()
                .filter(entry -> !skipResponseHeadersSet.contains(entry.getKey().toLowerCase()))
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        logMsg.append(headersLog);
        logMsg.append("}");
    }

    private void appendRequestHeaders(ServerWebExchange exchange, StringBuilder logMsg) {
        logMsg.append(", Request Headers: {");
        String headersLog = exchange.getRequest().getHeaders().entrySet().stream()
                .filter(entry -> !skipRequestHeadersSet.contains(entry.getKey().toLowerCase()))
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        logMsg.append(headersLog);
        logMsg.append("}");
    }

    /**
     * setting execution order of the filter.
     *
     * @return returns order of the filter
     */
    @Override
    public int getOrder() {
        return GatewayConstants.ACCESS_LOG_FILTER_ORDER;
    }

    /**
     * Get the ServerHttpResponse with a decorator to capture the response body.
     *
     * @param exchange   The ServerWebExchange object
     * @param startTime  The start time of the request for logging
     * @return A decorated ServerHttpResponse that captures the response body
     */
    private ServerHttpResponse getServerHttpResponse(ServerWebExchange exchange, Long startTime) {
        LOGGER.debug("request from accesslog#getServerHttpResponse {} ", exchange.getRequest().getPath());
        final ServerHttpResponse originalResponse = exchange.getResponse();
        final DataBufferFactory dataBufferFactory = originalResponse.bufferFactory();
        return new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                HttpStatusCode statusCode = originalResponse.getStatusCode();
                MediaType contentType = originalResponse.getHeaders().getContentType();
                AtomicReference<String> responseBody = new AtomicReference<>();
                if (logErrorResponse && isErrorResponse(statusCode) && isTextBasedResponse(contentType)) {
                    LOGGER.debug("accesslog: for request {} response is error and text",
                            exchange.getRequest().getPath());
                    try {
                        LOGGER.debug("accesslog: for request {} ", exchange.getRequest().getPath());
                        if (body instanceof Flux<? extends DataBuffer> fluxData) {
                            LOGGER.debug("accesslog: response for request {} is Flux",
                                    exchange.getRequest().getPath());
                            return super.writeWith(fluxData.buffer()
                                    .map(dataBuffers -> {
                                        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                        dataBuffers.forEach(dataBuffer -> {
                                            final byte[] responseContent = new byte[dataBuffer.readableByteCount()];
                                            dataBuffer.read(responseContent);
                                            try {
                                                outputStream.write(responseContent);
                                            } catch (IOException e) {
                                                LOGGER.error("accesslog: Error while reading api {} response stream {}",
                                                    exchange.getRequest().getPath(), e);
                                            }
                                        });
                                        accesslog(exchange, startTime, statusCode,
                                            outputStream.toString(StandardCharsets.UTF_8));
                                        responseBody.set(outputStream.toString(StandardCharsets.UTF_8));
                                        return dataBufferFactory.wrap(outputStream.toByteArray());
                                    }));
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error occurred during accessing response body for access logging", e);
                    }
                }

                accesslog(exchange, startTime, statusCode, responseBody.get());
                return super.writeWith(body);
            }
        };
    }

    private boolean isErrorResponse(HttpStatusCode statusCode) {
        return statusCode != null && statusCode.isError();
    }

    private boolean isTextBasedResponse(MediaType contentType) {
        return contentType != null && TEXT_MIME_TYPES.test(contentType);
    }
}
