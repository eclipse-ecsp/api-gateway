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

package org.eclipse.ecsp.restclient;

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * RestTemplateLogInterceptor.
 */
public class RestTemplateLogInterceptor implements ClientHttpRequestInterceptor {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RestTemplateLogInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        logRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
        logResponse(response);
        return response;
    }

    private void logRequest(HttpRequest request, byte[] body) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("===========================REST request begin================================");
            LOGGER.debug("URI         : {}", request.getURI());
            LOGGER.debug("Method      : {}", request.getMethod());
            LOGGER.debug("Headers     : {}", request.getHeaders());
            LOGGER.debug("Request body: {}", new String(body, StandardCharsets.UTF_8));
            LOGGER.debug("===========================REST request end==================================");
        }
    }

    private void logResponse(ClientHttpResponse response) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("===========================REST response begin===============================");
            LOGGER.debug("Status code  : {}", response.getStatusCode());
            LOGGER.debug("Status text  : {}", response.getStatusText());
            LOGGER.debug("Headers      : {}", response.getHeaders());
            LOGGER.debug("===========================REST response end=================================");
        }
    }

}