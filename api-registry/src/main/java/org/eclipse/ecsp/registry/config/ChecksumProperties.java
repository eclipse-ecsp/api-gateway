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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.registry.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for checksum-based route change detection.
 *
 * <p>Bound from prefix {@code api.registry.change-detection}. When {@link #enabled} is
 * {@code false}, all change-detection logic is bypassed and the original
 * save-always behaviour is restored (FR-08).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api.registry.change-detection")
public class ChecksumProperties {

    /**
     * Default constructor.
     */
    public ChecksumProperties() {
        // Default constructor
    }

    /**
     * Master switch. When {@code false}, change detection is skipped (FR-08).
     */
    private boolean enabled = true;

    /**
     * Checksum sub-configuration.
     */
    private Checksum checksum = new Checksum();

    /**
     * Checksum algorithm and field-selection configuration.
     */
    @Getter
    @Setter
    public static class Checksum {

        /**
         * Default constructor.
         */
        public Checksum() {
            // Default constructor
        }

        /**
         * JCA {@code MessageDigest} algorithm name (e.g. {@code SHA-256}, {@code SHA-512}).
         */
        private String algorithm = "SHA-256";

        /**
         * {@code RouteDefinition} field names included in the checksum.
         * Field order in this list does not affect the result — values are sorted internally.
         * An empty list applies the default field set: {@code predicates}, {@code filters}, {@code uri}.
         * A list containing only {@code "*"} includes all available fields.
         */
        private List<String> includeFields = new ArrayList<>();
    }
}
