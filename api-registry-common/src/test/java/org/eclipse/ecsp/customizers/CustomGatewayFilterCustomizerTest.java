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

package org.eclipse.ecsp.customizers;

import io.swagger.v3.oas.models.Operation;
import org.eclipse.ecsp.controller.CustomGatewayFiltersTestController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.method.HandlerMethod;
import java.lang.reflect.Method;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class CustomGatewayFilterCustomizerTest {

    @Test
    void testWithFiltersCustomization() throws NoSuchMethodException {
        Method method = CustomGatewayFiltersTestController.class.getMethod("testWithFilters");
        HandlerMethod handlerMethod = new HandlerMethod(new CustomGatewayFiltersTestController(), method);

        CustomGatewayFilterCustomizer customizer = new CustomGatewayFilterCustomizer();
        Operation operation = new Operation();

        operation = customizer.customize(operation, handlerMethod);

        @SuppressWarnings("unchecked")
        Map<String, Object> filter1Args = (Map<String, Object>) operation.getExtensions()
                .get(CustomGatewayFilterCustomizer.FILTERS_EXTENSION + "-ValidateApiKey");
        Assertions.assertEquals("x-api-key", filter1Args.get("header-name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> filter2Args = (Map<String, Object>) operation.getExtensions()
                .get(CustomGatewayFilterCustomizer.FILTERS_EXTENSION + "-ValidateClientId");
        Assertions.assertEquals("x-client", filter2Args.get("header-name"));
        Assertions.assertEquals("^[a-zA-Z0-9]{1,20}$", filter2Args.get("regex"));
    }

    @Test
    void testWithFilterSingleArgumentsCustomization() throws NoSuchMethodException {
        Method method = CustomGatewayFiltersTestController.class.getMethod("testWithFilterSingleArguments");
        HandlerMethod handlerMethod = new HandlerMethod(new CustomGatewayFiltersTestController(), method);

        CustomGatewayFilterCustomizer customizer = new CustomGatewayFilterCustomizer();
        Operation operation = new Operation();

        operation = customizer.customize(operation, handlerMethod);
        Assertions.assertNotNull(operation.getExtensions());

        @SuppressWarnings("unchecked")
        Map<String, Object> filter1Args = (Map<String, Object>) operation.getExtensions()
                .get(CustomGatewayFilterCustomizer.FILTERS_EXTENSION + "-TestFilter1");
        Assertions.assertEquals("value1", filter1Args.get("key1"));
    }

    @Test
    void testWithFilterMultiArgumentsCustomization() throws NoSuchMethodException {
        Method method = CustomGatewayFiltersTestController.class.getMethod("testWithFilterMultiArguments");
        HandlerMethod handlerMethod = new HandlerMethod(new CustomGatewayFiltersTestController(), method);

        CustomGatewayFilterCustomizer customizer = new CustomGatewayFilterCustomizer();
        Operation operation = new Operation();

        operation = customizer.customize(operation, handlerMethod);
        Assertions.assertNotNull(operation.getExtensions());

        @SuppressWarnings("unchecked")
        Map<String, Object> filter1Args = (Map<String, Object>) operation.getExtensions()
                .get(CustomGatewayFilterCustomizer.FILTERS_EXTENSION + "-TestFilter2");
        Assertions.assertEquals("value1", filter1Args.get("key1"));
        Assertions.assertEquals("value2", filter1Args.get("key2"));
    }

    @Test
    void testWithMultiFiltersCustomization() throws NoSuchMethodException {
        Method method = CustomGatewayFiltersTestController.class.getMethod("testWithMultiFilters");
        HandlerMethod handlerMethod = new HandlerMethod(new CustomGatewayFiltersTestController(), method);

        CustomGatewayFilterCustomizer customizer = new CustomGatewayFilterCustomizer();
        Operation operation = new Operation();

        operation = customizer.customize(operation, handlerMethod);
        Assertions.assertNotNull(operation.getExtensions());

        @SuppressWarnings("unchecked")
        Map<String, Object> filter1Args = (Map<String, Object>) operation.getExtensions()
                .get(CustomGatewayFilterCustomizer.FILTERS_EXTENSION + "-testFilter3");
        Assertions.assertEquals("value1", filter1Args.get("key1"));

        @SuppressWarnings("unchecked")
        Map<String, Object> filter2Args = (Map<String, Object>) operation.getExtensions()
                .get(CustomGatewayFilterCustomizer.FILTERS_EXTENSION + "-testFilter4");
        Assertions.assertEquals("value2", filter2Args.get("key2"));
    }

    @Test
    void  testWithNoFiltersCustomization() throws NoSuchMethodException {
        Method method = CustomGatewayFiltersTestController.class.getMethod("testWithNoFilters");
        HandlerMethod handlerMethod = new HandlerMethod(new CustomGatewayFiltersTestController(), method);

        CustomGatewayFilterCustomizer customizer = new CustomGatewayFilterCustomizer();
        Operation operation = new Operation();

        operation = customizer.customize(operation, handlerMethod);
        Assertions.assertNull(operation.getExtensions());
    }

    @Test
    void  testFiltersNoArgsCustomization() throws NoSuchMethodException {
        Method method = CustomGatewayFiltersTestController.class.getMethod("testFiltersNoArgs");
        HandlerMethod handlerMethod = new HandlerMethod(new CustomGatewayFiltersTestController(), method);

        CustomGatewayFilterCustomizer customizer = new CustomGatewayFilterCustomizer();
        Operation operation = new Operation();

        operation = customizer.customize(operation, handlerMethod);
        Assertions.assertNotNull(operation.getExtensions());

        @SuppressWarnings("unchecked")
        Map<String, Object> filter2Args = (Map<String, Object>) operation.getExtensions()
                .get(CustomGatewayFilterCustomizer.FILTERS_EXTENSION + "-testWithFiltersNoArgs");
        Assertions.assertEquals(0, filter2Args.size(), "Expected no arguments for testWithFiltersNoArgs filter");
    }
}