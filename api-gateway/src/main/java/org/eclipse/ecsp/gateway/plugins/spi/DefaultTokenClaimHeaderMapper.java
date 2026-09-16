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

import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link TokenClaimHeaderMapper}.
 *
 * <p>Replicates the downstream header-writing logic previously embedded in
 * {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter#filter}, specifically:
 * <ul>
 *   <li>Writing the {@code scope} header with the effective user scope string</li>
 *   <li>Writing the {@code override-scope} header with the union of user and route scopes</li>
 *   <li>Iterating the configured {@code claimToHeaderMapping} and adding each
 *       non-blank claim value as a request header</li>
 * </ul>
 *
 * <p>This bean is registered only when no other {@link TokenClaimHeaderMapper} bean is
 * present in the Spring application context ({@code @ConditionalOnMissingBean}).
 */
public class DefaultTokenClaimHeaderMapper implements TokenClaimHeaderMapper {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(DefaultTokenClaimHeaderMapper.class);

    @Override
    public ServerHttpRequest.Builder map(ServerHttpRequest.Builder builder,
                                         ClaimHeaderMappingContext context) {
        // Write scope and override-scope headers
        builder.header(GatewayConstants.SCOPE, context.getScopeString());
        builder.header(GatewayConstants.OVERRIDE_SCOPE, String.join(",", context.getOverrideScopes()));

        LOGGER.debug("Added scope header: {}, override-scope header: {}",
                context.getScopeString(), String.join(",", context.getOverrideScopes()));

        // Write claim-to-header mapped values
        Claims claims = context.getClaims();
        for (Map.Entry<String, String> entry : context.getClaimToHeaderMapping().entrySet()) {
            String claimKey = entry.getKey();
            String headerName = entry.getValue();
            String claimValue = getClaimValue(claims, claimKey);
            if (!StringUtils.isBlank(claimValue)) {
                builder.header(headerName, claimValue);
                LOGGER.debug("Mapped claim '{}' -> header '{}' with value: '{}'", claimKey, headerName, claimValue);
            }
        }

        return builder;
    }

    /**
     * Extract a claim value from Claims, converting collections/arrays to comma-separated strings.
     */
    private static String getClaimValue(Claims claims, String claimKey) {
        if (claimKey == null || claims == null || claims.get(claimKey) == null) {
            return null;
        }
        Object value = claims.get(claimKey);
        return switch (value) {
            case List<?> list -> list.stream().map(Object::toString).collect(Collectors.joining(","));
            case String[] arr -> String.join(",", arr);
            case Set<?> set -> set.stream().map(Object::toString).collect(Collectors.joining(","));
            case String str -> str;
            default -> String.valueOf(value);
        };
    }
}
