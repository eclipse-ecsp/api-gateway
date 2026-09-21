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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.eclipse.ecsp.gateway.model.Response;
import org.eclipse.ecsp.gateway.plugins.spi.AdditionalClaimValidator;
import org.eclipse.ecsp.gateway.plugins.spi.DefaultAdditionalClaimValidator;
import org.eclipse.ecsp.gateway.plugins.spi.DefaultGatewayErrorResponseResolver;
import org.eclipse.ecsp.gateway.plugins.spi.DefaultScopeValidator;
import org.eclipse.ecsp.gateway.plugins.spi.DefaultSignatureVerifier;
import org.eclipse.ecsp.gateway.plugins.spi.DefaultTokenClaimHeaderMapper;
import org.eclipse.ecsp.gateway.plugins.spi.DefaultTokenClaimValidator;
import org.eclipse.ecsp.gateway.plugins.spi.DefaultTokenDecoder;
import org.eclipse.ecsp.gateway.plugins.spi.DefaultTokenParser;
import org.eclipse.ecsp.gateway.plugins.spi.GatewayErrorResponseResolver;
import org.eclipse.ecsp.gateway.plugins.spi.ScopeValidator;
import org.eclipse.ecsp.gateway.plugins.spi.SignatureVerifier;
import org.eclipse.ecsp.gateway.plugins.spi.TokenClaimHeaderMapper;
import org.eclipse.ecsp.gateway.plugins.spi.TokenClaimValidator;
import org.eclipse.ecsp.gateway.plugins.spi.TokenDecoder;
import org.eclipse.ecsp.gateway.plugins.spi.TokenParser;
import org.eclipse.ecsp.gateway.utils.ObjectMapperUtil;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.http.codec.CodecCustomizer;
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
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
     * This is a work around override in order to support.
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
     * Exposes the shared ObjectMapper instance as a Spring-managed bean.
     * Required because Spring Boot 4.x no longer auto-configures ObjectMapper
     * via the transitive dependency chain for WebFlux applications.
     *
     * @return configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return ObjectMapperUtil.getObjectMapper();
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

    /**
     * Configure RetryTemplate with exponential backoff.
     *
     * @param properties JwtProperties containing retry configuration.
     * @return configured RetryTemplate
     */
    @Bean("jwkRefreshRetryTemplate")
    public RetryTemplate jwkRefreshRetryTemplate(JwtProperties properties) {
        // Configure exponential backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.getRetry().getInitialIntervalMs());
        backOffPolicy.setMultiplier(properties.getRetry().getMultiplier());
        backOffPolicy.setMaxInterval(properties.getRetry().getMaxIntervalMs());
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Configure retry policy with max attempts
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(RestClientException.class, true);
        retryableExceptions.put(Exception.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                properties.getRetry().getMaxAttempts(),
                retryableExceptions
        );
        retryTemplate.setRetryPolicy(retryPolicy);

        LOGGER.info("Jwks Refresh RetryTemplate configured: " 
            + "maxAttempts={}, initialInterval={}ms, multiplier={}, maxInterval={}ms",
                properties.getRetry().getMaxAttempts(),
                properties.getRetry().getInitialIntervalMs(),
                properties.getRetry().getMultiplier(),
                properties.getRetry().getMaxIntervalMs());

        return retryTemplate;
    }

    /**
     * Configure RetryTemplate with exponential backoff for routes refresh.
     *
     * @param properties RouteRefreshProperties containing retry configuration.
     * @return configured RetryTemplate
     */
    @Bean("routesRefreshRetryTemplate")
    public RetryTemplate routesRefreshRetryTemplate(RouteRefreshProperties properties) {
        // Configure exponential backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.getRetry().getInitialIntervalMs());
        backOffPolicy.setMultiplier(properties.getRetry().getMultiplier());
        backOffPolicy.setMaxInterval(properties.getRetry().getMaxIntervalMs());
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Configure retry policy with max attempts
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(RestClientException.class, true);
        retryableExceptions.put(Exception.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                properties.getRetry().getMaxAttempts(),
                retryableExceptions
        );
        retryTemplate.setRetryPolicy(retryPolicy);

        LOGGER.info("Routes Refresh RetryTemplate configured: "
            + "maxAttempts={}, initialInterval={}ms, multiplier={}, maxInterval={}ms",
                properties.getRetry().getMaxAttempts(),
                properties.getRetry().getInitialIntervalMs(),
                properties.getRetry().getMultiplier(),
                properties.getRetry().getMaxIntervalMs());

        return retryTemplate;
    }

    //@Bean
    public CodecCustomizer codecCustomizer(@Value("${spring.codec.max-in-memory-size:1MB}") String maxInMemorySize) {
        int maxInMemorySizeBytes = (int) DataSize.parse(maxInMemorySize).toBytes();
        return configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySizeBytes);
    }

    // -------------------------------------------------------------------------
    // SPI default bean registrations — each is skipped when a custom bean exists
    // -------------------------------------------------------------------------

    /**
     * Default {@link TokenParser} bean — extracts the bearer token from the
     * {@code Authorization} header. Overridden by any user-defined {@link TokenParser} bean.
     *
     * @return default token parser instance
     */
    @Bean
    @ConditionalOnMissingBean(TokenParser.class)
    public TokenParser defaultTokenParser() {
        LOGGER.info("Registering DefaultTokenParser (no custom TokenParser bean found).");
        return new DefaultTokenParser();
    }

    /**
     * Default {@link TokenDecoder} bean — decodes JWT structure via Nimbus without
     * signature verification. Overridden by any user-defined {@link TokenDecoder} bean.
     *
     * @return default token decoder instance
     */
    @Bean
    @ConditionalOnMissingBean(TokenDecoder.class)
    public TokenDecoder defaultTokenDecoder() {
        LOGGER.info("Registering DefaultTokenDecoder (no custom TokenDecoder bean found).");
        return new DefaultTokenDecoder();
    }

    /**
     * Default {@link SignatureVerifier} bean — verifies JWT signatures via JJWT.
     * Overridden by any user-defined {@link SignatureVerifier} bean.
     *
     * @return default signature verifier instance
     */
    @Bean
    @ConditionalOnMissingBean(SignatureVerifier.class)
    public SignatureVerifier defaultSignatureVerifier() {
        LOGGER.info("Registering DefaultSignatureVerifier (no custom SignatureVerifier bean found).");
        return new DefaultSignatureVerifier();
    }

    /**
     * Default {@link TokenClaimValidator} bean — validates configured required/regex claim rules.
     * Overridden by any user-defined {@link TokenClaimValidator} bean.
     *
     * @return default token claim validator instance
     */
    @Bean
    @ConditionalOnMissingBean(TokenClaimValidator.class)
    public TokenClaimValidator defaultTokenClaimValidator() {
        LOGGER.info("Registering DefaultTokenClaimValidator (no custom TokenClaimValidator bean found).");
        return new DefaultTokenClaimValidator();
    }

    /**
     * Default {@link AdditionalClaimValidator} no-op bean — performs no additional claim checks.
     * Override by registering a custom {@link AdditionalClaimValidator} bean to add
     * business-specific programmatic claim validation.
     *
     * @return no-op additional claim validator instance
     */
    @Bean
    @ConditionalOnMissingBean(AdditionalClaimValidator.class)
    public AdditionalClaimValidator defaultAdditionalClaimValidator() {
        LOGGER.info("Registering DefaultAdditionalClaimValidator "
                + "(no-op — no custom AdditionalClaimValidator bean found).");
        return new DefaultAdditionalClaimValidator();
    }

    /**
     * Default {@link ScopeValidator} bean — validates token scopes against route-required scopes.
     * Overridden by any user-defined {@link ScopeValidator} bean.
     *
     * @return default scope validator instance
     */
    @Bean
    @ConditionalOnMissingBean(ScopeValidator.class)
    public ScopeValidator defaultScopeValidator() {
        LOGGER.info("Registering DefaultScopeValidator (no custom ScopeValidator bean found).");
        return new DefaultScopeValidator();
    }

    /**
     * Default {@link TokenClaimHeaderMapper} bean — maps JWT claims to downstream HTTP headers.
     * Overridden by any user-defined {@link TokenClaimHeaderMapper} bean.
     *
     * @return default token claim header mapper instance
     */
    @Bean
    @ConditionalOnMissingBean(TokenClaimHeaderMapper.class)
    public TokenClaimHeaderMapper defaultTokenClaimHeaderMapper() {
        LOGGER.info("Registering DefaultTokenClaimHeaderMapper (no custom TokenClaimHeaderMapper bean found).");
        return new DefaultTokenClaimHeaderMapper();
    }

    /**
     * Default {@link GatewayErrorResponseResolver} bean — produces the standard
     * {@code {"message":"...", "code":"..."}} error response shape.
     * Overridden by any user-defined {@link GatewayErrorResponseResolver} bean.
     *
     * @return default error response builder instance
     */
    @Bean
    @ConditionalOnMissingBean(GatewayErrorResponseResolver.class)
    public GatewayErrorResponseResolver defaultGatewayErrorResponseResolver() {
        LOGGER.info("Registering DefaultGatewayErrorResponseResolver "
                + "(no custom GatewayErrorResponseResolver bean found).");
        return new DefaultGatewayErrorResponseResolver();
    }

}
