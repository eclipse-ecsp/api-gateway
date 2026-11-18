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

/**
 * Configuration properties for the detection of conflicting route registrations.
 *
 * <p>Bound from prefix {@code api.registry.conflict-detection}. Two routes conflict when they have the same HTTP
 * method and the same path, whatever the names of the path variables are, and they are not the same API registered
 * again by a replica of its own service.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api.registry.conflict-detection")
public class RouteConflictProperties {

    /**
     * Default constructor.
     */
    public RouteConflictProperties() {
        // Default constructor
    }

    /**
     * What happens to a registration that conflicts with a registered route.
     */
    private Mode mode = Mode.REJECT;

    /**
     * Reaction to a conflicting registration.
     */
    public enum Mode {
        /**
         * The registration is rejected with {@code 409 Conflict} and nothing is stored.
         */
        REJECT,
        /**
         * The registration is accepted as before, and the conflict is logged and counted.
         */
        WARN,
        /**
         * Conflicts are not looked for at all.
         */
        OFF
    }
}
