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
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.model.TokenHeaderValidationConfig;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link TokenClaimValidator}.
 *
 * <p>Validates token claims against configured required/regex rules from
 * {@link org.eclipse.ecsp.gateway.model.TokenHeaderValidationConfig}.
 * Replicates the {@code validateTokenHeaders()} and {@code validateClaims()} logic
 * previously embedded in
 * {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter}.
 *
 * <p>Includes the compiled regex pattern cache for performance (avoids recompiling
 * the same pattern on every request).
 *
 * <p>This bean is registered only when no other {@link TokenClaimValidator} bean is
 * present in the Spring application context ({@code @ConditionalOnMissingBean}).
 */
public class DefaultTokenClaimValidator implements TokenClaimValidator {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(DefaultTokenClaimValidator.class);
    private static final String INVALID_TOKEN_CODE = "api.gateway.error.token.invalid";
    private static final String INVALID_TOKEN = "Invalid Token";
    private static final String TOKEN_VERIFICATION_FAILED = "Token verification failed";

    /**
     * Cache for compiled regex patterns. Pattern.compile() is expensive — reuse compiled
     * patterns across requests for performance.
     */
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    @Override
    public void validate(Claims claims,
                         boolean skipClaimValidation,
                         Map<String, TokenHeaderValidationConfig> validationConfig) {
        if (skipClaimValidation) {
            LOGGER.debug("Skipping token claim validation (skip-claim-validation=true).");
            return;
        }

        if (CollectionUtils.isEmpty(validationConfig)) {
            LOGGER.debug("No token header validation config present, skipping claim validation.");
            return;
        }

        try {
            for (Map.Entry<String, TokenHeaderValidationConfig> entry : validationConfig.entrySet()) {
                String claimName = entry.getKey();
                TokenHeaderValidationConfig config = entry.getValue();
                LOGGER.debug("Validating token claim: {}", claimName);
                validateSingleClaim(claims, claimName, config);
                LOGGER.debug("Token claim validation passed for: {}", claimName);
            }
        } catch (PatternSyntaxException regexException) {
            LOGGER.error("Token claim validation failed - invalid regex pattern: {}", regexException.getMessage());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        } catch (ApiGatewayException e) {
            throw e;
        } catch (Exception ex) {
            LOGGER.error("Token claim validation failed with unexpected error: {}", ex.getMessage());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        }
    }

    private void validateSingleClaim(Claims claims, String claimName, TokenHeaderValidationConfig config) {
        if (!config.isRequired()) {
            LOGGER.debug("Claim '{}' is not required, skipping.", claimName);
            return;
        }

        String matchedKey = getClaimKey(claims, claimName);
        String claimValue = getClaimValue(claims, matchedKey);

        if (StringUtils.isEmpty(matchedKey) || StringUtils.isEmpty(claimValue)) {
            LOGGER.error("Token claim validation failed - required claim '{}' is missing.", claimName);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }

        if (StringUtils.isNotEmpty(config.getRegex())) {
            Pattern pattern = PATTERN_CACHE.computeIfAbsent(config.getRegex(), Pattern::compile);
            if (!pattern.matcher(claimValue).matches()) {
                LOGGER.error("Token claim validation failed - claim '{}' value '{}' does not match regex '{}'.",
                        claimName, claimValue, config.getRegex());
                throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
            }
        }
    }

    private static String getClaimKey(Claims claims, String claimName) {
        if (claims == null) {
            return null;
        }
        return claims.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(claimName))
                .findAny()
                .orElse(null);
    }

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
