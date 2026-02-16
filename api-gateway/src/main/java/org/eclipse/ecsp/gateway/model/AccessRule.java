package org.eclipse.ecsp.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing a single access rule (allow or deny).
 *
 * <p>
 * Rules follow the format: [!]service:route
 * - ! prefix indicates a deny rule
 * - * wildcard supported for service and/or route
 *
 * <p>
 * Examples:
 * - Allow all routes in user-service: service="user-service", route="*", deny=false
 * - Deny specific route: service="user-service", route="ban-user", deny=true
 * - Allow all services and routes: service="*", route="*", deny=false
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessRule {

    /**
     * Service name or wildcard (*).
     * Examples: "user-service", "payment-service", "*"
     */
    private String service;

    /**
     * Route path or wildcard (*).
     * Examples: "get-user-profile", "get-*", "*"
     */
    private String route;

    /**
     * Deny flag.
     * true = deny rule (negative rule, prefixed with !)
     * false = allow rule (positive rule, no prefix)
     */
    private boolean deny;

    /**
     * Original rule string for logging/debugging.
     * Examples: "user-service:*", "!payment-service:refund"
     */
    private String originalRule;
}
