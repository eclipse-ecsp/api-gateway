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

package org.eclipse.ecsp.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing a single access rule (allow or deny).
 *
 * <p>Rules follow the format: [!]service:route
 * - ! prefix indicates a deny rule
 * - * wildcard supported for service and/or route
 *
 * <p>Examples:
 * - Allow all routes in user-service: service="user-service", route="*", deny=false
 * - Deny specific route: service="user-service", route="ban-user", deny=true
 * - Allow all services and routes: service="*", route="*", deny=false
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessRule {

    /**
     * Service name or wildcard (*).
     * Examples: "user-service", "payment-service", "*"
     */
    private String service;

    /**
     * Route path or wildcard (*).
     * Examples: "get-user-profile", "get-*", "*"
     */
    private String route;

    /**
     * Deny flag.
     * true = deny rule (negative rule, prefixed with !)
     * false = allow rule (positive rule, no prefix)
     */
    private boolean deny;

    /**
     * Original rule string for logging/debugging.
     * Examples: "user-service:*", "!payment-service:refund"
     */
    private String originalRule;
}
