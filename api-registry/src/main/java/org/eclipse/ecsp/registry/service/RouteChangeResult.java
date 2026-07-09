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

/**
 * Immutable result of a checksum-based change detection evaluation.
 *
 * <p>Carries the resolved {@link RouteChangeType}, the previous and current checksums,
 * the route identifier, and a correlation UUID that links this result to its structured
 * log entry and event payload.
 *
 * @param changeType       resolved type of change
 * @param previousChecksum checksum stored before this change; {@code null} for {@code NEW}
 * @param currentChecksum  checksum computed for the incoming route; {@code null} for {@code DELETED}
 * @param routeId          unique route identifier
 * @param correlationId    UUID correlating the result to its log entry and event payload
 */
public record RouteChangeResult(
        RouteChangeType changeType,
        String previousChecksum,
        String currentChecksum,
        String routeId,
        String correlationId) {
}
