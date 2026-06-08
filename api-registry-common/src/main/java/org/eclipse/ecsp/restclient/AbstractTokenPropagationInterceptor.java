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

import org.eclipse.ecsp.security.SecurityContext;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Base class for token-propagation interceptors and filters.
 *
 * <p>Provides shared host-classification logic, token-retrieval from
 * {@link SecurityContext}, and the concrete {@link #intercept} implementation of
 * {@link org.springframework.http.client.ClientHttpRequestInterceptor}. Subclasses used
 * for {@code RestTemplate} and {@code RestClient} inherit this behaviour without
 * duplication; a reactive ({@code WebClient}) subclass may override it instead.
 *
 * <p><strong>Thread-safety note:</strong> {@link #resolveToken(URI)} reads from the
 * {@link SecurityContext} ThreadLocal. For {@code RestTemplate} and {@code RestClient}
 * interceptors this is always called on the same servlet thread as the original request,
 * so no additional synchronisation is needed.
 */
public abstract class AbstractTokenPropagationInterceptor
    implements ClientHttpRequestInterceptor {

    private static final IgniteLogger LOGGER =
        IgniteLoggerFactory.getLogger(AbstractTokenPropagationInterceptor.class);

    private final ValidationConfigProperties config;

    /**
     * Constructs an interceptor with the given configuration.
     *
     * @param config the validation / propagation configuration properties
     */
    protected AbstractTokenPropagationInterceptor(ValidationConfigProperties config) {
        this.config = config;
    }

    /**
     * Propagates the current thread's Bearer token to the outgoing request.
     *
     * <p>Skips propagation when:
     * <ul>
     *   <li>an {@code Authorization} header is already present on the request, or</li>
     *   <li>no valid (non-expired) token is found in {@link SecurityContext}, or</li>
     *   <li>{@link #shouldSkipPropagation(URI)} returns {@code true}.</li>
     * </ul>
     *
     * @param request   the outgoing HTTP request
     * @param body      the request body bytes
     * @param execution the request execution chain
     * @return the response from the downstream service
     * @throws IOException if the execution fails
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        if (request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION) != null) {
            LOGGER.debug("Request to {} already has Authorization header, skipping token propagation",
                request.getURI());
            return execution.execute(request, body);
        }
        Optional<String> token = resolveToken(request.getURI());
        if (token.isEmpty() || shouldSkipPropagation(request.getURI())) {
            LOGGER.debug("No token available or propagation skipped for request to {}", request.getURI());
            return execution.execute(request, body);
        }
        LOGGER.debug("Propagating token to request to {}", request.getURI());
        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token.get());
        return execution.execute(request, body);
    }

    /**
     * Returns {@code true} when the Bearer token must NOT be forwarded to the given target.
     *
     * <p>Token propagation is skipped when:
     * <ul>
     *   <li>the host appears in {@code exclude-hosts}, or</li>
     *   <li>the host is external AND {@code allow-external-hosts=false} AND the host is
     *       not in {@code include-hosts}</li>
     * </ul>
     *
     * @param targetUri the URI of the downstream service
     * @return {@code true} if propagation should be skipped
     */
    protected boolean shouldSkipPropagation(URI targetUri) {
        String host = targetUri.getHost();
        if (host == null) {
            LOGGER.warn("Target URI {} does not have a host; skipping token propagation", targetUri);
            return false;
        }
        List<String> excludeHosts = config.getTokenPropagation().getExcludeHosts();
        for (String excluded : excludeHosts) {
            if (host.equalsIgnoreCase(excluded)) {
                LOGGER.debug("Host {} is in exclude-hosts; skipping token propagation", host);
                return true;
            }
        }
        if (config.getTokenPropagation().isAllowExternalHosts()) {
            LOGGER.debug("allow-external-hosts is true; allowing token propagation to {}", host);
            return false;
        }
        // If allow-external-hosts is false, only forward when host is in include-hosts
        List<String> includeHosts = config.getTokenPropagation().getIncludeHosts();
        if (!includeHosts.isEmpty()) {
            for (String included : includeHosts) {
                if (host.equalsIgnoreCase(included)) {
                    LOGGER.debug("Host {} is in include-hosts; allowing token propagation", host);
                    return false;
                }
            }
            LOGGER.debug("Host {} is not in include-hosts; skipping token propagation", host);
            return true;
        }
        // No include-hosts restriction — allow all non-excluded hosts
        LOGGER.debug("No include-hosts restriction; allowing token propagation to {}", host);
        return false;
    }

    /**
     * Retrieves the current Bearer token from {@link SecurityContext}.
     *
     * <p>Returns {@link Optional#empty()} and logs a warning if the token is expired.
     *
     * @param targetUri the URI of the downstream service (used only for logging)
     * @return the token, or empty if absent or expired
     */
    protected Optional<String> resolveToken(URI targetUri) {
        Optional<String> token = SecurityContext.getToken();
        if (token.isEmpty()) {
            LOGGER.warn("No token found in SecurityContext; skipping propagation to {}", targetUri);
            return Optional.empty();
        }
        if (SecurityContext.isTokenExpired()) {
            LOGGER.warn("Token is expired; skipping propagation to {}", targetUri);
            return Optional.empty();
        }
        LOGGER.debug("Token is valid; allowing propagation to {}", targetUri);
        return token;
    }
}
