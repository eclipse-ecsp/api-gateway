package org.eclipse.ecsp.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enable scheduling for polling fallback.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
