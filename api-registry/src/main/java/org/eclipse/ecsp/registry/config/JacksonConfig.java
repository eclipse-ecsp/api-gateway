/********************************************************************************
 * Copyright (c) 2023-24 Harman International
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and\
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.registry.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper configuration for JSON serialization/deserialization.
 *
 * <p>Configures:
 * - Java 8 time module for LocalDateTime/Instant handling
 * - JSONB serialization for PostgreSQL JSONB columns
 */
@Configuration
public class JacksonConfig {
    /**
     * Default constructor.
     */
    public JacksonConfig() {
        // Default constructor
    }

    /**
     * Configure Jackson ObjectMapper bean.
     *
     * <p>Enables:
      * - JavaTimeModule for java.time.* types (LocalDateTime, Instant)
     * - Proper timezone handling (UTC)
     * - JSONB column serialization support
     *
     * @return Configured Jackson2ObjectMapperBuilderCustomizer instance
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer objectMapper() {
        return builder -> {
            builder.modules(new JavaTimeModule());
            builder.featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
