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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.GlobalFilterUtils;
import org.eclipse.ecsp.gateway.utils.GlobalFilterUtils.CachedResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class CacheFilterTest {

    @Mock
    ServerHttpRequest request;
    @Mock
    ServerHttpResponse mutatedHttpResponse;
    @Mock
    HttpHeaders httpHeaders;
    @Mock
    ServerWebExchange exchange;
    @Mock
    URI uri;
    @Mock
    HttpMethod httpMethod;
    @Mock
    ServerWebExchange.Builder builder;
    @Mock
    Cache.ValueWrapper value;
    @Mock
    private RedisCacheManager cacheManager;
    @Mock
    private Cache cache;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private GlobalFilterUtils globalFilterUtils;
    @InjectMocks
    private CacheFilter cacheFilter;

    @Test
    void subsequentGetCallTest() {
        when(cacheManager.getCache(any())).thenReturn(cache);

        String cachedRequestKey = "http://example.com";
        ServerWebExchange serverWebExchange = mock(ServerWebExchange.class);
        when(serverWebExchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(uri);
        when(uri.toString()).thenReturn(cachedRequestKey);

        when(request.getHeaders()).thenReturn(httpHeaders);
        when(request.getMethod()).thenReturn(httpMethod);
        when(httpMethod.name()).thenReturn("GET");
        when(httpHeaders.getFirst(GatewayConstants.ACCOUNT_ID)).thenReturn("accountId");
        when(httpHeaders.getFirst(GatewayConstants.TENANT_ID)).thenReturn("tenantId");
        when(httpHeaders.getFirst(GatewayConstants.USER_ID)).thenReturn("userId");

        when(cache.get(cachedRequestKey)).thenReturn(null);
        when(serverWebExchange.getResponse()).thenReturn(mutatedHttpResponse);
        when(mutatedHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);

        when(serverWebExchange.mutate()).thenReturn(builder);

        when(builder.response(any())).thenReturn(builder);
        when(builder.build()).thenReturn(serverWebExchange);

        try (MockedConstruction<GlobalFilterUtils> mockPaymentService =
                     Mockito.mockConstruction(GlobalFilterUtils.class,
                             (mock, context) -> {
                                 when(
                                         mock.getServerHttpResponse(serverWebExchange, cache, cachedRequestKey))
                                         .thenReturn(mutatedHttpResponse);
                             })) {
            CacheFilter.Config config = new CacheFilter.Config();
            config.setCacheKey("{routeId}-{tenantId}-searchRequest");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);
            cacheFilter.apply(config).filter(serverWebExchange, chain);
        }
        Mockito.verify(cache, atLeastOnce()).get(Mockito.any());

    }

    @Test
    void subsequentGetCallTestMethodIsPost() {
        when(cacheManager.getCache(any())).thenReturn(cache);

        String cachedRequestKey = "http://example.com";
        ServerWebExchange serverWebExchange = mock(ServerWebExchange.class);
        when(serverWebExchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(uri);
        when(uri.toString()).thenReturn(cachedRequestKey);

        when(request.getHeaders()).thenReturn(httpHeaders);
        when(request.getMethod()).thenReturn(httpMethod);
        when(httpMethod.name()).thenReturn("POST");
        doNothing().when(globalFilterUtils).deleteFromRedisCache(cache, cachedRequestKey);
        when(httpHeaders.getFirst(GatewayConstants.ACCOUNT_ID)).thenReturn("accountId");
        when(httpHeaders.getFirst(GatewayConstants.TENANT_ID)).thenReturn("tenantId");
        when(httpHeaders.getFirst(GatewayConstants.USER_ID)).thenReturn("userId");

        when(cache.get(cachedRequestKey)).thenReturn(null);
        when(serverWebExchange.getResponse()).thenReturn(mutatedHttpResponse);
        when(mutatedHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);

        when(serverWebExchange.mutate()).thenReturn(builder);

        when(builder.response(any())).thenReturn(builder);
        when(builder.build()).thenReturn(serverWebExchange);

        try (MockedConstruction<GlobalFilterUtils> mockPaymentService =
                     Mockito.mockConstruction(GlobalFilterUtils.class,
                             (mock, context) -> {
                                 when(mock.getServerHttpResponse(serverWebExchange, cache, cachedRequestKey))
                                         .thenReturn(mutatedHttpResponse);
                             })) {
            CacheFilter.Config config = new CacheFilter.Config();
            config.setCacheKey("{routeId}-{tenantId}-searchRequest");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);
            cacheFilter.apply(config).filter(serverWebExchange, chain);
            assertEquals("POST", serverWebExchange.getRequest().getMethod().name());
        }
    }

    @Test
    void getCallTestWithDataPresentInRedis() {
        when(cacheManager.getCache(any())).thenReturn(cache);

        String cachedRequestKey = "http://example.com";
        ServerWebExchange serverWebExchange = mock(ServerWebExchange.class);
        when(serverWebExchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(uri);
        when(uri.toString()).thenReturn(cachedRequestKey);

        when(request.getHeaders()).thenReturn(httpHeaders);
        when(request.getMethod()).thenReturn(httpMethod);
        when(httpMethod.name()).thenReturn("GET");
        when(httpHeaders.getFirst(GatewayConstants.ACCOUNT_ID)).thenReturn("accountId");
        when(httpHeaders.getFirst(GatewayConstants.TENANT_ID)).thenReturn("tenantId");
        when(httpHeaders.getFirst(GatewayConstants.USER_ID)).thenReturn("userId");
        Mockito.when(cache.getName()).thenReturn("Default");
        when(cache.get(any())).thenReturn(
                () -> "[{httpStatus=OK, headers={transfer-encoding=[chunked], "
                        + "Vary=[Origin, Access-Control-Request-Method, Access-Control-Request-Headers, "
                        + "origin,access-control-request-method,access-control-request-headers,accept-encoding]"
                        + ", X-Content-Type-Options=[nosniff], X-XSS-Protection=[1; mode=block], "
                        + "Cache-Control=[no-cache, no-store, max-age=0, must-revalidate], Pragma=[no-cache], "
                        + "Expires=[0], X-Frame-Options=[DENY], Content-Encoding=[gzip], "
                        + "Content-Type=[application/json], Date=[Wed, 12 Jun 2024 14:28:29 GMT]}, "
                        + "body=H4sIAAAAAAAA/8yYX2/TQAzAv0ueO2r77LNvbzzBAxKIDV4QD2ENU0TTVmlAQtO+O84mULulGoy0Oe"
                        + "Upucv5d/539n26KRZlVxbnN0W5rNquOC/e/qjai01VLV7efbidFYt1U9YrH9r2n9/UTd3dj80Kn7ut1/0Y+dv"
                        + "VumnK1cLfNt/70a5uqm1XNpviHKOpgIJQRLidnVhsQgaLpxWr0TgJxPQXYmf9hKpfN87jnIDCyCTCaoS7JG21r"
                        + "Mqty6QXPJqsRGYYVEMIQ7t2UXj2/l7w2aX/trNxmCOMvPOk5qY3UhykOWQDtDnSuCgKrhhTYEkyrTs4SYKAmiQ"
                        + "NhqGbiP6YqN630XEUk9xQgkpP4uyoBY8AEpAjBSYZtNC7Zdl9XbfNAfeVOfY8PCIPu7NEiqwTJxAn0RT8YcapS"
                        + "aIqehglthxIEpBKDjohhpCHTjx6osf0c0jGjJ07kugn/z8l/t8kMi6JBgXPLpNbx5SCCdLkJ1BPIjGQZaET6cs"
                        + "0nZpEkTx2+mNochJCCAYQnxXFY5OwlyvDlcrJSYTJJo+dO5LICJNXBT2JoLdVWehEAiFmoRNvNnW4hjw1iUlSmj"
                        + "6zuWFi8k4sh8yGyoE4BxIC8Apl6hyLgQRiFFYeIrnYtPXq+sPmui0X1aNmkEcu29C7rxAgoeyqZcTlTWJi5DR1n"
                        + "nAE8yQeIk6dJ1AAzNtMs8Ezdrm+/lJefXtwVXMMjJj6/tvzxAHL763w+HcTBtfpvj47n3H5c9Mzv6pWVVtfva7c"
                        + "j9uP5bJelI72YF84l7E92jXrLaKpWY5gkXm/8s0EzPo7u5gbWBDhSITZaSz4OQ/GANmBuTEBve38HzA7Apj7vkV"
                        + "vLHLzMeXkzSjS3vX5eMsbpqgAiHT7+RcAAAD//wMASe2AevoZAAA=}]");
        when(serverWebExchange.getResponse()).thenReturn(mutatedHttpResponse);
        when(mutatedHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);

        when(serverWebExchange.mutate()).thenReturn(builder);

        when(builder.response(any())).thenReturn(builder);
        when(builder.build()).thenReturn(serverWebExchange);

        try (MockedConstruction<GlobalFilterUtils> mockPaymentService =
                     Mockito.mockConstruction(GlobalFilterUtils.class,
                             (mock, context) -> {
                                 when(mock.getServerHttpResponse(serverWebExchange, cache, cachedRequestKey))
                                         .thenReturn(mutatedHttpResponse);
                             })) {
            CacheFilter.Config config = new CacheFilter.Config();
            config.setCacheKey("{routeId}-{tenantId}-searchRequest");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);
            cacheFilter.apply(config).filter(serverWebExchange, chain);
        }
        Mockito.verify(cache, atLeastOnce()).get(Mockito.any());
    }

    @Test
    void putCallTest() {

        when(cacheManager.getCache(any())).thenReturn(cache);
        String cachedRequestKey = "http://example.com";
        when(cache.get(cachedRequestKey)).thenReturn(null);
        when(globalFilterUtils.getServerHttpResponse(any(), any(), any())).thenReturn(mock(ServerHttpResponse.class));

        when(cache.get(cachedRequestKey)).thenReturn(null);
        globalFilterUtils.deleteFromRedisCache(cache, cachedRequestKey);

        CacheFilter.Config config = new CacheFilter.Config();
        cacheFilter.apply(config);
        Mockito.verifyNoInteractions(cache);

    }

    @Test
    void postCallTest() {

        when(cacheManager.getCache(any())).thenReturn(cache);
        String cachedRequestKey = "http://example.com";

        when(cache.get(cachedRequestKey)).thenReturn(null);

        CacheFilter.Config config = new CacheFilter.Config();
        GatewayFilter result = cacheFilter.apply(config);
        assertNotNull(result);
    }

    @Test
    void deleteCallTest() {

        when(cacheManager.getCache(any())).thenReturn(cache);
        String cachedRequestKey = "http://example.com";
        when(cache.get(cachedRequestKey)).thenReturn(null);
        when(globalFilterUtils.getServerHttpResponse(any(), any(), any())).thenReturn(mock(ServerHttpResponse.class));

        when(cache.get(cachedRequestKey)).thenReturn(null);
        globalFilterUtils.deleteFromRedisCache(cache, cachedRequestKey);
        CacheFilter.Config config = new CacheFilter.Config();
        GatewayFilter result = cacheFilter.apply(config);
        assertNotNull(result);
    }

    @Test
    void nullCacheTest() {

        when(cacheManager.getCache(any())).thenReturn(null);
        String cachedRequestKey = "http://example.com";

        when(cache.get(cachedRequestKey)).thenReturn(null);
        when(globalFilterUtils.getServerHttpResponse(any(), any(), any())).thenReturn(mock(ServerHttpResponse.class));

        when(cache.get(cachedRequestKey)).thenReturn(null);
        globalFilterUtils.deleteFromRedisCache(cache, cachedRequestKey);

        CacheFilter.Config config = new CacheFilter.Config();
        GatewayFilter result = cacheFilter.apply(config);
        assertNotNull(result);
    }

    @Test
    void getCallNullResponseTest() {
        when(cacheManager.getCache(any())).thenReturn(cache);

        String cachedRequestKey = "http://example.com";
        Cache.ValueWrapper mockedValue = mock(Cache.ValueWrapper.class);
        when(cache.get(cachedRequestKey)).thenReturn(mockedValue);
        String mockString = "StringValueFromCache";
        when(globalFilterUtils.getResponseInString(any())).thenReturn(mockString);
        CacheFilter.Config config = new CacheFilter.Config();
        GatewayFilter result = cacheFilter.apply(config);
        assertNotNull(result);
    }

    @Test
    void getCallTest() {
        when(cacheManager.getCache(any())).thenReturn(cache);

        String cachedRequestKey = "http://example.com";

        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);
        ServerWebExchange mockedExchange = mockExchange(cachedRequestKey, GatewayConstants.GET);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        DataBufferFactory factory = mock(DataBufferFactory.class);
        DataBuffer buffer = mock(DataBuffer.class);
        when(mockedExchange.getResponse()).thenReturn(response);
        when(mockedExchange.getResponse().getHeaders()).thenReturn(header);
        when(mockedExchange.getResponse().bufferFactory()).thenReturn(factory);


        String mockString = "StringValueFromCache";
        when(mockedExchange.getResponse().bufferFactory().wrap(mockString.getBytes())).thenReturn(buffer);

        CachedResponse cacheResponse = new CachedResponse(HttpStatus.OK, header, mockString.getBytes());
        Cache.ValueWrapper mockedValue = new ValueWrapper() {

            @Override
            public Object get() {
                return cacheResponse;
            }
        };
        when(cache.get(cachedRequestKey)).thenReturn(mockedValue);

        when(globalFilterUtils.getResponseInString(any())).thenReturn(mockString);

        mock(GatewayFilterChain.class);
        CacheFilter.Config config = new CacheFilter.Config();
        cacheFilter.apply(config);
        Mockito.verifyNoInteractions(cache);
    }


    private ServerWebExchange mockExchange(String cachedRequestKey, String methodName) {

        ServerWebExchange mockedExchange = mock(ServerWebExchange.class);
        ServerHttpRequest mockedRequest = mock(ServerHttpRequest.class);
        when(mockedExchange.getRequest()).thenReturn(mockedRequest);
        when(mockedRequest.getURI()).thenReturn(URI.create(cachedRequestKey));
        if (methodName.equalsIgnoreCase("GET")) {
            when(mockedRequest.getMethod()).thenReturn(HttpMethod.GET);
        } else if (methodName.equalsIgnoreCase("PUT")) {
            when(mockedRequest.getMethod()).thenReturn(HttpMethod.PUT);
        } else if (methodName.equalsIgnoreCase("DELETE")) {
            when(mockedRequest.getMethod()).thenReturn(HttpMethod.DELETE);
        } else if (methodName.equalsIgnoreCase("POST")) {
            when(mockedRequest.getMethod()).thenReturn(HttpMethod.POST);
        }

        ServerHttpResponse response = mock(ServerHttpResponse.class);
        ServerWebExchange.Builder mockedBuilder = mock(ServerWebExchange.Builder.class);
        when(mockedExchange.mutate()).thenReturn(mockedBuilder);
        when(mockedExchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(new HttpHeaders());
        when(response.writeWith(Mockito.any())).thenReturn(Mono.empty().then());
        DataBufferFactory mockedBufferFacory = Mockito.mock(DataBufferFactory.class);
        when(response.bufferFactory()).thenReturn(mockedBufferFacory);
        doReturn(Mockito.mock(DataBuffer.class)).when(mockedBufferFacory).wrap(Mockito.any(byte[].class));

        return mockedExchange;
    }

    @Test
    void testGetCacheResponse() {
        GlobalFilterUtils globalFilterUtilMock = mock(GlobalFilterUtils.class);
        ReflectionTestUtils.setField(cacheFilter, "globalFilterUtils", globalFilterUtilMock);
        when(globalFilterUtilMock.getResponseInString(Mockito.any())).thenReturn("Str123");
        CachedResponse cachedResponse = new CachedResponse();
        cachedResponse.setHttpStatus(HttpStatus.OK);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");
        cachedResponse.setHeaders(headers);
        cachedResponse.setBody("hello".getBytes());
        ValueWrapper mockedCache = Mockito.mock(ValueWrapper.class);
        doReturn(cachedResponse).when(mockedCache).get();
        ReflectionTestUtils.invokeMethod(cacheFilter, "getCachedResponse", mockExchange("key", "GET"), mockedCache);
        Mockito.verify(mockedCache, atLeastOnce()).get();
    }

    @Test
    void testGetCacheCompressionFailedResponse() {
        GlobalFilterUtils globalFilterUtilMock = mock(GlobalFilterUtils.class);
        ReflectionTestUtils.setField(cacheFilter, "globalFilterUtils", globalFilterUtilMock);
        when(globalFilterUtilMock.getResponseInString(Mockito.any())).thenReturn("Str123");
        CachedResponse cachedResponse = new CachedResponse();
        cachedResponse.setHttpStatus(HttpStatus.OK);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");
        cachedResponse.setHeaders(headers);
        cachedResponse.setBody("hello".getBytes());
        ValueWrapper mockedCache = Mockito.mock(ValueWrapper.class);
        doReturn(cachedResponse).when(mockedCache).get();
        ReflectionTestUtils.invokeMethod(cacheFilter, "getCachedResponse", mockExchange("key", "GET"), mockedCache);
        Mockito.verify(globalFilterUtilMock, atLeastOnce()).getResponseInString(Mockito.any());
        Mockito.verify(mockedCache, atLeastOnce()).get();
    }

    @Test
    void testGetNullErrorResponse() {
        CachedResponse cachedResponse = new CachedResponse();
        cachedResponse.setHttpStatus(HttpStatus.OK);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");
        cachedResponse.setHeaders(headers);
        cachedResponse.setBody("hello".getBytes());
        ValueWrapper mockedCache = Mockito.mock(ValueWrapper.class);
        doReturn(cachedResponse).when(mockedCache).get();
        Assertions.assertThrowsExactly(NullPointerException.class,
                () -> ReflectionTestUtils.invokeMethod(cacheFilter,
                        "getCachedResponse",
                        mockExchange("key", "GET"),
                        mockedCache));
        Mockito.verify(mockedCache, atLeastOnce()).get();
    }

    @Test
    void testPrepareRequestKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("accountId", "igniteAccount");
        headers.set("tenantId", "ignite");
        headers.set("user-id", "user123");
        ServerHttpRequest mockedRequest = Mockito.mock(ServerHttpRequest.class);
        doReturn(headers).when(mockedRequest).getHeaders();
        doReturn(URI.create("http://localhost:8080/v2/users")).when(mockedRequest).getURI();
        ReflectionTestUtils.invokeMethod(cacheFilter, "prepareCachedRequestKey",
                mockedRequest, "accountId-tenantId-userId", "routeId123");
        Mockito.verify(mockedRequest, atLeastOnce()).getHeaders();
    }

    @Test
    void testPrepareRequestKeyNoHeaders() {
        HttpHeaders headers = new HttpHeaders();
        ServerHttpRequest mockedRequest = Mockito.mock(ServerHttpRequest.class);
        doReturn(headers).when(mockedRequest).getHeaders();
        doReturn(URI.create("http://localhost:8080/v2/users"))
                .when(mockedRequest).getURI();
        ReflectionTestUtils.invokeMethod(cacheFilter,
                "prepareCachedRequestKey",
                mockedRequest, "accountId-tenantId-userId", "routeId123");
        Mockito.verify(mockedRequest, atLeastOnce()).getHeaders();
    }

    @Test
    void testPrepareRequestKeyWithoutHeaders() {
        HttpHeaders headers = new HttpHeaders();
        ServerHttpRequest mockedRequest = Mockito.mock(ServerHttpRequest.class);
        doReturn(headers).when(mockedRequest).getHeaders();
        doReturn(URI.create("http://localhost:8080/v2/users"))
                .when(mockedRequest).getURI();
        String key = (String) ReflectionTestUtils.invokeMethod(cacheFilter,
                "prepareCachedRequestKey",
                mockedRequest, "{accountId}-{tenantId}-{userId}", "routeId123");
        assertEquals("{accountId}-{tenantId}-{userId}", key);
    }

    @Test
    void testPrepareRequestKeyWithHeaders() {
        testPrepareKey("{accountId}-{tenantId}-{USER-ID}",
                "igniteAccount-ignite-user123");
    }

    @Test
    void testPrepareRequestKeyWithRouteId() {
        testPrepareKey("{routeId}-{accountId}-{tenantId}-{USER-ID}",
                "routeId123-igniteAccount-ignite-user123");
    }

    @Test
    void testPrepareRequestKeyWithRequestPath() {
        testPrepareKey("{requestPath}-{routeId}-{accountId}-{tenantId}-{USER-ID}",
                "http://localhost:8080/v2/users-routeId123-igniteAccount-ignite-user123");
    }

    @Test
    void testPrepareRequestKeyWithUserId() {
        testPrepareKey("{requestPath}-{routeId}-{accountId}-{tenantId}-{userId}",
            "http://localhost:8080/v2/users-routeId123-igniteAccount-ignite-user123");
    }

    void testPrepareKey(String key, String expected) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(GatewayConstants.ACCOUNT_ID, "igniteAccount");
        headers.set(GatewayConstants.TENANT_ID, "ignite");
        headers.set(GatewayConstants.USER_ID, "user123");
        ServerHttpRequest mockedRequest = Mockito.mock(ServerHttpRequest.class);
        doReturn(headers).when(mockedRequest).getHeaders();
        doReturn(URI.create("http://localhost:8080/v2/users"))
                .when(mockedRequest).getURI();
        String result = (String) ReflectionTestUtils.invokeMethod(cacheFilter,
                "prepareCachedRequestKey",
                mockedRequest, key, "routeId123");
        assertEquals(expected, result);
    }
}