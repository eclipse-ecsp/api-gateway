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

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response DTO for validation errors (400 Bad Request).
 * Contains a message and list of field-level validation errors.
 */
@Getter
@Setter
@Builder
public class ValidationErrorResponseDto {
    /**
     * Default constructor.
     */
    public ValidationErrorResponseDto() {
        // Default constructor
    }

    /**
     * Constructor with all fields.
     *
     * @param message error message
     * @param errors list of field-level validation errors
     */
    public ValidationErrorResponseDto(String message, List<FieldError> errors) {
        this.message = message;
        this.errors = errors;
    }

    private String message;
    private List<FieldError> errors;

    /**
     * Field-level validation error.
     */
    @Getter
    @Setter
    @Builder
    public static class FieldError {
        /**
         * Default constructor.
         */
        public FieldError() {
            // Default constructor
        }

        /**
         * Constructor with all fields.
         *
         * @param clientId client ID
         * @param field field name
         * @param error error message
         */
        public FieldError(String clientId, String field, String error) {
            this.clientId = clientId;
            this.field = field;
            this.error = error;
        }

        private String clientId;
        private String field;
        private String error;
    }
}
