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

import org.eclipse.ecsp.security.SecurityContext;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Reactive {@link ExchangeFilterFunction} that propagates the current thread's Bearer token
 * to outbound WebClient calls.
 *
 * <p><strong>Thread-safety:</strong> The token is captured synchronously from the calling
 * thread before any reactive operator is applied. This ensures the ThreadLocal value is
 * read on the correct (servlet) thread rather than a scheduler thread.
 */
public class WebClientTokenFilter implements ExchangeFilterFunction {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(WebClientTokenFilter.class);

    private final ValidationConfigProperties config;

    /**
     * Constructs the filter with the given configuration.
     *
     * @param config the validation / propagation configuration properties
     */
    public WebClientTokenFilter(ValidationConfigProperties config) {
        this.config = config;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        // SYNC — token is captured on the calling (servlet) thread BEFORE any reactive operator.
        Optional<String> token = resolveToken(request.url());

        if (token.isEmpty()) {
            return next.exchange(request);
        }
        if (request.headers().getFirst(HttpHeaders.AUTHORIZATION) != null) {
            return next.exchange(request);
        }
        ClientRequest outbound = ClientRequest.from(request)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.get())
            .build();
        return next.exchange(outbound);
    }

    private Optional<String> resolveToken(URI targetUri) {
        if (shouldSkipPropagation(targetUri)) {
            return Optional.empty();
        }
        Optional<String> token = SecurityContext.getToken();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        if (SecurityContext.isTokenExpired()) {
            LOGGER.warn("Token is expired; skipping propagation to {}", targetUri);
            return Optional.empty();
        }
        return token;
    }

    private boolean shouldSkipPropagation(URI targetUri) {
        String host = targetUri.getHost();
        if (host == null) {
            return false;
        }
        List<String> excludeHosts = config.getTokenPropagation().getExcludeHosts();
        for (String excluded : excludeHosts) {
            if (host.equalsIgnoreCase(excluded)) {
                return true;
            }
        }
        if (config.getTokenPropagation().isAllowExternalHosts()) {
            return false;
        }
        List<String> includeHosts = config.getTokenPropagation().getIncludeHosts();
        if (!includeHosts.isEmpty()) {
            for (String included : includeHosts) {
                if (host.equalsIgnoreCase(included)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
