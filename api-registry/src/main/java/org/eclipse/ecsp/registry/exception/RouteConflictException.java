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

package org.eclipse.ecsp.registry.exception;

import lombok.Getter;
import java.util.List;

/**
 * Exception thrown when a route is registered under the method and path of a route that is already registered.
 * Results in HTTP 409 Conflict response.
 */
@Getter
public class RouteConflictException extends RuntimeException {
    /**
     * Ids of the registered routes the registration conflicts with.
     */
    private final List<String> conflictingRouteIds;

    /**
     * Constructor with the conflicting route ids.
     *
     * @param message             error message
     * @param conflictingRouteIds ids of the registered routes that already own the method and path
     */
    public RouteConflictException(String message, List<String> conflictingRouteIds) {
        super(message);
        this.conflictingRouteIds = conflictingRouteIds;
    }
}
