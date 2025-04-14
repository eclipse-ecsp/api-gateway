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

package org.eclipse.ecsp.security;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.HashMap;
import java.util.Map;

/**
 * CustomHeaders class to hold custom headers for API requests.
 *
 * <p>This class is used to encapsulate the correlation ID, tenant ID, account ID, and any additional
 * metadata that may be required for processing API requests.
 */
@Getter
@ToString
public class CustomHeaders {
    /**
     * The correlation ID for the request.
     */
    protected final String correlationId;
    /**
     * The tenant ID for the request.
     */
    protected final String tenantId;
    /**
     * The account ID for the request.
     */
    protected final String accountId;

    /**
     * A map to hold additional metadata for the request.
     */
    @Setter
    protected Map<String, String> metadata;

    /**
     * Constructor to initialize CustomHeaders with correlation ID, tenant ID, and account ID.
     *
     * @param correlationId the correlation ID for the request
     * @param tenantId      the tenant ID for the request
     * @param accountId     the account ID for the request
     */
    public CustomHeaders(String correlationId, String tenantId, String accountId) {
        this.correlationId = correlationId;
        this.tenantId = tenantId;
        this.accountId = accountId;
        this.metadata = new HashMap<>();
    }
}
