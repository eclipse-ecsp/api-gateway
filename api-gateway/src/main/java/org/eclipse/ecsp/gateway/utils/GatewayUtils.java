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

package org.eclipse.ecsp.gateway.utils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * GatewayUtils.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GatewayUtils {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(GatewayUtils.class);

    /**
     * Get the outcome from the HTTP status code.
     *
     * @param statusCode the HTTP status code
     * @return the outcome as a string
     */
    public static String getOutcomeFromHttpStatus(HttpStatusCode statusCode) {
        String status = GatewayConstants.UNKNOWN;
        if (statusCode == null) {
            return status;
        }
        if (statusCode.is2xxSuccessful()) {
            status = "SUCCESS";
        } else if (statusCode.is4xxClientError()) {
            status = "CLIENT_ERROR";
        } else if (statusCode.is5xxServerError()) {
            status = "SERVER_ERROR";
        } else if (statusCode.isError()) {
            status = "ERROR";
        }
        return status;
    }

    /**
     * Helper method to extract client IP address from the request.
     *
     * @param request the ServerHttpRequest
     * @return client IP address
     */
    public static String getClientIpAddress(ServerHttpRequest request) {
        String clientIp = "unknown";
        try {
            // Check X-Forwarded-For header first (common in proxy setups)
            String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
            if (StringUtils.isNotEmpty(forwardedFor)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                clientIp = forwardedFor.split(",")[0].trim();
            } else {
                // Fallback to remote address
                if (request.getRemoteAddress() != null 
                        && request.getRemoteAddress().getAddress() != null) {
                    clientIp = request.getRemoteAddress().getAddress().getHostAddress();
                }
            }
        } catch (Exception ex) {
            LOGGER.debug("Unable to extract client IP address: {}", ex.getMessage());
        }
        return clientIp;
    }

    /**
     * Helper method to determine the specific reason for token validation failure.
     *
     * @param ex the exception thrown during token validation
     * @return a human-readable failure reason
     */
    public static String getTokenValidationFailureReason(Exception ex) {
        if (ex instanceof ExpiredJwtException) {
            return "Token expired";
        } else if (ex instanceof MalformedJwtException) {
            return "Malformed token";
        } else if (ex instanceof UnsupportedJwtException) {
            return "Unsupported token format";
        } else if (ex instanceof SecurityException) {
            return "Token signature verification failed";
        } else if (ex instanceof IllegalArgumentException) {
            return "Invalid token argument";
        } else {
            return "Unknown token validation error";
        }
    }
}
