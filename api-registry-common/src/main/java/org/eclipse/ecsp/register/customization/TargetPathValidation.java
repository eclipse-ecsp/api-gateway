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

package org.eclipse.ecsp.register.customization;

/**
 * Result of {@link PathTemplateUtils#validate(String, String)}.
 *
 * @param valid                {@code true} if the target path template is acceptable
 * @param reason               a typed rejection reason (e.g. {@code invalid_path_template},
 *                              {@code reserved_path}); {@code null} when {@link #valid} is {@code true}
 * @param normalizedTargetPath the trimmed target path; {@code null} when {@link #valid} is {@code false}
 */
public record TargetPathValidation(boolean valid, String reason, String normalizedTargetPath) {

    /**
     * Builds a successful validation result.
     *
     * @param normalizedTargetPath the trimmed, accepted target path
     * @return a valid result
     */
    public static TargetPathValidation valid(String normalizedTargetPath) {
        return new TargetPathValidation(true, null, normalizedTargetPath);
    }

    /**
     * Builds a rejected validation result.
     *
     * @param reason the typed rejection reason
     * @return an invalid result
     */
    public static TargetPathValidation invalid(String reason) {
        return new TargetPathValidation(false, reason, null);
    }
}
