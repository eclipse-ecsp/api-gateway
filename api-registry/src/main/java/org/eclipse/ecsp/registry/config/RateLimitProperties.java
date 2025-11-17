package org.eclipse.ecsp.registry.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for rate limiting settings.
 */
@ConfigurationProperties(prefix = "api-registry.rate-limit")
@Configuration
@Getter
@Setter
public class RateLimitProperties {
    private int maxReplenishRate = 10000;
    private int maxBurstCapacity = 10000;
    private int maxRequestedTokens = 100;
    private List<String> keyResolvers = new ArrayList<>();
}
