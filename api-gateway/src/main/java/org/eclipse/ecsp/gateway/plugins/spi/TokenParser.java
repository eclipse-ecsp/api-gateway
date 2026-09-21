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

import org.springframework.web.server.ServerWebExchange;

/**
 * SPI for extracting a raw (unsigned) token string from the incoming HTTP exchange.
 *
 * <p>The default implementation reads the {@code Authorization} header and strips the
 * {@code Bearer } scheme prefix. Register a Spring bean implementing this interface to
 * override the token-extraction strategy — for example, to read a token from a cookie
 * or a custom header — without modifying any core gateway code.
 *
 * <p>Implementations are injected into {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter}
 * via {@link org.eclipse.ecsp.gateway.plugins.JwtAuthValidator}.
 *
 * <p>If no custom bean is present, the gateway automatically uses
 * {@link org.eclipse.ecsp.gateway.plugins.spi.DefaultTokenParser} via
 * {@code @ConditionalOnMissingBean}.
 */
public interface TokenParser {

    /**
     * Parse and return the raw token string from the incoming exchange.
     *
     * @param exchange the incoming server web exchange
     * @return raw token string without any scheme prefix (e.g., without {@code "Bearer "}),
     *         or {@code null} / blank string if no token is present.
     *         Throw {@link org.eclipse.ecsp.gateway.exceptions.ApiGatewayException}
     *         to reject the request immediately with a specific HTTP status and error code.
     */
    String parse(ServerWebExchange exchange);
}
