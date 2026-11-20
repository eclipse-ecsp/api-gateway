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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link RouteCustomizerProperties}: constraints, route selection and property binding.
 */
class RouteCustomizerPropertiesTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableBinding.class, RouteCustomizerProperties.class);

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        factory.close();
    }

    private static RouteCustomizerProperties.FilterOverride filter(String name) {
        RouteCustomizerProperties.FilterOverride filter = new RouteCustomizerProperties.FilterOverride();
        filter.setName(name);
        return filter;
    }

    private static RouteCustomizerProperties.RouteMapping routeIdMapping() {
        RouteCustomizerProperties.RouteMapping mapping = new RouteCustomizerProperties.RouteMapping();
        mapping.setRouteId("Demo-doThis");
        mapping.setFilters(List.of(filter("VehicleIdValidation")));
        return mapping;
    }

    private static RouteCustomizerProperties.RouteMapping pathMapping(String path, String method) {
        RouteCustomizerProperties.RouteMapping mapping = new RouteCustomizerProperties.RouteMapping();
        mapping.setPath(path);
        mapping.setMethod(method);
        mapping.setFilters(List.of(filter("VehicleIdValidation")));
        return mapping;
    }

    private static Set<String> violationMessages(RouteCustomizerProperties.RouteMapping mapping) {
        return validator.validate(mapping).stream().map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void routeIdAndPathMappingsAreValid() {
        assertTrue(validator.validate(routeIdMapping()).isEmpty());
        assertTrue(validator.validate(pathMapping("/v1/api/dothis", "GET")).isEmpty());
        assertTrue(validator.validate(pathMapping("/v1/api/dothis", null)).isEmpty());
    }

    @Test
    void rejectsNeitherRouteIdNorPath() {
        RouteCustomizerProperties.RouteMapping mapping = routeIdMapping();
        mapping.setRouteId(null);
        assertThat(violationMessages(mapping)).containsExactly("exactly one of route-id or path must be set");
    }

    @Test
    void rejectsBothRouteIdAndPath() {
        RouteCustomizerProperties.RouteMapping mapping = routeIdMapping();
        mapping.setPath("/v1/api/dothis");
        assertThat(violationMessages(mapping)).containsExactly("exactly one of route-id or path must be set");
    }

    @Test
    void rejectsMethodWithoutPath() {
        RouteCustomizerProperties.RouteMapping mapping = routeIdMapping();
        mapping.setMethod("GET");
        assertThat(violationMessages(mapping)).containsExactly("method is only meaningful together with path");
    }

    @Test
    void rejectsUnsupportedMethod() {
        assertThat(violationMessages(pathMapping("/v1/api/dothis", "FETCH")))
                .containsExactly("must be a supported HTTP method");
    }

    @Test
    void rejectsMappingWithoutFilters() {
        RouteCustomizerProperties.RouteMapping mapping = routeIdMapping();
        mapping.setFilters(List.of());
        assertThat(violationMessages(mapping)).containsExactly("mapping must configure at least one filter");
    }

    @Test
    void rejectsBlankFilterName() {
        RouteCustomizerProperties.RouteMapping mapping = routeIdMapping();
        mapping.setFilters(List.of(filter(" ")));
        assertFalse(validator.validate(mapping).isEmpty());
    }

    @Test
    void routeIdMappingSelectsOnlyThatRouteId() {
        RouteCustomizerProperties.RouteMapping mapping = routeIdMapping();
        assertTrue(mapping.matches("Demo-doThis", "/anything", "GET"));
        assertFalse(mapping.matches("Demo-doThat", "/anything", "GET"));
        assertFalse(mapping.matches(null, "/anything", "GET"));
    }

    @Test
    void pathMappingWithoutMethodSelectsEveryMethodOfThePath() {
        RouteCustomizerProperties.RouteMapping mapping = pathMapping("/v1/api/dothis", null);
        assertTrue(mapping.matches("any-id", "/v1/api/dothis", "GET"));
        assertTrue(mapping.matches("any-id", "/v1/api/dothis", "POST"));
        assertFalse(mapping.matches("any-id", "/v1/api/other", "GET"));
    }

    @Test
    void pathMappingWithMethodSelectsThatMethodIgnoringCase() {
        RouteCustomizerProperties.RouteMapping mapping = pathMapping("/v1/api/dothis", "get");
        assertTrue(mapping.matches("any-id", "/v1/api/dothis", "GET"));
        assertFalse(mapping.matches("any-id", "/v1/api/dothis", "POST"));
    }

    @Test
    void mappingWithoutSelectorSelectsNothing() {
        assertFalse(new RouteCustomizerProperties.RouteMapping().matches("any-id", "/v1/api/dothis", "GET"));
    }

    @Test
    void bindsTheListFromBothSpellingsOfTheRouteIdKey() {
        contextRunner.withPropertyValues(
                "api.registry.route-customizer.enabled=true",
                "api.registry.route-customizer.mappings[0].route-id=Demo-doThis",
                "api.registry.route-customizer.mappings[0].filters[0].name=VehicleIdValidation",
                "api.registry.route-customizer.mappings[0].filters[0].args.source=header",
                "api.registry.route-customizer.mappings[0].filters[0].args.name=vehicle-id",
                "api.registry.route-customizer.mappings[1].routeId=Demo-doThat",
                "api.registry.route-customizer.mappings[1].filters[0].name=AuditFilter",
                "api.registry.route-customizer.mappings[2].path=/v1/api/dothis",
                "api.registry.route-customizer.mappings[2].method=POST",
                "api.registry.route-customizer.mappings[2].filters[0].name=AuditFilter").run(context -> {
                    RouteCustomizerProperties props = context.getBean(RouteCustomizerProperties.class);
                    assertTrue(props.isEnabled());
                    List<RouteCustomizerProperties.RouteMapping> mappings = props.getMappings();
                    assertThat(mappings).extracting(RouteCustomizerProperties.RouteMapping::getRouteId)
                            .containsExactly("Demo-doThis", "Demo-doThat", null);
                    assertThat(mappings.get(0).getFilters().get(0).getArgs())
                            .isEqualTo(Map.of("source", "header", "name", "vehicle-id"));
                    assertThat(mappings.get(2).getPath()).isEqualTo("/v1/api/dothis");
                    assertThat(mappings.get(2).getMethod()).isEqualTo("POST");
                });
    }

    @Test
    void emptyMappingsListWhenEnabledWithoutMappings() {
        contextRunner.withPropertyValues("api.registry.route-customizer.enabled=true").run(context -> {
            RouteCustomizerProperties props = context.getBean(RouteCustomizerProperties.class);
            assertTrue(props.isEnabled());
            assertThat(props.getMappings()).isEmpty();
        });
    }

    @Test
    void failsStartupWhenEnabledAndMappingIsInvalid() {
        contextRunner.withPropertyValues(
                "api.registry.route-customizer.enabled=true",
                "api.registry.route-customizer.mappings[0].route-id=Demo-doThis").run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause().hasMessageContaining("mapping must configure at least one filter");
                });
    }

    @Test
    void isAbsentByDefaultAndWhenExplicitlyDisabledEvenWithInvalidMappings() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(RouteCustomizerProperties.class));
        contextRunner.withPropertyValues(
                "api.registry.route-customizer.enabled=false",
                "api.registry.route-customizer.mappings[0].route-id=Demo-doThis")
                .run(context -> assertThat(context).doesNotHaveBean(RouteCustomizerProperties.class));
    }

    /**
     * Enables {@code @ConfigurationProperties} binding for the context under test.
     */
    @Configuration
    @EnableConfigurationProperties
    static class EnableBinding {
    }
}
