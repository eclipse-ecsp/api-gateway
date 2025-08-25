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

package org.eclipse.ecsp.gateway.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.reactivestreams.Publisher;
import org.springframework.cache.Cache;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * GlobalFilterUtils to read the response body.
 */
@Component
public class GlobalFilterUtils {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(GlobalFilterUtils.class);
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private static final int MIN_INPUT_STREAM_LENGTH = -1;

    /**
     * getServerHttpResponse returns the ServerHttpResponse from exchange object.
     *
     * @param exchange      ServerWebExchange object
     * @param cache         Redis Cache object
     * @param cachedRequest cacheKey
     * @return returns ServerHttpResponse
     */
    public ServerHttpResponse getServerHttpResponse(ServerWebExchange exchange, Cache cache, String cachedRequest) {
        LOGGER.debug("request from GlobalFilterUtils#getServerHttpResponse {} ", exchange.getRequest().getPath());
        ServerHttpResponseDecorator decorator = null;
        try {
            final ServerHttpResponse originalResponse = exchange.getResponse();
            final DataBufferFactory dataBufferFactory = originalResponse.bufferFactory();
            decorator = new ServerHttpResponseDecorator(originalResponse) {
                @Override
                public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                    try {
                        LOGGER.debug("RequestCache | responseCache for request {} ", exchange.getRequest().getPath());
                        if (body instanceof Flux<? extends DataBuffer> fluxData) {
                            LOGGER.debug("RequestCache | response for request {} is Flux",
                                    exchange.getRequest().getPath());
                            return super.writeWith(fluxData.buffer().map(dataBuffers -> {
                                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                dataBuffers.forEach(dataBuffer -> {
                                    final byte[] responseContent = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(responseContent);
                                    try {
                                        outputStream.write(responseContent);
                                    } catch (IOException e) {
                                        LOGGER.error("RequestCache | Error while reading api {} response stream {}",
                                                exchange.getRequest().getPath(), e);
                                        throw new ApiGatewayException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                "api.gateway.request.error",
                                                "Error occurred while processing the request");
                                    }
                                });
                                if (Objects.requireNonNull(getStatusCode()).is2xxSuccessful()) {
                                    LOGGER.info("RequestCache | Saving response for request {} to cache",
                                            exchange.getRequest().getPath());
                                    final CachedResponse cachedResponse = new CachedResponse(
                                            getStatusCode(),
                                            getHeaders(),
                                            outputStream.toByteArray());
                                    cache.put(cachedRequest, cachedResponse);
                                }
                                return dataBufferFactory.wrap(outputStream.toByteArray());
                            }));
                        }
                    } catch (Exception e) {
                        LOGGER.error("RequestCache | error saving response for cache, {}", e);
                    }
                    return super.writeWith(body);
                }
            };
        } catch (Exception e) {
            LOGGER.error("ERROR {}:", e);

        }
        return decorator;
    }

    /**
     * returns the byteResponse in String.
     *
     * @param byteResponse byteResponse
     * @return String format for byteResponse
     */
    public String getResponseInString(byte[] byteResponse) {
        String decompressedString = null;
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteResponse);
             GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {

            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            StringBuilder resultStringBuilder = new StringBuilder();
            int length;
            while ((length = gzipInputStream.read(buffer)) != MIN_INPUT_STREAM_LENGTH) {
                resultStringBuilder.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
            }
            decompressedString = resultStringBuilder.toString();

        } catch (IOException e) {
            LOGGER.error("Error while reading the cached Byte Response {}", e);
        }
        return decompressedString;
    }

    /**
     * getFromRedisCache returns the CachedResponse from Redis Cache.
     *
     * @param cache Redis Cache object
     * @param key   cacheKey
     */
    public void deleteFromRedisCache(Cache cache, String key) {
        cache.evict(key);
    }

    /**
     * Defines the Structure of CachedRequest.
     */
    @Data
    @Builder
    public static class CachedRequest {
        /**
         * request path.
         */
        RequestPath path;
        /**
         * request method.
         */
        HttpMethod method;
        /**
         * request query params.
         */
        MultiValueMap<String, String> queryParams;

    }

    /**
     * Defines the structure of CachedResponse.
     */
    @Data
    @AllArgsConstructor
    public static class CachedResponse {
        /**
         * response status code.
         */
        HttpStatus httpStatus;
        /**
         * response headers.
         */
        HttpHeaders headers;
        /**
         * response body is byte array.
         */
        byte[] body;

        /**
         * Default constructor for CachedResponse.
         */
        public CachedResponse() {
            //DO NOT DELETE
        }

        /**
         * Constructor to create CachedResponse.
         *
         * @param statusCode HttpStatusCode
         * @param headers2   HttpHeaders
         * @param byteArray  byte[]
         */
        public CachedResponse(HttpStatusCode statusCode, HttpHeaders headers2, byte[] byteArray) {
            this.body = byteArray;
            this.headers = headers2;
            this.httpStatus = (HttpStatus) statusCode;
        }
    }
}