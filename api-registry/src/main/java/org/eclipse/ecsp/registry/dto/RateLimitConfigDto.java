package org.eclipse.ecsp.registry.dto;

import java.util.Map;

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
    private String keyResolver;
    private Map<String, String> args;
    private long requestedTokens = 1;
    private Boolean denyEmptyKey = true;
    private String emptyKeyStatus = "400";
}
