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

import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * {@link ClientHttpRequestInterceptor} for {@code RestClient} that propagates the
 * current thread's Bearer token to downstream service calls.
 *
 * <p>Separate from {@link RestTemplateTokenInterceptor} to allow independent
 * bean-lifecycle management for the {@code RestClient} integration.
 */
public class RestClientTokenInterceptor extends AbstractTokenPropagationInterceptor
    implements ClientHttpRequestInterceptor {
    
    private static final IgniteLogger LOGGER =
        IgniteLoggerFactory.getLogger(RestClientTokenInterceptor.class);

    /**
     * Constructs an interceptor with the given configuration.
     *
     * @param config the validation / propagation configuration properties
     */
    public RestClientTokenInterceptor(ValidationConfigProperties config) {
        super(config);
    }

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
}
