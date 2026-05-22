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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.webclient;

import io.netty.channel.ChannelOption;
import org.eclipse.ecsp.config.WebClientConfig;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link WebClientConfig}.
 */
@ExtendWith(MockitoExtension.class)
class WebClientConfigTest {

    private WebClientConfig webClientConfig;

    @BeforeEach
    void setUp() {
        webClientConfig = new WebClientConfig();
    }

    @Test
    void shouldCreateWebClientTokenFilter() {
        ValidationConfigProperties config = new ValidationConfigProperties();
        WebClientTokenFilter filter = webClientConfig.webClientTokenFilter(config);
        Assertions.assertNotNull(filter);
    }

    @Test
    void shouldConfigureConnectTimeoutWhenSpecified() {
        WebClient.Builder builder = webClientConfig.webClientBuilder(Optional.empty(), 3000L, 7000L);
        ReactorClientHttpConnector connector = (ReactorClientHttpConnector)
                ReflectionTestUtils.getField(builder, "connector");
        Assertions.assertNotNull(connector);
        HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
        Integer connectTimeout = (Integer) httpClient.configuration().options()
                .get(ChannelOption.CONNECT_TIMEOUT_MILLIS);
        Assertions.assertEquals(3000, connectTimeout);
    }

    @Test
    void shouldConfigureReadTimeoutWhenSpecified() {
        WebClient.Builder builder = webClientConfig.webClientBuilder(Optional.empty(), 3000L, 7000L);
        ReactorClientHttpConnector connector = (ReactorClientHttpConnector)
                ReflectionTestUtils.getField(builder, "connector");
        Assertions.assertNotNull(connector);
        HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
        Duration responseTimeout = httpClient.configuration().responseTimeout();
        Assertions.assertEquals(Duration.ofMillis(7000L), responseTimeout);
    }

    @Test
    void shouldUseDefaultTimeoutsWhenNotSpecified() {
        WebClient.Builder builder = webClientConfig.webClientBuilder(Optional.empty(), 5000L, 5000L);
        ReactorClientHttpConnector connector = (ReactorClientHttpConnector)
                ReflectionTestUtils.getField(builder, "connector");
        Assertions.assertNotNull(connector);
        HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
        Integer connectTimeout = (Integer) httpClient.configuration().options()
                .get(ChannelOption.CONNECT_TIMEOUT_MILLIS);
        Duration responseTimeout = httpClient.configuration().responseTimeout();
        Assertions.assertEquals(5000, connectTimeout);
        Assertions.assertEquals(Duration.ofMillis(5000L), responseTimeout);
    }

    @Test
    void shouldAddTokenFilterToBuilderWhenPresent() {
        ValidationConfigProperties config = new ValidationConfigProperties();
        WebClientTokenFilter filter = new WebClientTokenFilter(config);
        WebClient.Builder builder = webClientConfig.webClientBuilder(Optional.of(filter), 5000L, 5000L);
        @SuppressWarnings("unchecked")
        List<ExchangeFilterFunction> filters =
                (List<ExchangeFilterFunction>) ReflectionTestUtils.getField(builder, "filters");
        Assertions.assertNotNull(filters);
        Assertions.assertTrue(filters.contains(filter));
    }

    @Test
    void shouldNotAddFilterToBuilderWhenAbsent() {
        WebClient.Builder builder = webClientConfig.webClientBuilder(Optional.empty(), 5000L, 5000L);
        @SuppressWarnings("unchecked")
        List<ExchangeFilterFunction> filters =
                (List<ExchangeFilterFunction>) ReflectionTestUtils.getField(builder, "filters");
        Assertions.assertTrue(filters == null || filters.isEmpty());
    }

    @Test
    void shouldCreateFallbackWebClient() {
        WebClient.Builder builder = webClientConfig.webClientBuilder(Optional.empty(), 5000L, 5000L);
        WebClient webClient = webClientConfig.webClient(builder);
        Assertions.assertNotNull(webClient);
    }

}
