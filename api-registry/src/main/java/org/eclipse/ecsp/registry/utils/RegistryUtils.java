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

package org.eclipse.ecsp.registry.utils;

import org.springframework.http.HttpStatusCode;

/**
 * RegistryUtils.
 */
public class RegistryUtils {
    /**
     * Private constructor to prevent instantiation.
     */
    private RegistryUtils() {
        // Utility class
    }

    /**
     * Get the outcome from the HTTP status code.
     *
     * @param statusCode the HTTP status code
     * @return the outcome as a string
     */
    public static String getOutcomeFromHttpStatus(HttpStatusCode statusCode) {
        String status = RegistryConstants.UNKNOWN;
        if (statusCode == null) {
            return status;
        }
        if (statusCode.is2xxSuccessful()) {
            status = "SUCCESS";
        } else if (statusCode.is4xxClientError()) {
            status = "CLIENT_ERROR";
        } else if (statusCode.is5xxServerError()) {
            status = "SERVER_ERROR";
        }
        return status;
    }
}
