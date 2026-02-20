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