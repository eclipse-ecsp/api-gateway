package org.eclipse.ecsp.gateway.utils;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service for extracting client IDs from JWT claims.
 *
 * <p>Supports configurable claim name chain with case-insensitive matching.
 * Extracts the first non-empty claim value from the configured chain.
 * Claims are read without signature verification since the gateway trusts upstream auth.
 *
 * @see org.eclipse.ecsp.gateway.config.ClientAccessControlProperties
 */
public class JwtUtils {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(JwtUtils.class);

    private JwtUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Extract client ID from JWT token using configured claim names.
     * Works for both signed (JWS) and unsecured JWTs without verifying the signature,
     * since the gateway trusts that upstream authentication has already validated the token.
     *
     * @param jwt JWT token string (without "Bearer " prefix)
     * @param claimNames Ordered list of claim names to try
     * @return Client ID if found, null otherwise
     */
    public static String extractClientId(String jwt, List<String> claimNames) {
        if (jwt == null || jwt.isBlank()) {
            LOGGER.debug("JWT token is null or empty");
            return null;
        }

        if (claimNames == null || claimNames.isEmpty()) {
            LOGGER.warn("No claim names configured for client ID extraction");
            return null;
        }

        try {
            // Parse JWT and read claims without signature verification
            // (gateway trusts upstream auth; Nimbus allows claim extraction for any JWT type)
            JWTClaimsSet claims = JWTParser.parse(jwt).getJWTClaimsSet();

            // Try each claim name in order (case-insensitive)
            for (String claimName : claimNames) {
                String clientId = extractClaimCaseInsensitive(claims, claimName);
                if (clientId != null && !clientId.isBlank()) {
                    LOGGER.debug("Extracted client ID '{}' from claim '{}'", clientId, claimName);
                    return clientId.trim();
                }
            }

            LOGGER.warn("No client ID found in JWT claims. Tried claim names: {} - "
                    + "This may indicate misconfigured claim mapping", claimNames);
            return null;

        } catch (Exception e) {
            LOGGER.warn("Failed to parse JWT token - malformed token: {} - "
                    + "Request will be denied", e.getMessage());
            return null;
        }
    }

    /**
     * Extract claim value with case-insensitive name matching.
     * If the claim value is a collection (JSON array), the first element is returned.
     *
     * @param claims JWT claims
     * @param claimName Target claim name (case-insensitive)
     * @return Claim value as string, or null if not found
     */
    private static String extractClaimCaseInsensitive(JWTClaimsSet claims, String claimName) {
        if (claims == null || claimName == null) {
            return null;
        }

        // Try exact match first (optimization)
        Object exactValue = claims.getClaim(claimName);
        if (exactValue != null) {
            return claimValueToString(exactValue);
        }

        // Try case-insensitive match
        String lowerClaimName = claimName.toLowerCase();
        for (Map.Entry<String, Object> entry : claims.getClaims().entrySet()) {
            if (entry.getKey().toLowerCase().equals(lowerClaimName)) {
                return claimValueToString(entry.getValue());
            }
        }

        return null;
    }

    /**
     * Convert a claim value to a string.
     * If the value is a collection (JSON array), the first non-null element is returned.
     *
     * @param value Raw claim value
     * @return String representation, or null
     */
    private static String claimValueToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(item -> item != null && !item.toString().isBlank())
                    .map(Object::toString)
                    .findFirst()
                    .orElse(null);
        }
        return value.toString();
    }
}
