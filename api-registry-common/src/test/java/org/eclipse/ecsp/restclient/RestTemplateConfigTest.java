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

package org.eclipse.ecsp.restclient;

import org.eclipse.ecsp.config.RestTemplateConfig;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Tests for {@link RestTemplateConfig}.
 */
@ExtendWith(MockitoExtension.class)
class RestTemplateConfigTest {

    @Mock
    private ObjectProvider<RestTemplateTokenInterceptor> tokenInterceptorProvider;

    private RestTemplateConfig restTemplateConfig;

    @BeforeEach
    void setUp() {
        restTemplateConfig = new RestTemplateConfig();
    }

    @Test
    void restTemplateTest() {
        Mockito.when(tokenInterceptorProvider.getIfAvailable()).thenReturn(null);
        RestTemplate restTemplate = restTemplateConfig.registryRestTemplate(true, 5000, 5000, tokenInterceptorProvider);
        Assertions.assertNotNull(restTemplate);
    }

    @Test
    void shouldConfigureConnectTimeoutWhenSpecified() {
        Mockito.when(tokenInterceptorProvider.getIfAvailable()).thenReturn(null);
        RestTemplate restTemplate = restTemplateConfig.registryRestTemplate(true, 3000L, 7000L, tokenInterceptorProvider);
        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        Assertions.assertEquals(3000, ReflectionTestUtils.getField(factory, "connectTimeout"));
    }

    @Test
    void shouldConfigureReadTimeoutWhenSpecified() {
        Mockito.when(tokenInterceptorProvider.getIfAvailable()).thenReturn(null);
        RestTemplate restTemplate = restTemplateConfig.registryRestTemplate(true, 3000L, 7000L, tokenInterceptorProvider);
        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        Assertions.assertEquals(7000, ReflectionTestUtils.getField(factory, "readTimeout"));
    }

    @Test
    void shouldUseDefaultTimeoutsWhenNotSpecified() {
        Mockito.when(tokenInterceptorProvider.getIfAvailable()).thenReturn(null);
        RestTemplate restTemplate = restTemplateConfig.registryRestTemplate(true, 5000L, 5000L, tokenInterceptorProvider);
        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        Assertions.assertEquals(5000, ReflectionTestUtils.getField(factory, "connectTimeout"));
        Assertions.assertEquals(5000, ReflectionTestUtils.getField(factory, "readTimeout"));
    }

    @Test
    void shouldAddTokenInterceptorWhenAvailable() {
        ValidationConfigProperties config = new ValidationConfigProperties();
        RestTemplateTokenInterceptor interceptor = new RestTemplateTokenInterceptor(config);
        Mockito.when(tokenInterceptorProvider.getIfAvailable()).thenReturn(interceptor);
        RestTemplate restTemplate = restTemplateConfig.registryRestTemplate(true, 5000L, 5000L, tokenInterceptorProvider);
        Assertions.assertTrue(restTemplate.getInterceptors().contains(interceptor));
    }

    @Test
    void shouldNotSetCustomErrorHandlerWhenHandleErrorFalse() {
        Mockito.when(tokenInterceptorProvider.getIfAvailable()).thenReturn(null);
        RestTemplate restTemplate = restTemplateConfig.registryRestTemplate(false, 5000L, 5000L, tokenInterceptorProvider);
        Assertions.assertFalse(restTemplate.getErrorHandler() instanceof RestTemplateErrorHandler);
    }
}

