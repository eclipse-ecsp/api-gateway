package org.eclipse.ecsp.registry.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Event DTO for Redis Pub/Sub notifications.
 *
 * <p>
 * Published to Redis channel on CLIENT_ACCESS_CONTROL_UPDATED events.
 * Gateway subscribes to this event to refresh its cache immediately.
 *
 * @see <a href="data-model.md">Data Model Documentation - Redis Event Model</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientAccessControlEventDto {

    /**
     * Unique event identifier for deduplication.
     * UUID string format.
     */
    @JsonProperty("eventId")
    private String eventId;

    /**
     * Event generation timestamp.
     * ISO 8601 format.
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Event type.
     * Always "CLIENT_ACCESS_CONTROL_UPDATED" for this feature.
     */
    @JsonProperty("eventType")
    private String eventType;

    /**
     * Operation type.
     * Values: "CREATE", "UPDATE", "DELETE"
     */
    @JsonProperty("operation")
    private String operation;

    /**
     * Modified client IDs.
     * Array of client identifiers affected by this change.
     */
    @JsonProperty("clientIds")
    private List<String> clientIds;

    /**
     * Affected services.
     * Array of service names (empty if route-specific change).
     */
    @JsonProperty("services")
    private List<String> services;

    /**
     * Affected routes.
     * Array of route paths (empty if service-specific change).
     */
    @JsonProperty("routes")
    private List<String> routes;
}
