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

package org.eclipse.ecsp.gateway.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * ApiGatewayException class to wrap exception with errorCode and message.
 */
@Getter
public class ApiGatewayException extends RuntimeException {
    /**
     * Error code associated with the exception.
     */
    private final String errorCode;

    /**
     * Message describing the exception.
     */
    private final String message;

    /**
     * HTTP status code associated with the exception.
     */
    private final HttpStatusCode statusCode;

    /**
     * Constructs a new ApiGatewayException with the specified status code, error code, and message.
     *
     * @param statusCode the HTTP status code associated with the exception
     * @param errorCode  the error code associated with the exception
     * @param message    the message describing the exception
     */
    public ApiGatewayException(HttpStatusCode statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.message = message;
    }
}