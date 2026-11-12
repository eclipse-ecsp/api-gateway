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

package org.eclipse.ecsp.register.customization;

import io.swagger.v3.oas.models.Operation;
import java.util.Objects;

/**
 * Single, shared implementation of the route-id formula ({@code tag + "-" + operationId})
 * used by {@code ApiRoutesLoader.createRouteDefinition}, the route customizer matching
 * algorithm, and {@link RouteMappingMatcher}.
 *
 * <p>Kept as one utility (rather than two independently-maintained formulas) so the loader's
 * persisted route id and the customizer's early, pre-registration match/alias-of extension can
 * never drift apart.
 */
public final class RouteIdUtils {

    private RouteIdUtils() {
    }

    /**
     * Computes the route id for an operation, or {@code null} when the operation lacks an
     * {@code operationId} or a tag. Safe to call while scanning unrelated, possibly-incomplete
     * operations &mdash; never throws.
     *
     * @param operation the OpenAPI operation
     * @return the computed route id, or {@code null} if it cannot be computed
     */
    public static String computeRouteId(Operation operation) {
        if (operation == null || operation.getOperationId() == null
                || operation.getTags() == null || operation.getTags().isEmpty()) {
            return null;
        }
        String normalizedOperationId = operation.getOperationId().replace("_", "-");
        return operation.getTags().get(0) + "-" + normalizedOperationId;
    }

    /**
     * Computes the route id for an operation, failing fast when it cannot be computed. Used by
     * {@code ApiRoutesLoader.createRouteDefinition}, which must preserve its existing
     * per-operation fail-fast behaviour (an untagged/id-less operation is skipped with an
     * {@code ERROR} log by the caller's try/catch, exactly as before this utility existed).
     *
     * @param operation the OpenAPI operation
     * @return the computed route id, never {@code null}
     * @throws NullPointerException if the operation lacks an operationId or a tag
     */
    public static String requireRouteId(Operation operation) {
        return Objects.requireNonNull(computeRouteId(operation),
                "Operation must define an operationId and at least one tag");
    }
}
