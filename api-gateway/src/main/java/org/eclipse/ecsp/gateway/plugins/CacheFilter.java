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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.GlobalFilterUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.Cache;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.cache.LocalResponseCacheUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Filter to cache the response data.
 */
@Component
@ConditionalOnExpression("${api.caching.enabled:true} and '${api.caching.type}' == 'redis'")
public class CacheFilter extends AbstractGatewayFilterFactory<CacheFilter.Config> {
    /**
     * Creating the instance of logger.
     */
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(CacheFilter.class);
    /**
     * RediscacheManager instance connection to Redis.
     */
    private final RedisCacheManager cacheManager;
    /**
     * Global FilterUtils to get the response body.
     */
    private final GlobalFilterUtils globalFilterUtils;
    ObjectMapper objectMapper = new ObjectMapper();
    /**
     * cacheName holds the name of the cache.
     */
    @Value("${api.caching.cacheName}")
    private String cacheName;


    /**
     * Parameterized Constructor.
     *
     * @param cacheManager cacheManager.
     */
    public CacheFilter(RedisCacheManager cacheManager) {
        super(Config.class);
        this.cacheManager = cacheManager;
        this.globalFilterUtils = new GlobalFilterUtils();
    }

    private static String prepareCachedRequestKey(ServerHttpRequest request, String cachedRequestKey) {
        LOGGER.debug("CacheRequest | cachedRequestKey: {} ", cachedRequestKey);
        LOGGER.debug("CacheRequest | tenantId {}, userID {} ",
                request.getHeaders().getFirst(GatewayConstants.TENANT_ID),
                request.getHeaders().getFirst(GatewayConstants.USER_ID));
        StringBuilder finalCacheKey = new StringBuilder(request.getURI().toString());
        String cacheKey = cachedRequestKey.replace("{", "");
        cacheKey = cacheKey.replace("}", "");
        String[] cachedKey = cacheKey.split("-");
        LOGGER.debug("CacheRequest | details of cacheKey {} ", cachedKey[0], cachedKey.length);
        for (String key : cachedKey) {
            if (key.equalsIgnoreCase(GatewayConstants.ACCOUNT_ID)
                    && request.getHeaders().getFirst(GatewayConstants.ACCOUNT_ID) != null) {
                finalCacheKey.append("_").append(request.getHeaders().getFirst(GatewayConstants.ACCOUNT_ID));
                LOGGER.debug("CacheRequest | after adding accountId {}", finalCacheKey);
            }
            if (key.equalsIgnoreCase(GatewayConstants.TENANT_ID)
                    && request.getHeaders().getFirst(GatewayConstants.TENANT_ID) != null) {
                finalCacheKey.append("_").append(request.getHeaders().getFirst(GatewayConstants.TENANT_ID));
                LOGGER.debug("CacheRequest | after adding tenantId {}", finalCacheKey);
            }
            if (key.equalsIgnoreCase(GatewayConstants.USERID)
                    && request.getHeaders().getFirst(GatewayConstants.USER_ID) != null) {
                finalCacheKey.append("_").append(request.getHeaders().getFirst(GatewayConstants.USER_ID));
                LOGGER.debug("CacheRequest | after adding userId {}", finalCacheKey);
            }
        }
        finalCacheKey.append("_").append(cachedKey[cachedKey.length - 1]);
        LOGGER.debug("CacheRequest | final cache key {} ", finalCacheKey);
        return finalCacheKey.toString();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            if (!HttpMethod.GET.equals(exchange.getRequest().getMethod())
                    && LocalResponseCacheUtils.isNoCacheRequest(exchange.getRequest())) {
                LOGGER.debug("CacheFilter | request doesnt qualify for caching, skipping cache filter for request {} ",
                        exchange.getRequest().getPath());
                return chain.filter(exchange);
            }
            return processCacheRequest(config, exchange, chain);
        }, GatewayConstants.CACHE_FILTER_ORDER);
    }

    private Mono<Void> processCacheRequest(Config config, ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String cachedRequestKey = prepareCachedRequestKey(request, config.cacheKey);
        LOGGER.debug("CacheRequest | cache key prepared {} ", cachedRequestKey);
        final Cache cache = cacheManager.getCache(cacheName);
        if (cache != null && StringUtils.isNotEmpty(cachedRequestKey)) {
            LOGGER.debug("cache Name {}", cache.getName());
            Cache.ValueWrapper cacheResponse = null;
            try {
                LOGGER.debug("cacheResponse {} ", cache.get(cachedRequestKey));
                cacheResponse = cache.get(cachedRequestKey);
            } catch (Exception e) {
                LOGGER.error("exception while getting data from redis {} ", e.getMessage());
            }

            LOGGER.debug("Cached response available :{} ", !Objects.isNull(cacheResponse));
            if (!Objects.isNull(cacheResponse)) {
                Mono<Void> cachedExchange = getCachedResponse(exchange, cacheResponse);
                if (cachedExchange != null) {
                    LOGGER.debug("returning cached response for request {} ", request.getPath());
                    return cachedExchange;
                }
            } else {
                LOGGER.info("Cache response data not available calling microservices {} {}",
                        request.getMethod(), request.getPath());
                final ServerHttpResponse mutatedHttpResponse =
                        globalFilterUtils.getServerHttpResponse(exchange, cache, cachedRequestKey);
                LOGGER.debug("RequestCache | mutatedHttpResponse {} ", mutatedHttpResponse);
                return chain.filter(exchange.mutate().response(mutatedHttpResponse).build());
            }
        }
        return chain.filter(exchange);
    }

    private Mono<Void> getCachedResponse(ServerWebExchange exchange, Cache.ValueWrapper cacheResponse) {
        String objectValue = null;
        try {
            objectValue = objectMapper.writeValueAsString(cacheResponse.get());
            GlobalFilterUtils.CachedResponse response =
                    objectMapper.readValue(objectValue, GlobalFilterUtils.CachedResponse.class);
            LOGGER.debug("CacheRequest | cached response {}", response);
            if (response != null) {
                byte[] responseBody = response.getBody();
                byte[] resultByte = responseBody;
                if ("gzip".equalsIgnoreCase(response
                        .getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING))) {
                    String decompressedString = globalFilterUtils.getResponseInString(responseBody);
                    LOGGER.debug("CacheRequest | cache decompressedString: {}", decompressedString);
                    resultByte = decompressedString.getBytes(StandardCharsets.UTF_8);
                }
                LOGGER.debug("CacheRequest | CachedResponses Status : {}", response.getHttpStatus());
                final ServerHttpResponse serverHttpResponse = exchange.getResponse();
                serverHttpResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                final DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(resultByte);
                return exchange.getResponse().writeWith(Flux.just(buffer));
            }

        } catch (JsonProcessingException e) {
            LOGGER.error("CacheRequest | Error serializing cached response", e);
        }
        return null;
    }

    /**
     * Config class to pass configuration to filter.
     */
    @Setter
    @Getter
    @NoArgsConstructor
    @ToString
    public static class Config {
        /**
         * Cache key to be used for caching the response.
         */
        protected String cacheKey;
        // Put the configuration properties
    }
}