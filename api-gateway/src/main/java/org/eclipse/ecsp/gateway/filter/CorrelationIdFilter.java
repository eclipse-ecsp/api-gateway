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

package org.eclipse.ecsp.gateway.filter;

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.List;
import java.util.UUID;

/**
 * Global filter for correlation ID propagation across requests.
 *
 * <p>Extracts or generates correlation ID from X-Correlation-ID header,
 * adds it to MDC for structured logging, and propagates downstream.
 *
 * <p>Execution order: HIGHEST_PRECEDENCE to ensure correlation ID is available
 * for all subsequent filters and logging.
 */
@Component
public class CorrelationIdFilter implements WebFilter, Ordered {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "correlationId";

    @Value("${api.gateway.correlation-id-header:correlationId}")
    private String headerName = CORRELATION_ID_HEADER;

    /**
     * Default constructor.
     */
    public CorrelationIdFilter() {
        // Default constructor
    }

   

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Extract or generate correlation ID
        String correlationId = extractOrGenerateCorrelationId(exchange);

        // Add correlation ID to exchange attributes
        exchange.getAttributes().put(headerName, correlationId);

        // Add correlation ID to response header for client traceability
        exchange.getResponse().getHeaders().add(headerName, correlationId);

        // Set MDC for logging in this thread
        MDC.put(headerName, correlationId);

        // Propagate correlation ID in reactive context
        return chain.filter(exchange)
                .contextWrite(Context.of(headerName, correlationId))
                .doFinally(signalType -> MDC.remove(headerName));
    }

    /**
     * Extract correlation ID from request header or generate new UUID.
     *
     * @param exchange Server web exchange
     * @return Correlation ID
     */
    private String extractOrGenerateCorrelationId(ServerWebExchange exchange) {
        List<String> correlationHeaders = exchange.getRequest()
                .getHeaders()
                .get(headerName);

        if (correlationHeaders != null && !correlationHeaders.isEmpty()) {
            String correlationId = correlationHeaders.get(0);
            if (correlationId != null && !correlationId.isBlank()) {
                LOGGER.debug("Using existing {} header: {}", headerName, correlationId);
                return correlationId.trim();
            }
        }

        String newCorrelationId = UUID.randomUUID().toString();
        LOGGER.debug("Generated new {} header: {}", headerName, newCorrelationId);
        return newCorrelationId;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
