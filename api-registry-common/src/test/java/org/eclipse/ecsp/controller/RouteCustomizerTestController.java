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

package org.eclipse.ecsp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.eclipse.ecsp.security.Security;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fixture controller for the route-customizer integration test.
 */
@RestController
@RequestMapping("/route-customizer")
public class RouteCustomizerTestController {

    /**
     * Returns the requested vehicle id.
     *
     * @param vehicleId the vehicle id
     * @return the vehicle id
     */
    @Operation(summary = "GET /vehicles/{vehicleId}", description = "Find vehicle by id")
    @SecurityRequirement(name = Security.JWT_AUTH_VALIDATOR, scopes = {"SelfManage"})
    @GetMapping("/vehicles/{vehicleId}")
    public String findVehicle(@PathVariable("vehicleId") String vehicleId) {
        return vehicleId;
    }
}
