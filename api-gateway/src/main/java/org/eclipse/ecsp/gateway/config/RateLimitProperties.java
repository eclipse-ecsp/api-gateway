package org.eclipse.ecsp.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.gateway.model.RateLimit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties controlling rate limiting behaviour for the gateway.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api.gateway.rate-limit")
public class RateLimitProperties {
    private boolean enabled = false;
    private List<RateLimit> overrides;
    private RateLimit defaults;
}
