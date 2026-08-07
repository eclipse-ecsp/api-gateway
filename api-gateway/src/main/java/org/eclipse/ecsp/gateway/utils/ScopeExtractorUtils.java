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

package org.eclipse.ecsp.gateway.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for extracting required authorization scopes from the JWT Token representations.
 *
 * @author Abhishek Kumar
 */
public abstract class ScopeExtractorUtils {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ScopeExtractorUtils.class);
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperUtil.getObjectMapper();

    private ScopeExtractorUtils() {
    }

    /**
     * Extracts scopes based on the configuration or defaults to "scope".
     *
     * @param claims the token claims a java.util.Map or io.jsonwebtoken.Claims containing the user scopes.
     * @param jwtProperties to determine priority configured claim to base extraction.
     * @return an extracted Set of required configured Scope.
     */
    public static Set<String> extractScopes(java.util.Map<String, Object> claims, JwtProperties jwtProperties) {
        List<String> scopeClaims = getScopeClaims(jwtProperties);

        Object claim = null;
        for (String claimName : scopeClaims) {
            claim = claims.get(claimName);
            if (claim != null) {
                break;
            }
        }
        
        if (claim == null) {
            return Collections.emptySet();
        }

        Set<String> extractedScopes = new HashSet<>();
        parseClaim(claim, extractedScopes);

        return extractedScopes.stream()
                .filter(Objects::nonNull)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private static List<String> getScopeClaims(JwtProperties jwtProperties) {
        List<String> scopeClaims = Arrays.asList("scope");
        if (jwtProperties != null && jwtProperties.getScopeClaims() != null 
                && !jwtProperties.getScopeClaims().isEmpty()) {
            
            boolean allBlank = true;
            for (String claimName : jwtProperties.getScopeClaims()) {
                if (StringUtils.isNotBlank(claimName)) {
                    allBlank = false;
                    break;
                }
            }
            if (!allBlank) {
                scopeClaims = jwtProperties.getScopeClaims();
            }
        }
        return scopeClaims;
    }

    private static void parseClaim(Object claim, Set<String> extractedScopes) {
        if (claim instanceof Collection) {
            for (Object obj : (Collection<?>) claim) {
                if (obj instanceof String strScope) {
                    parseStringClaim(strScope, extractedScopes);
                }
            }
        } else if (claim instanceof String stringClaim) {
            parseStringClaim(stringClaim, extractedScopes);
        } else {
            LOGGER.debug("Unsupported scope claim type: {}", claim.getClass().getSimpleName());
        }
    }

    private static void parseStringClaim(String stringClaim, Set<String> extractedScopes) {
        if (stringClaim.trim().startsWith("[")) {
            try {
                Collection<String> parsedCollection = OBJECT_MAPPER.readValue(stringClaim,
                        OBJECT_MAPPER.getTypeFactory().constructCollectionType(Collection.class, String.class));
                extractedScopes.addAll(parsedCollection);
            } catch (JsonProcessingException e) {
                LOGGER.warn("Failed to parse JSON string array scope claim", e);
            }
        } else {
            String delimiter = stringClaim.contains(",") ? "," : "\\s+";
            String[] parts = stringClaim.split(delimiter);
            Collections.addAll(extractedScopes, parts);
        }
    }
}
