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
 * CustomGatewayFilterInvalidTestController provides test endpoints with various filter annotations
 * for demonstration and testing purposes.
 *
 * <p>Each method demonstrates different usages of {@link CustomGatewayFilter}
 * and {@link CustomGatewayFilters} annotations, including single and multiple
 * filters, as well as different ways to specify filter arguments.
 *
 * @author Abhishek Kumar
 */
@RestController("/v1/test")
public class CustomGatewayFiltersTestController {

    /**
     * Returns a simple test string.
     *
     * @return the string "test"
     */
    public String test() {
        return "test";
    }

    /**
     * Returns a test string with multiple request filters applied.
     *
     * <p>Demonstrates usage of {@link CustomGatewayFilters} with multiple
     * {@link CustomGatewayFilter} annotations, each with different argument styles.
     *
     * @return the string "testWithFilters"
     */
    @CustomGatewayFilter(name = "ValidateApiKey",
                args = { "header-name=x-api-key" })
    @CustomGatewayFilter(name = "ValidateClientId",
                    args = {
                        "header-name=x-client",
                        "regex=^[a-zA-Z0-9]{1,20}$",
                    }
          )

    @GetMapping("/testWithFilters")
    public String testWithFilters() {
        return "testWithFilters";
    }


    /**
     * Returns a test string with a single filter and a single argument.
     *
     * @return the string "testWithFilterSingleArguments"
     */
    @CustomGatewayFilter(name = "TestFilter1", args = "key1=value1")
    @GetMapping("/testWithFilterSingleArguments")
    public String testWithFilterSingleArguments() {
        return "testWithFilterSingleArguments";
    }

    /**
     * Returns a test string with a single filter and multiple arguments.
     *
     * @return the string "testWithFilterMultiArguments"
     */
    @CustomGatewayFilter(name = "TestFilter2", args = {
        "key1= value1",
        "key2=value2"
    })
    @GetMapping("/testWithFilterMultiArguments")
    public String testWithFilterMultiArguments() {
        return "testWithFilterMultiArguments";
    }


    /**
     * Returns a test string with multiple filters, each using a string argument.
     *
     * @return the string "testWithMultiFilters"
     */
    @CustomGatewayFilter(name = "testFilter3", args = "key1=value1")
    @CustomGatewayFilter(name = "testFilter4", args = "key2=value2")
    @GetMapping("/testWithMultiFilters")
    public String testWithMultiFilters() {
        return "testWithMultiFilters";
    }

    @GetMapping("/testWithMultiFiltersArgument")
    public String testWithNoFilters() {
        return "testWithNoFilters";
    }

    @GetMapping("/testWithNoFiltersArgs")
    @CustomGatewayFilter(name = "testWithFiltersNoArgs", args = {})
    public String testFiltersNoArgs() {
        return "testWithFiltersNoArgs";
    }
}
