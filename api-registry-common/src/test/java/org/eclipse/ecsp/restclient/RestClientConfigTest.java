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

package org.eclipse.ecsp.restclient;

import org.eclipse.ecsp.config.RestClientConfig;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link RestClientConfig}.
 */
@ExtendWith(MockitoExtension.class)
class RestClientConfigTest {

    @Mock
    private ObjectProvider<RestClientTokenInterceptor> tokenInterceptorProvider;

    private RestClientConfig restClientConfig;

    @BeforeEach
    void setUp() {
        restClientConfig = new RestClientConfig();
    }

    @Test
    void shouldCreateRestClientTokenInterceptor() {
        ValidationConfigProperties config = new ValidationConfigProperties();
        RestClientTokenInterceptor interceptor = restClientConfig.restClientTokenInterceptor(config);
        Assertions.assertNotNull(interceptor);
    }

    @Test
    void shouldConfigureConnectTimeoutWhenSpecified() {
        RestClient.Builder builder = restClientConfig.restClientBuilder(Optional.empty(), 3000L, 7000L);
        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory)
                ReflectionTestUtils.getField(builder, "requestFactory");
        Assertions.assertNotNull(factory);
        Assertions.assertEquals(3000, ReflectionTestUtils.getField(factory, "connectTimeout"));
    }

    @Test
    void shouldConfigureReadTimeoutWhenSpecified() {
        RestClient.Builder builder = restClientConfig.restClientBuilder(Optional.empty(), 3000L, 7000L);
        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory)
                ReflectionTestUtils.getField(builder, "requestFactory");
        Assertions.assertNotNull(factory);
        Assertions.assertEquals(7000, ReflectionTestUtils.getField(factory, "readTimeout"));
    }

    @Test
    void shouldUseDefaultTimeoutsWhenNotSpecified() {
        RestClient.Builder builder = restClientConfig.restClientBuilder(Optional.empty(), 5000L, 5000L);
        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory)
                ReflectionTestUtils.getField(builder, "requestFactory");
        Assertions.assertNotNull(factory);
        Assertions.assertEquals(5000, ReflectionTestUtils.getField(factory, "connectTimeout"));
        Assertions.assertEquals(5000, ReflectionTestUtils.getField(factory, "readTimeout"));
    }

    @Test
    void shouldAddTokenInterceptorToBuilderWhenPresent() {
        ValidationConfigProperties config = new ValidationConfigProperties();
        RestClientTokenInterceptor interceptor = new RestClientTokenInterceptor(config);
        RestClient.Builder builder = restClientConfig.restClientBuilder(Optional.of(interceptor), 5000L, 5000L);
        @SuppressWarnings("unchecked")
        List<ClientHttpRequestInterceptor> interceptors =
                (List<ClientHttpRequestInterceptor>) ReflectionTestUtils.getField(builder, "interceptors");
        Assertions.assertNotNull(interceptors);
        Assertions.assertTrue(interceptors.contains(interceptor));
    }

    @Test
    void shouldNotAddInterceptorToBuilderWhenAbsent() {
        RestClient.Builder builder = restClientConfig.restClientBuilder(Optional.empty(), 5000L, 5000L);
        @SuppressWarnings("unchecked")
        List<ClientHttpRequestInterceptor> interceptors =
                (List<ClientHttpRequestInterceptor>) ReflectionTestUtils.getField(builder, "interceptors");
        Assertions.assertTrue(interceptors == null || interceptors.isEmpty());
    }

    @Test
    void shouldCreateFallbackRestClientWithInterceptor() {
        ValidationConfigProperties config = new ValidationConfigProperties();
        RestClientTokenInterceptor interceptor = new RestClientTokenInterceptor(config);
        Mockito.when(tokenInterceptorProvider.getIfAvailable()).thenReturn(interceptor);
        RestClient client = restClientConfig.registryRestClient(tokenInterceptorProvider);
        Assertions.assertNotNull(client);
    }

    @Test
    void shouldCreateFallbackRestClientWithoutInterceptor() {
        Mockito.when(tokenInterceptorProvider.getIfAvailable()).thenReturn(null);
        RestClient client = restClientConfig.registryRestClient(tokenInterceptorProvider);
        Assertions.assertNotNull(client);
    }

}
