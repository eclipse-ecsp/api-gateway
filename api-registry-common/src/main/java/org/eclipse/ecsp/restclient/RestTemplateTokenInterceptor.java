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

/**
 * {@link org.springframework.http.client.ClientHttpRequestInterceptor} for {@code RestTemplate} that propagates the
 * current thread's Bearer token to downstream service calls.
 *
 * <p>The {@code intercept} method is inherited from
 * {@link AbstractTokenPropagationInterceptor}.
 */
public class RestTemplateTokenInterceptor extends AbstractTokenPropagationInterceptor {

    /**
     * Constructs an interceptor with the given configuration.
     *
     * @param config the validation / propagation configuration properties
     */
    public RestTemplateTokenInterceptor(ValidationConfigProperties config) {
        super(config);
    }
}
