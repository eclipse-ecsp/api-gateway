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

package org.eclipse.ecsp.gateway.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;

/**
 * GatewayConstants.
 *
 * <p>Constants used in the API Gateway.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GatewayConstants {

    /**
     * The key used to store the request path in the request attributes.
     */
    public static final String PATH = "Path";
    /**
     * The key used to store the request path in the request attributes.
     */
    public static final String METHOD = "Method";
    /**
     * The key used to store the request method in the request attributes.
     */
    public static final String KEY_0 = "_genkey_0";
    /**
     * constant value is sub.
     */
    public static final String SUB = "sub";

    /**
     * constant value is required.
     */
    public static final String REQUIRED = "required";
    /**
     * constant value is regex.
     */
    public static final String REGEX = "regex";
    /**
     * constant value is Authorization.
     */
    public static final String AUTHORIZATION = "Authorization";
    /**
     * constant value is Bearer.
     */
    public static final String BEARER = "Bearer ";
    /**
     * constant value is user-id.
     */
    public static final String USER_ID = "user-id";
    /**
     * constant value is userId.
     */
    public static final String USERID = "userId";
    /**
     * constant value is scope.
     */
    public static final String SCOPE = "scope";
    /**
     * constant value is override-scope.
     */
    public static final String OVERRIDE_SCOPE = "override-scope";
    /**
     * constant value is correlationId.
     */
    public static final String CORRELATION_ID = "correlationId";
    /**
     * constant value is accountId.
     */
    public static final String ACCOUNT_ID = "accountId";
    /**
     * constant value is tenantId.
     */
    public static final String TENANT_ID = "tenantId";
    /**
     * constant value is "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".
     */
    public static final String UUID_REGEX =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    /**
     * constant value is "^[a-zA-Z0-9]+$".
     */
    public static final String ALPHANUMERIC_REGEX = "^[a-zA-Z0-9]+$";
    /**
     * constant value is Schema.
     */
    public static final String SCHEMA = "Schema";
    /**
     * constant value is SchemaValidator.
     */
    public static final String SCHEMA_VALIDATOR = "SchemaValidator";
    /**
     * constant value is -----BEGIN CERTIFICATE-----.
     */
    public static final String CERTIFICATE_HEADER_PREFIX = "-----BEGIN CERTIFICATE-----";
    /**
     * constant value is -----BEGIN PUBLIC KEY-----.
     */
    public static final String PUBLICKEY_HEADER_PREFIX = "-----BEGIN PUBLIC KEY-----";
    /**
     * constant value is /v3/api-docs/.
     */
    public static final String API_DOCS_URL = "/v3/api-docs/";
    /**
     * constant value is "/".
     */
    public static final String PATH_DELIMITER = "/";
    /**
     * constant value is GET.
     */
    public static final String GET = "GET";
    /**
     * constant value is POST.
     */
    public static final String POST = "POST";
    /**
     * constant value is DELETE.
     */
    public static final String DELETE = "DELETE";
    /**
     * constant value is PUT.
     */
    public static final String PUT = "PUT";
    /**
     * constant value is UTF-8.
     */
    public static final String ENCODING_UTF = "UTF-8";
    /**
     * constant value is service-name.
     */
    public static final String SERVICE_NAME = "service-name";
    /**
     * constant value is UNKNOWN.
     */
    public static final String UNKNOWN = "UNKNOWN";
    /**
     * constant value is service.
     */
    public static final String SERVICE = "service";

    // Filter order
    /**
     * constant value is -1000.
     */
    public static final int ACCESS_LOG_FILTER_ORDER = -1000;
    /**
     * constant value is 0.
     */
    public static final int JWT_AUTH_FILTER_ORDER = NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 2;
    /**
     * constant value is 1.
     */
    public static final int CACHE_FILTER_ORDER = NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
    /**
     * constant value is 20.
     */
    public static final int REQUEST_HEADER_FILTER_ORDER = 20;
    /**
     * constant value is 30.
     */
    public static final int REQUEST_BODY_VALIDATOR_FILTER_ORDER = 30;

    // monitoring
    /**
     * constant value is health.
     */
    public static final String HEALTH = "health";
    /**
     * constant value is prometheus.
     */
    public static final String PROMETHEUS = "prometheus";

    /**
     * Key for message field in responses or error objects.
     */
    public static final String MESSAGE = "message";

    /**
     * Key for code field in responses or error objects.
     */
    public static final String CODE = "code";

    /**
     * Key for API Gateway error attribute.
     */
    public static final String API_GATEWAY_ERROR = "api.gateway.error";

    /**
     * Message for request not found errors.
     */
    public static final String REQUEST_NOT_FOUND = "Request not found";

    /**
     * Message for internal server errors.
     */
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    /**
     * Gateway metrics filter order.
     */
    public static final int GATEWAY_METRICS_FILTER_ORDER = -100;

    // Metric type constants - Used for categorizing and labeling metrics in monitoring systems
    /**
     * Metric label key for identifying the component being monitored.
     */
    public static final String COMPONENT = "component";

    /**
     * Metric label value for public key cache component identification.
     */
    public static final String PUBLIC_KEY_CACHE = "public-key-cache";

    /**
     * Metric label key for specifying the type of metric being recorded.
     */
    public static final String TYPE = "type";

    /**
     * Metric label key for tracking the number of key sources configured.
     */
    public static final String KEY_SOURCES = "key-sources";

    /**
     *  Metric label key for tracking cache size information.
     */
    public static final String CACHE_SIZE = "cache-size";

    /**
     * Metric label key for tracking the time taken for refresh operations.
     */
    public static final String REFRESH_TIME = "last-refresh-time";

    /**
     * Metric label key for tracking the number of public key refresh operations.
     */
    public static final String REFRESH_COUNT = "refresh-count";

    /**
     * Metric label key for tracking the count of source refresh operations.
     */
    public static final String REFRESH_SOURCE_COUNT = "refresh-source-count";

    /**
     * Metric label key for tracking the timestamp for source refresh operations.
     */
    public static final String REFRESH_SOURCE_TIME = "refresh-source-time";

    // Default metric names - Standard metric identifiers used in monitoring dashboards
    /**
     * Default metric name for public key cache size.
     */
    public static final String DEFAULT_CACHE_SIZE_METRIC = "public_key_cache_size";
    /**
     * Default metric name for public key sources count.
     */
    public static final String DEFAULT_KEY_SOURCES_METRIC = "public_key_sources_count";
    /**
     * Default metric name for public key refresh count.
     */
    public static final String DEFAULT_REFRESH_COUNT_METRIC = "public_key_refresh_count";
    /**
     * Default metric name for last public key refresh time.
     */
    public static final String DEFAULT_LAST_REFRESH_TIME_METRIC = "public_key_refresh_time";
    /**
     * Default metric name for public key source refresh count.
     */
    public static final String DEFAULT_SOURCE_REFRESH_COUNT_METRIC = "public_key_source_refresh_count";
    /**
     * Default metric name for public key source refresh time.
     */
    public static final String DEFAULT_SOURCE_REFRESH_TIME_METRIC = "public_key_source_refresh_time";
}