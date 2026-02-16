package org.eclipse.ecsp.registry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson ObjectMapper configuration for JSON serialization/deserialization.
 *
 * <p>
 * Configures:
 * - Java 8 time module for OffsetDateTime/Instant handling
 * - JSONB serialization for PostgreSQL JSONB columns
 * - Pretty printing for development/debugging
 */
@Configuration
public class JacksonConfig {

    /**
     * Configure Jackson ObjectMapper bean.
     *
     * <p>
     * Enables:
     * - JavaTimeModule for java.time.* types (OffsetDateTime, Instant)
     * - Proper timezone handling (UTC)
     * - JSONB column serialization support
     *
     * @return Configured ObjectMapper instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register Java 8 time module for OffsetDateTime/Instant
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps (use ISO-8601 strings instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Pretty print for readability (can be disabled in production)
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        return mapper;
    }
}
