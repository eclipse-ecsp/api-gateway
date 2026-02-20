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

package org.eclipse.ecsp.registry.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for creating/updating Client Access Control configurations.
 *
 * <p>Used in:
 * - POST /v1/config/client-access-control (bulk creation)
 * - PUT /v1/config/client-access-control/{id} (update)
 *
 * @see <a href="contracts.md">API Contracts Documentation</a>
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientAccessControlRequestDto {
    /**
     * Default constructor.
     */
    public ClientAccessControlRequestDto() {
        // Default constructor
    }

    /**
     * Constructor with all fields.
     *
     * @param clientId client identifier
     * @param description human-readable description
     * @param tenant tenant/organization name
     * @param isActive active status flag
     * @param allow access control configuration
     */
    public ClientAccessControlRequestDto(String clientId, String description, String tenant,
                                        Boolean isActive, List<String> allow) {
        this.clientId = clientId;
        this.description = description;
        this.tenant = tenant;
        this.isActive = isActive;
        this.allow = allow;
    }

    /**
     * Client identifier extracted from JWT claims.
     * Required field.
     * Length: 3-128 characters.
     * Pattern: Alphanumeric with ._- allowed.
     */
    @JsonProperty("clientId")
    @NotBlank(message = "clientId is required", groups = {CreateClientAccessControlGroup.class})
    private String clientId;

    /**
     * Human-readable description of the client.
     * Required field.
     * Max length: 512 characters.
     */
    @JsonProperty("description")
    @NotBlank(message = "description is required", groups = {CreateClientAccessControlGroup.class})
    private String description;

    /**
     * Tenant/organization name.
     * Required field.
     * Max length: 256 characters.
     */
    @JsonProperty("tenant")
    @NotBlank(message = "tenant is required", groups = {CreateClientAccessControlGroup.class})
    private String tenant;

    /**
     * Active status flag.
     * Optional field.
     * Default: true if omitted.
     */
    @JsonProperty("isActive")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Array of allow/deny rules in service:route format.
     * Optional field.
     *
     * <p>Format examples:
     * - Allow rules: ["user-service:*", "payment-service:process"]
     * - Deny rules: ["!user-service:ban-user"]
     * - Wildcards: ["*:*", "user-service:get-*"]
     *
     * <p>Empty/null treated as deny-by-default.
     */
    @JsonProperty("allow")
    @NotEmpty(message = "At least one allow rule is required", groups = {CreateClientAccessControlGroup.class})
    private List<String> allow;
}
