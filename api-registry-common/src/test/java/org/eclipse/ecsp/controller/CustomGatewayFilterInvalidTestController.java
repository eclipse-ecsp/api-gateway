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


import org.eclipse.ecsp.annotations.CustomGatewayFilter;
import org.eclipse.ecsp.annotations.CustomGatewayFilters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CustomGatewayFilterInvalidTestController provides test endpoints with various filter annotations.
 * for demonstration and testing purposes.
 *
 * <p>Each method demonstrates different usages of {@link CustomGatewayFilter}
 * and {@link CustomGatewayFilters} annotations, including single and multiple
 * filters, as well as different ways to specify filter arguments.
 *
 * @author Abhishek Kumar
 */
@RestController("/v2/test")
public class CustomGatewayFilterInvalidTestController {

    @GetMapping("/testFiltersWithInvalidArgs")
    @CustomGatewayFilter(name = "testFiltersWithInvalidArgs", args = "invalidArg")
    public String testFiltersWithInvalidArgs() {
        return "testFiltersWithInvalidArgs";
    }

    @GetMapping("/testFiltersWithInvalidArgs")
    @CustomGatewayFilter(args = "invalidArg")
    public String testFiltersWithNoName() {
        return "testFiltersWithInvalidArgs";
    }

    @GetMapping("/testFilters")
    @CustomGatewayFilters(value = {}, filters = {})
    public String testFiltersWithNoArgs() {
        return "testFiltersWithNoArgs";
    }
}
