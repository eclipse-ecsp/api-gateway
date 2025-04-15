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

import lombok.experimental.FieldNameConstants;

/**
 * enum to define Scopes.
 */
@FieldNameConstants(onlyExplicitlyIncluded = true)
public enum Scopes {
    /**
     * openid scope.
     */
    @FieldNameConstants.Include OPEN_ID,
    /**
     * OPENID scope.
     */
    @FieldNameConstants.Include OPENID,
    /**
     * SYSTEM_MANAGE scope.
     */
    @FieldNameConstants.Include SYSTEM_MANAGE,
    /**
     * SYSTEM_READ scope.
     */
    @FieldNameConstants.Include SYSTEM_READ,
    /**
     * DOMAIN_MANAGE scope.
     */
    @FieldNameConstants.Include DOMAIN_MANAGE,
    /**
     * ACCOUNT_MANAGE scope.
     */
    @FieldNameConstants.Include ACCOUNT_MANAGE,
    /**
     * VEHICLE_MANAGE scope.
     */
    @FieldNameConstants.Include VEHICLE_MANAGE,
    /**
     * USER_MANAGE scope.
     */
    @FieldNameConstants.Include USER_MANAGE,
    /**
     * SelfManage scope.
     */
    @FieldNameConstants.Include SELF_MANAGE
}
