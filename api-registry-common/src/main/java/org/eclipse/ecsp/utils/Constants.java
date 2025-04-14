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

package org.eclipse.ecsp.utils;

/**
 * Constant Class to define the constants used in API registry common.
 */
public class Constants {
    /**
     * Endpoint for POST requests.
     */
    public static final String POST_ENDPOINT = "/api/v1/routes";

    /**
     * Default port number.
     */
    public static final String PORT = "7000";

    /**
     * Colon character.
     */
    public static final String COLON = ":";

    /**
     * User ID key.
     */
    public static final String USER_ID = "user-id";

    /**
     * Scope key.
     */
    public static final String SCOPE = "scope";

    /**
     * Override scope key.
     */
    public static final String OVERRIDE_SCOPE = "override-scope";

    /**
     * Correlation ID key.
     */
    public static final String CORRELATION_ID = "correlationId";

    /**
     * Account ID key.
     */
    public static final String ACCOUNT_ID = "accountId";

    /**
     * Tenant ID key.
     */
    public static final String TENANT_ID = "tenantId";

    /**
     * Prefix for generated names.
     */
    public static final String GENERATED_NAME_PREFIX = "_genkey_";

    /**
     * Method key.
     */
    public static final String METHOD = "Method";

    /**
     * Path key.
     */
    public static final String PATH = "Path";

    /**
     * Key for generated key 0.
     */
    public static final String KEY_0 = "_genkey_0";

    /**
     * Schema key.
     */
    public static final String SCHEMA = "Schema";

    /**
     * Body class key.
     */
    public static final String BODY_CLASS = "bodyClass";

    /**
     * String class name.
     */
    public static final String STRING = "java.lang.String";

    /**
     * Path delimiter.
     */
    public static final String PATH_DELIMITER = "/";

    /**
     * Health check URL.
     */
    public static final String HEALTH_URL = "/actuator/health";

    /**
     * Open API documentation URL.
     */
    public static final String OPEN_API_URL = "/v3/api-docs/";

    /**
     * Rewrite path filter key.
     */
    public static final String REWRITE_PATH_FILTER = "RewritePath";

    /**
     * Regular expression key.
     */
    public static final String REGEX = "regexp";

    /**
     * Replacement key.
     */
    public static final String REPLACEMENT = "replacement";

    /**
     * Regular expression segment.
     */
    public static final String REGEX_SEGMENT = "/(?<segment>.*)";

    /**
     * Replacement regular expression.
     */
    public static final String REPLACEMENT_REGEX = "/${segment}";
}
