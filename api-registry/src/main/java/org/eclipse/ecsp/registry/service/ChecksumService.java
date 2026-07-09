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

package org.eclipse.ecsp.registry.service;

import org.eclipse.ecsp.register.model.RouteDefinition;
import java.util.Optional;

/**
 * Computes a deterministic, configurable checksum over selected
 * attributes of a {@link RouteDefinition}.
 *
 * <p>Implementations must be stateless and must produce identical output for identical inputs
 * regardless of field insertion order (NFR-01).
 */
public interface ChecksumService {

    /**
     * Computes the checksum for the given route definition.
     *
     * <p>Returns {@code Optional.empty()} on any failure; callers must treat
     * absence as {@link RouteChangeType#UPDATED} to ensure route registration proceeds (FR-07).
     *
     * @param route the route definition to hash; must not be {@code null}
     * @return {@code Optional} containing the hex-encoded checksum, or empty on failure
     */
    Optional<String> compute(RouteDefinition route);
}
