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

package org.eclipse.ecsp.registry.exception;

import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when duplicate client IDs are detected.
 * Results in HTTP 409 Conflict response.
 */
@Getter
public class DuplicateClientException extends RuntimeException {
    private final List<String> duplicateClientIds;

    /**
     * Constructor with duplicate client IDs.
     *
     * @param message Error message
     * @param duplicateClientIds List of duplicate client IDs
     */
    public DuplicateClientException(String message, List<String> duplicateClientIds) {
        super(message);
        this.duplicateClientIds = duplicateClientIds;
    }
}
