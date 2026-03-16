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

package org.eclipse.ecsp.registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Response DTO for Client Access Control configurations.
 *
 * <p>Used in:
 * - GET /v1/config/client-access-control (list all)
 * - GET /v1/config/client-access-control/{id} (get by ID)
 * - Response after create/update operations
 *
 * @see <a href="contracts.md">API Contracts Documentation</a>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientAccessControlResponseDto {
    /**
     * Default constructor.
     */
    public ClientAccessControlResponseDto() {
        // Default constructor
    }

    /**
     * Constructor with all fields.
     *
     * @param clientId client identifier
     * @param description human-readable description
     * @param tenant tenant/organization
     * @param isActive active status flag
     * @param allow access control configuration
     */
    public ClientAccessControlResponseDto(String clientId, String description, String tenant,
                                         Boolean isActive, List<String> allow) {
        this.clientId = clientId;
        this.description = description;
        this.tenant = tenant;
        this.isActive = isActive;
        this.allow = allow;
    }

    /**
     * Client identifier (primary key).
     */
    @JsonProperty("clientId")
    private String clientId;

    /**
     * Human-readable description.
     */
    @JsonProperty("description")
    private String description;

    /**
     * Tenant/organization.
     */
    @JsonProperty("tenant")
    private String tenant;

    /**
     * Active status flag.
     */
    @JsonProperty("isActive")
    private Boolean isActive;

    /**
     * Allow/deny rules array.
     */
    @JsonProperty("allow")
    private List<String> allow;
}
