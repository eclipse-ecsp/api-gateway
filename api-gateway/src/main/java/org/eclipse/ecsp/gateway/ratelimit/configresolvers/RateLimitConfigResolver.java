package org.eclipse.ecsp.gateway.ratelimit.configresolvers;

import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.model.RateLimit;

/**
 * Resolver for determining rate limit configurations for routes.
 *
 * @author Abhishek Kumar
 */
public interface RateLimitConfigResolver {
    /**
     * Resolves the rate limit configuration for the given route.
     *
     * @param route the route definition
     * @return the applicable rate limit configuration
     */
    RateLimit resolveRateLimit(IgniteRouteDefinition route);
}
