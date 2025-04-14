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

package org.eclipse.ecsp.security.validator;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.eclipse.ecsp.security.Scopes;
import org.springframework.stereotype.Component;

/**
 * TestValidator.
 */
@Component
public class TestValidator {

    @SecurityRequirement(name = "ValidScope", scopes = {Scopes.Fields.SelfManage})
    public void validScope() {

    }

    @SecurityRequirement(name = "EmptyScope")
    public void emptyScope() {

    }

    @SecurityRequirement(name = "InvalidScope", scopes = {"Invalid-Scope"})
    public void invalidScope() {
    }

    public void emptyAnnotation() {
    }
}
