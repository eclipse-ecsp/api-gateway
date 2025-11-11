package org.eclipse.ecsp.registry.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for Rate Limit Configuration.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RateLimitConfigDto {
    private String routeId;
    private String service;
    private long replenishRate;
    private long burstCapacity;
    private boolean includeHeaders = true;
    private RateLimitType rateLimitType;
    private String headerName;

    /**
     * Enumeration for Rate Limit Types.
     */
    public enum RateLimitType {
        CLIENT_IP,
        HEADER,
        ROUTE_PATH,
        ROUTE_NAME
    }
}
