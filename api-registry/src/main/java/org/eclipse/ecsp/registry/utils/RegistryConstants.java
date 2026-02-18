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

package org.eclipse.ecsp.registry.utils;

import lombok.NoArgsConstructor;

/**
 * RegistryConstants is a constant class to define constants used in Registry application.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class RegistryConstants {

    /**
     * REGISTRY.
     */
    public static final String REGISTRY = "REGISTRY";
    /**
     * "/".
     */
    public static final String PATH_DELIMITER = "/";
    /**
     * api_registry_database_type .
     */
    public static final String DATABASE_TYPE = "api-registry.database.type";
    /**
     * sql.
     */
    public static final String SQL = "sql";
    /**
     * nosql.
     */
    public static final String NOSQL = "nosql";
    /**
     * UNKNOWN.
     */
    public static final String UNKNOWN = "UNKNOWN";

    /**
     * REGISTRY_EVENT_PREFIX.
     */
    public static final String REGISTRY_EVENT_PREFIX = "api-registry.events";

    /**
     * REGISTRY_EVENT_ENABLED.
     */
    public static final String REGISTRY_EVENT_ENABLED = REGISTRY_EVENT_PREFIX + ".enabled";
}
