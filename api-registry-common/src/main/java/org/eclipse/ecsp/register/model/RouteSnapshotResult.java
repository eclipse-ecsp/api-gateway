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

package org.eclipse.ecsp.register.model;

/**
 * Result of a {@link RouteSnapshotRequest} reconciliation.
 *
 * @param service    the reconciled service name
 * @param generation the applied deployment generation
 * @param snapshotId the applied snapshot id
 * @param created    always zero; route creation is handled by individual POST registrations
 * @param updated    always zero; route updates are handled by individual POST registrations
 * @param retired    number of previously active routes marked inactive/retired
 * @param unchanged  number of desired routes whose definitions were left unchanged
 */
public record RouteSnapshotResult(
        String service, long generation, String snapshotId,
        int created, int updated, int retired, int unchanged) {
}
