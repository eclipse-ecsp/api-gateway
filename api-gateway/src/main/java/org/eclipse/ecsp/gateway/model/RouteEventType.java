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

package org.eclipse.ecsp.gateway.model;

/**
 * Enum representing the type of route change event that covers all route lifecycle operations.
 */
public enum RouteEventType {
    /**
     * Indicates a route change occurred (create, update, or delete).
     */
    ROUTE_CHANGE,
    
    /**
     * Indicates a rate limit configuration changed for specific routes.
     */
    RATE_LIMIT_CONFIG_CHANGE,
    
    /**
     * Indicates service health status changed (up/down).
     */
    SERVICE_HEALTH_CHANGE,

    /**
     * Indicates client access control configuration changed.
     */
    CLIENT_ACCESS_CONTROL_UPDATED,
}
