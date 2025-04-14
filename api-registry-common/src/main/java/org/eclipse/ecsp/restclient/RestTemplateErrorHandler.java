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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.register.model.Response;
import org.eclipse.ecsp.utils.ObjectMapperUtil;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

/**
 * Custom error handler for RestTemplate to handle client and server errors.
 */
public class RestTemplateErrorHandler implements ResponseErrorHandler {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RestTemplateErrorHandler.class);
    private final ObjectMapper mapper = ObjectMapperUtil.getObjectMapper();

    /**
     * Checks if the response has an error (4xx or 5xx status codes).
     *
     * @param response the client HTTP response
     * @return true if the response has an error, false otherwise
     * @throws IOException if an I/O error occurs
     */
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
    }

    /**
     * Handles the error response by logging the error details and throwing appropriate exceptions.
     *
     * @param url      the request URI
     * @param method   the HTTP method
     * @param response the client HTTP response
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        String msg = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        Response body = mapper.readValue(msg, Response.class);
        if (body != null && body.getMessage() != null) {
            msg = body.getMessage();
        }
        LOGGER.info("Response url: {}-{} status: {}, body: {}, message: {}",
                method, url, response.getStatusCode(), body, msg);
        if (response.getStatusCode().is5xxServerError()) {
            LOGGER.error("Internal service error for {} - {}, error: {}",
                    method.name(), url.getPath(),
                    ((body != null && body.getMessage() != null) ? body.getMessage() : msg));
            // handle SERVER_ERROR
            throw new IOException("Internal service error, please retry after sometime.");
        } else if (response.getStatusCode().is4xxClientError()) {
            LOGGER.error("Invalid request found for {} - {}, error: {}",
                    method.name(), url.getPath(),
                    ((body != null && body.getMessage() != null) ? body.getMessage() : msg));
            // handle CLIENT_ERROR
            throw new IllegalArgumentException("Invalid request found: "
                    + ((body != null && body.getMessage() != null) ? body.getMessage() : msg));
        }
    }
}
