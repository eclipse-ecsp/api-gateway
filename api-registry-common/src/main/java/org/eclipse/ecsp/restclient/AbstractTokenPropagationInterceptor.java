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
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Base class for token-propagation interceptors and filters.
 *
 * <p>Provides shared host-classification logic and token-retrieval from
 * {@link SecurityContext}. Subclasses implement the HTTP-client-specific
 * {@code intercept} / {@code filter} method.
 *
 * <p><strong>Thread-safety note:</strong> {@link #resolveToken(URI)} reads from the
 * {@link SecurityContext} ThreadLocal. For {@code RestTemplate} and {@code RestClient}
 * interceptors this is always called on the same servlet thread as the original request,
 * so no additional synchronisation is needed.
 */
public abstract class AbstractTokenPropagationInterceptor {

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
        // If allow-external-hosts is false, only forward when host is in include-hosts
        List<String> includeHosts = config.getTokenPropagation().getIncludeHosts();
        if (!includeHosts.isEmpty()) {
            for (String included : includeHosts) {
                if (host.equalsIgnoreCase(included)) {
                    return false;
                }
            }
            return true;
        }
        // No include-hosts restriction — allow all non-excluded hosts
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
            return Optional.empty();
        }
        if (SecurityContext.isTokenExpired()) {
            LOGGER.warn("Token is expired; skipping propagation to {}", targetUri);
            return Optional.empty();
        }
        return token;
    }
}
