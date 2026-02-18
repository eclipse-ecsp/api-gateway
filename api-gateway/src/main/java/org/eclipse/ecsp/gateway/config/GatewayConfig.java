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

package org.eclipse.ecsp.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.eclipse.ecsp.gateway.model.Response;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.config.HttpClientSslConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import javax.net.ssl.SSLException;
import java.time.Duration;

/**
 * GatewayConfig Configration class.
 */
@Configuration
public class GatewayConfig {

    /**
     * Creates LOGGER object.
     */
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(GatewayConfig.class);
    /**
     * property defines the sliding widnow_size for circuit breaker.
     */
    public static final Integer WINDOW_SIZE = 20;

    /**
     * defines the number of permitted calls in Half Open state.
     */
    public static final Integer PERMITTED_CALLS = 5;
    /**
     * Defines the max failure threshold rate.
     */
    public static final Integer FAILURE_RATE_THRESHOLD = 50;
    /**
     * Defines the wait duration for request in open state.
     */
    public static final Integer WAIT_DURATION = 5;
    /**
     * Defines the time out duration.
     */
    public static final Integer TIMEOUT_DURATION = 5;
    /**
     * fallback uri "/fallback/**".
     */
    public static final String FALLBACK_URI = "/fallback/**";

    /**
     * This is a work around override in order to support
     * HTTP2 Procotol on netty.
     * Refer - <a href="https://github.com/spring-cloud/spring-cloud-gateway/issues/2580">...</a>
     * By doing this, we can still use HTTPS on the gateway and HTTP
     * communication behind the gateway even if HTTP2 is active.
     *
     * @param httpClientProperties httpClientProperties
     * @param serverProperties     serverProperties
     * @param sslBundles           sslBundles
     * @return HttpClientSslConfigurer HttpClientSslConfigurer
     */
    @Bean
    @Primary
    public HttpClientSslConfigurer noopHttpClientSslConfigurer(HttpClientProperties httpClientProperties,
                                                               final ServerProperties serverProperties,
                                                               SslBundles sslBundles) {
        return new HttpClientSslConfigurer(httpClientProperties.getSsl(),
                serverProperties, sslBundles) {
            @Override
            public HttpClient configureSsl(HttpClient client) {
                if (serverProperties.getHttp2().isEnabled()) {
                    HttpClientProperties.Ssl ssl = httpClientProperties.getSsl();
                    return client.secure(sslContextSpec -> {
                        try {
                            SslContextBuilder clientSslCtxt = SslContextBuilder.forClient()
                                    .trustManager(InsecureTrustManagerFactory.INSTANCE);
                            sslContextSpec.sslContext(clientSslCtxt.build()).handshakeTimeout(ssl.getHandshakeTimeout())
                                    .closeNotifyFlushTimeout(ssl.getCloseNotifyFlushTimeout())
                                    .closeNotifyReadTimeout(ssl.getCloseNotifyReadTimeout());
                        } catch (SSLException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                }
                return super.configureSsl(client);
            }
        };
    }


    /**
     * Add @Bean during implementation.
     *
     * @return returns customizer
     */
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(WINDOW_SIZE)
                        .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS)
                        .failureRateThreshold(FAILURE_RATE_THRESHOLD)
                        .waitDurationInOpenState(Duration.ofSeconds(WAIT_DURATION))
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(TIMEOUT_DURATION))
                        .build())
                .build());
    }

    /**
     * routerFunction return the RouterFunction.
     *
     * @return returns routerFunction
     */
    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        return RouterFunctions
                .route(RequestPredicates
                        .GET(FALLBACK_URI), this::handleFallback)
                .andRoute(RequestPredicates
                        .POST(FALLBACK_URI), this::handleFallback)
                .andRoute(RequestPredicates
                        .PUT(FALLBACK_URI), this::handleFallback)
                .andRoute(RequestPredicates
                        .DELETE(FALLBACK_URI), this::handleFallback);
    }

    /**
     * method handles the fallback mechanism.
     *
     * @param request ServerRequest
     * @return returns ServerResponse
     */
    public Mono<ServerResponse> handleFallback(ServerRequest request) {
        Response body = new Response("Service is unavailable. Please try after sometime.");
        LOGGER.warn("service is down for uri: {}", request.path());
        return ServerResponse
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    /**
     * Creates a WebProperties.Resources bean.
     *
     * @return a new instance of WebProperties.Resources
     */
    @Bean
    public WebProperties.Resources resources() {
        return new WebProperties.Resources();
    }

    /**
     * EndpointFilter to restrict exposing endpoints other than defined in exposeEndpoints.
     *
     * @return instance of {@link EndpointFilter}
     */
    @Bean
    @ConditionalOnProperty(name = "api.gateway.metrics.enabled",
            havingValue = "false")
    EndpointFilter<ExposableWebEndpoint> gatewayEndpointFilter() {
        LOGGER.info("Metrics are not enabled, disabling all endpoints.");
        return (endpoint -> false);
    }

}
