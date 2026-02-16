package org.eclipse.ecsp.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for extracting client IDs from JWT claims.
 *
 * <p>
 * Supports configurable claim name chain with case-insensitive matching.
 * Extracts the first non-empty claim value from the configured chain.
 *
 * @see org.eclipse.ecsp.gateway.config.ClientAccessControlProperties
 */
@Service
@Slf4j
public class JwtClaimExtractor {

    /**
     * Extract client ID from JWT token using configured claim names.
     *
     * @param jwt JWT token string (without "Bearer " prefix)
     * @param claimNames Ordered list of claim names to try
     * @return Client ID if found, null otherwise
     */
    public String extractClientId(String jwt, List<String> claimNames) {
        if (jwt == null || jwt.isBlank()) {
            log.debug("JWT token is null or empty");
            return null;
        }

        if (claimNames == null || claimNames.isEmpty()) {
            log.warn("No claim names configured for client ID extraction");
            return null;
        }

        try {
            // Parse JWT claims without signature verification (gateway trusts upstream auth)
            Claims claims = Jwts.parser()
                    .unsecured()
                    .build()
                    .parseUnsecuredClaims(jwt)
                    .getPayload();

            // Try each claim name in order (case-insensitive)
            for (String claimName : claimNames) {
                String clientId = extractClaimCaseInsensitive(claims, claimName);
                if (clientId != null && !clientId.isBlank()) {
                    log.debug("Extracted client ID '{}' from claim '{}'", clientId, claimName);
                    return clientId.trim();
                }
            }

            log.warn("No client ID found in JWT claims. Tried claim names: {} - "
                    + "This may indicate misconfigured claim mapping", claimNames);
            return null;

        } catch (Exception e) {
            log.warn("Failed to parse JWT token - malformed or invalid signature: {} - "
                    + "Request will be denied", e.getMessage());
            return null;
        }
    }

    /**
     * Extract claim value with case-insensitive name matching.
     *
     * @param claims JWT claims
     * @param claimName Target claim name (case-insensitive)
     * @return Claim value as string, or null if not found
     */
    private String extractClaimCaseInsensitive(Claims claims, String claimName) {
        if (claims == null || claimName == null) {
            return null;
        }

        // Try exact match first (optimization)
        Object exactValue = claims.get(claimName);
        if (exactValue != null) {
            return exactValue.toString();
        }

        // Try case-insensitive match
        String lowerClaimName = claimName.toLowerCase();
        for (String key : claims.keySet()) {
            if (key.toLowerCase().equals(lowerClaimName)) {
                Object value = claims.get(key);
                return value != null ? value.toString() : null;
            }
        }

        return null;
    }
}
