package org.eclipse.ecsp.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Domain model representing client access configuration in the gateway cache.
 *
 * <p>This is a gateway-side representation optimized for fast rule validation.
 * Cached in ConcurrentHashMap for O(1) lookup by clientId.
 *
 * @see AccessRule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientAccessControlConfigDto {
    private String clientId;
    private String tenant;
    private String description;
    private boolean active;
    private List<String> allow;
}
