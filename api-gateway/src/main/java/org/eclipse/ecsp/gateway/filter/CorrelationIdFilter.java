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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
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
 * <p>
 * Extracts or generates correlation ID from X-Correlation-ID header,
 * adds it to MDC for structured logging, and propagates downstream.
 *
 * <p>
 * Execution order: HIGHEST_PRECEDENCE to ensure correlation ID is available
 * for all subsequent filters and logging.
 */
@Component
@Slf4j
public class CorrelationIdFilter implements WebFilter, Ordered {

    private static final String X_CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Extract or generate correlation ID
        String correlationId = extractOrGenerateCorrelationId(exchange);

        // Add correlation ID to exchange attributes
        exchange.getAttributes().put(GatewayConstants.CORRELATION_ID, correlationId);

        // Add correlation ID to response header for client traceability
        exchange.getResponse().getHeaders().add(X_CORRELATION_ID_HEADER, correlationId);

        // Set MDC for logging in this thread
        MDC.put(GatewayConstants.CORRELATION_ID, correlationId);

        // Propagate correlation ID in reactive context
        return chain.filter(exchange)
                .contextWrite(Context.of(GatewayConstants.CORRELATION_ID, correlationId))
                .doFinally(signalType -> MDC.remove(GatewayConstants.CORRELATION_ID));
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
                .get(X_CORRELATION_ID_HEADER);

        if (correlationHeaders != null && !correlationHeaders.isEmpty()) {
            String correlationId = correlationHeaders.get(0);
            if (correlationId != null && !correlationId.isBlank()) {
                log.debug("Using existing correlation ID: {}", correlationId);
                return correlationId.trim();
            }
        }

        String newCorrelationId = UUID.randomUUID().toString();
        log.debug("Generated new correlation ID: {}", newCorrelationId);
        return newCorrelationId;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
