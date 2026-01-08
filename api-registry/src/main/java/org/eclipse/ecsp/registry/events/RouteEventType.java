package org.eclipse.ecsp.registry.events;

/**
 * Enum representing the type of route change event that covers all route lifecycle operations.
 */
public enum RouteEventType {
    /**
     * Indicates a route change occurred (create, update, or delete).
     */
    ROUTE_CHANGE, RATE_LIMIT_CONFIG_CHANGE, SERVICE_HEALTH_CHANGE
}
