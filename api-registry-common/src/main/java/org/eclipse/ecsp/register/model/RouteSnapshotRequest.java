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

package org.eclipse.ecsp.register.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * Authoritative desired-route-set contract for
 * {@code PUT /api/v1/routes/services/{service}/snapshot}.
 *
 * <p>Unlike {@code POST /api/v1/routes}, absence from this list is meaningful: any route
 * previously registered for the service but absent here is retired (see
 * {@code ApiRouteService#reconcile}).
 *
 * @param generation monotonically increasing per-service deployment generation
 * @param snapshotId SHA-256 digest of canonical {@code (generation, routes)} content
 *                    ({@link org.eclipse.ecsp.register.SnapshotDigestUtil}), recomputed and
 *                    verified server-side; distinguishes an idempotent retry from conflicting
 *                    content submitted at the same generation
 * @param routes     the complete desired route set for the service, including static,
 *                    OpenAPI-derived, alias, and API-doc routes
 */
public record RouteSnapshotRequest(
        @Positive long generation,
        @NotBlank String snapshotId,
        @NotEmpty @Valid List<RouteDefinition> routes) {
}
