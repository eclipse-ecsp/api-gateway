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

package org.eclipse.ecsp.gateway.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * PublicKeyRefreshEvent is used to signal a refresh of public keys.
 * It contains the type of refresh and the source ID that triggered the event.
 */
@Getter
@Setter
@AllArgsConstructor
public class PublicKeyRefreshEvent {
    private RefreshType refreshType;
    private String sourceId;

    /**
     * Enum representing the type of refresh operation.
     * PUBLIC_KEY indicates a refresh of a single public key,
     * ALL_KEYS indicates a refresh of all public keys.
     */
    public enum RefreshType {
        /**
         * Refresh a single public key.
         */
        PUBLIC_KEY,
        /**
         * Refresh all public keys.
         */
        ALL_KEYS,
    }

}
