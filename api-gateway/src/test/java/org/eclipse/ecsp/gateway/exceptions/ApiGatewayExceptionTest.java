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

package org.eclipse.ecsp.gateway.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiGatewayExceptionTest {

    @Test
    void testConstructorWithoutCause() {
        ApiGatewayException ex = new ApiGatewayException(
                HttpStatus.BAD_REQUEST, "api.error.bad_request", "Invalid request parameters");

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        Assertions.assertEquals("api.error.bad_request", ex.getErrorCode());
        Assertions.assertEquals("Invalid request parameters", ex.getMessage());
        Assertions.assertNull(ex.getCause());
    }

    @Test
    void testConstructorWithCause() {
        IllegalStateException cause = new IllegalStateException("Original cause error");
        ApiGatewayException ex = new ApiGatewayException(
                HttpStatus.INTERNAL_SERVER_ERROR, "api.error.internal", "Internal error occurred", cause);

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        Assertions.assertEquals("api.error.internal", ex.getErrorCode());
        Assertions.assertEquals("Internal error occurred", ex.getMessage());
        Assertions.assertNotNull(ex.getCause());
        Assertions.assertSame(cause, ex.getCause());
        Assertions.assertEquals("Original cause error", ex.getCause().getMessage());
    }
}
