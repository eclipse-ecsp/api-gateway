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
import lombok.Getter;
import lombok.Setter;
import java.util.List;


/**
 * Response DTO for bulk create operations.
 * Contains a message and list of created client IDs.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkCreateResponseDto {
    /**
     * Default constructor.
     */
    public BulkCreateResponseDto() {
        // Default constructor
    }

    /**
     * Constructor with all fields.
     *
     * @param message the response message
     * @param created the list of created client IDs
     */
    public BulkCreateResponseDto(String message, List<String> created) {
        this.message = message;
        this.created = created;
    }

    private String message;
    private List<String> created;
}
