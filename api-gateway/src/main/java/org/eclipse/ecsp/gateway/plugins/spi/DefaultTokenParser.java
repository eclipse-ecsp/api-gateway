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

package org.eclipse.ecsp.gateway.plugins.spi;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

/**
 * Default implementation of {@link TokenParser}.
 *
 * <p>Extracts the bearer token from the HTTP {@code Authorization} header.
 * Replicates the token-extraction logic previously embedded in
 * {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter#filter}.
 *
 * <p>This bean is registered only when no other {@link TokenParser} bean is present
 * in the Spring application context ({@code @ConditionalOnMissingBean}).
 */
public class DefaultTokenParser implements TokenParser {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(DefaultTokenParser.class);
    private static final String INVALID_TOKEN_CODE = "api.gateway.error.token.invalid";
    private static final String INVALID_TOKEN = "Invalid Token";

    @Override
    public String parse(ServerWebExchange exchange) {
        String requestPath = exchange.getRequest().getPath().value();
        String requestId = exchange.getRequest().getId();

        String authHeader = exchange.getRequest().getHeaders().getFirst(GatewayConstants.AUTHORIZATION);

        // Fallback for WebSockets: Check Sec-WebSocket-Protocol header
        if (StringUtils.isBlank(authHeader) || !authHeader.startsWith(GatewayConstants.BEARER)) {
            String upgradeHeader = exchange.getRequest().getHeaders().getFirst("Upgrade");
            if ("websocket".equalsIgnoreCase(upgradeHeader)) {
                java.util.List<String> protocols = exchange.getRequest().getHeaders().get("Sec-WebSocket-Protocol");
                if (protocols != null) {
                    for (String protocolHeader : protocols) {
                        for (String protocol : protocolHeader.split(",")) {
                            String trimmed = protocol.trim();
                            // A JWT has 2 dots (header.payload.signature)
                            if (trimmed.split("\\.").length == 3) {
                                LOGGER.debug("Token extracted from Sec-WebSocket-Protocol header for requestUrl: {}, requestId: {}", requestPath, requestId);
                                return trimmed;
                            }
                        }
                    }
                }
            }
            
            LOGGER.error("Token validation failed - Token missing or invalid format. "
                    + "requestUrl: {}, requestId: {}", requestPath, requestId);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }

        String rawToken = authHeader.split(" ")[1];
        LOGGER.debug("Token extracted from Authorization header for requestUrl: {}, requestId: {}",
                requestPath, requestId);
        return rawToken;
    }
}
