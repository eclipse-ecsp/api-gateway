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

package org.eclipse.ecsp.security;

import org.eclipse.ecsp.tokenvalidator.model.TokenClaim;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Thread-local holder for JWT-validated security data.
 *
 * <p>Populated exclusively by {@code TokenValidationInterceptor} after successful JWT
 * validation. Completely separate from {@link HeaderContext}, which holds raw HTTP
 * header values that may or may not be JWT-validated.
 *
 * <p>All storage is per-thread. {@link #clear()} must be called in
 * {@code afterCompletion} to prevent ThreadLocal memory leaks in servlet-container
 * thread pools.
 */
public abstract class SecurityContext {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(SecurityContext.class);

    private static final ThreadLocal<SecurityDetails> SECURITY_CONTEXT = new ThreadLocal<>();

    private static final String EXP_CLAIM = "exp";
    private static final String SUB_CLAIM = "sub";
    private static final String SCOPE_CLAIM = "scope";
    private static final String USER_ID_CLAIM = "user_id";
    private static final String TENANT_ID_CLAIM = "tenantId";
    private static final String ACCOUNT_ID_CLAIM = "accountId";
    private static final long MILLIS_PER_SECOND = 1000L;

    /**
     * Private constructor — utility class must not be instantiated.
     */
    private SecurityContext() {
        // Utility class
    }

    /**
     * Populates the current thread's security context from a validated JWT.
     *
     * <p>Parses the {@code exp}, {@code sub}, and {@code scope} claims from
     * {@code claims} and stores the result alongside the raw token string.
     *
     * @param rawToken the Bearer token string (without the "Bearer " prefix)
     * @param claims   the verified claims returned by the token validator
     */
    public static void set(String rawToken, List<TokenClaim> claims) {
        LocalDateTime expiry = parseExpiry(claims);
        String userId = extractClaim(claims, USER_ID_CLAIM);
        if (userId == null) {
            userId = extractClaim(claims, SUB_CLAIM);
        }
        
        Set<String> scopes = parseScopes(claims);
        String tenantId = extractClaim(claims, TENANT_ID_CLAIM);
        String accountId = extractClaim(claims, ACCOUNT_ID_CLAIM);
        LOGGER.debug("Setting security context for userId={}, tenantId={}, accountId={}, scopes={}",
            userId, tenantId, accountId, scopes);
        SECURITY_CONTEXT.set(new SecurityDetails(rawToken, claims, expiry, userId, scopes, tenantId, accountId));
    }

    /**
     * Returns the raw Bearer token for the current thread, if present.
     *
     * @return an {@link Optional} containing the raw token, or empty if no context is set
     */
    public static Optional<String> getToken() {
        SecurityDetails details = SECURITY_CONTEXT.get();
        return details == null ? Optional.empty() : Optional.ofNullable(details.rawToken());
    }

    /**
     * Returns all verified JWT claims for the current thread.
     *
     * @return the claim list, or an empty list if no context is set
     */
    public static List<TokenClaim> getClaims() {
        SecurityDetails details = SECURITY_CONTEXT.get();
        return details == null ? Collections.emptyList() : details.claims();
    }

    /**
     * Returns the user-id (subject) from the validated JWT for the current thread.
     *
     * @return an {@link Optional} containing the user-id, or empty if not present
     */
    public static Optional<String> getUserId() {
        SecurityDetails details = SECURITY_CONTEXT.get();
        return details == null ? Optional.empty() : Optional.ofNullable(details.userId());
    }

    /**
     * Returns the tenant ID from the validated JWT for the current thread.
     *
     * @return an {@link Optional} containing the tenant ID, or empty if not present
     */
    public static Optional<String> getTenantId() {
        SecurityDetails details = SECURITY_CONTEXT.get();
        return details == null ? Optional.empty() : Optional.ofNullable(details.tenantId());
    }

    /**
     * Returns the account ID from the validated JWT for the current thread.
     *
     * @return an {@link Optional} containing the account ID, or empty if not present
     */
    public static Optional<String> getAccountId() {
        SecurityDetails details = SECURITY_CONTEXT.get();
        return details == null ? Optional.empty() : Optional.ofNullable(details.accountId());
    }

    /**
     * Returns the raw scope strings from the validated JWT for the current thread.
     *
     * @return the scope set, or an empty set if no context is set
     */
    public static Set<String> getScopes() {
        SecurityDetails details = SECURITY_CONTEXT.get();
        return details == null ? Collections.emptySet() : details.scopes();
    }

    /**
     * Returns whether the JWT stored in the current thread's context is expired.
     *
     * <p>Returns {@code true} if no context is set (treat absent token as expired).
     *
     * @return {@code true} if the token is expired or absent
     */
    public static boolean isTokenExpired() {
        SecurityDetails details = SECURITY_CONTEXT.get();
        if (details == null || details.expiry() == null) {
            return true;
        }
        return LocalDateTime.now(ZoneOffset.UTC).isAfter(details.expiry());
    }

    /**
     * Clears the security context for the current thread.
     *
     * <p>Must be called in {@code afterCompletion} of the interceptor to prevent
     * ThreadLocal memory leaks in servlet-container thread pools.
     */
    public static void clear() {
        SECURITY_CONTEXT.remove();
    }

    private static LocalDateTime parseExpiry(List<TokenClaim> claims) {
        for (TokenClaim claim : claims) {
            if (EXP_CLAIM.equals(claim.getName())) {
                Object value = claim.getValue();
                if (value instanceof Number num) {
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(num.longValue() * MILLIS_PER_SECOND),
                        ZoneOffset.UTC);
                }

                if (value instanceof Date date) { // NOSONAR
                    return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC); 
                }
                if (value instanceof LocalDateTime date) {
                    return date;
                }
                LOGGER.warn("Unexpected type for 'exp' claim: {}", value == null ? "null" : value.getClass());
                return null;
            }
        }
        return null;
    }

    private static String extractClaim(List<TokenClaim> claims, String claimName) {
        for (TokenClaim claim : claims) {
            if (claimName.equals(claim.getName()) && claim.getValue() != null) {
                return claim.getValue().toString();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> parseScopes(List<TokenClaim> claims) {
        for (TokenClaim claim : claims) {
            if (SCOPE_CLAIM.equals(claim.getName()) && claim.getValue() != null) {
                Object value = claim.getValue();
                if (value instanceof List) {
                    return new HashSet<>((List<String>) value);
                }
                // space-separated string
                String[] parts = value.toString().split(" ");
                Set<String> result = new HashSet<>();
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        result.add(part);
                    }
                }
                return result;
            }
        }
        return Collections.emptySet();
    }

    /**
     * Immutable holder for JWT-validated data associated with a single request thread.
     */
    public static class SecurityDetails {

        private final String rawToken;
        private final List<TokenClaim> claims;
        private final LocalDateTime expiry;
        private final String userId;
        private final Set<String> scopes;
        private final String tenantId;
        private final String accountId;

        /**
         * Constructs a {@code SecurityDetails} instance.
         *
         * @param rawToken the original Bearer token string
         * @param claims   the verified JWT claims
         * @param expiry   the token expiry instant parsed from the {@code exp} claim
         * @param userId   the subject ({@code sub}) claim value
         * @param scopes   the raw scope strings from the JWT
         * @param tenantId the tenant ID from the JWT
         * @param accountId the account ID from the JWT
         */
        public SecurityDetails(String rawToken, List<TokenClaim> claims,
                               LocalDateTime expiry, String userId, Set<String> scopes,
                               String tenantId, String accountId) {
            this.rawToken = rawToken;
            this.claims = claims;
            this.expiry = expiry;
            this.userId = userId;
            this.scopes = scopes;
            this.tenantId = tenantId;
            this.accountId = accountId;
        }

        /**
         * Returns the raw Bearer token string.
         *
         * @return the raw token
         */
        public String rawToken() {
            return rawToken;
        }

        /**
         * Returns the verified JWT claims.
         *
         * @return list of claims
         */
        public List<TokenClaim> claims() {
            return claims;
        }

        /**
         * Returns the token expiry instant.
         *
         * @return the expiry instant, or {@code null} if the {@code exp} claim was absent
         */
        public LocalDateTime expiry() {
            return expiry;
        }

        /**
         * Returns the user-id (subject) from the JWT.
         *
         * @return the userId, or {@code null} if not present
         */
        public String userId() {
            return userId;
        }

        /**
         * Returns the raw scope strings from the JWT.
         *
         * @return the scope set
         */
        public Set<String> scopes() {
            return scopes;
        }

        /**
         * Returns the tenant ID from the JWT.
         *
         * @return the tenant ID, or {@code null} if not present
         */
        public String tenantId() {
            return tenantId;
        }

        /**
         * Returns the account ID from the JWT.
         *
         * @return the account ID, or {@code null} if not present
         */
        public String accountId() {
            return accountId;
        }
    }
}
