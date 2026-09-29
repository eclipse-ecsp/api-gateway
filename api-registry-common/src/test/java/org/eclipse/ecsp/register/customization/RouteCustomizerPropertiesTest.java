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

package org.eclipse.ecsp.register.customization;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteCustomizerPropertiesTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        factory.close();
    }

    private RouteCustomizerProperties.RouteMapping validMapping() {
        RouteCustomizerProperties.RouteMapping mapping = new RouteCustomizerProperties.RouteMapping();
        mapping.setRouteId("Demo-doThis");
        mapping.setExposeAs(List.of("/api/dothis"));
        return mapping;
    }

    @Test
    void validMappingHasNoViolations() {
        assertTrue(validator.validate(validMapping()).isEmpty());
    }

    @Test
    void rejectsNeitherRouteIdNorPath() {
        RouteCustomizerProperties.RouteMapping mapping = validMapping();
        mapping.setRouteId(null);
        assertFalse(validator.validate(mapping).isEmpty());
    }

    @Test
    void rejectsBothRouteIdAndPath() {
        RouteCustomizerProperties.RouteMapping mapping = validMapping();
        mapping.setPath("/v1/api/dothis");
        assertFalse(validator.validate(mapping).isEmpty());
    }

    @Test
    void rejectsMethodWithoutPath() {
        RouteCustomizerProperties.RouteMapping mapping = validMapping();
        mapping.setMethod("GET");
        assertFalse(validator.validate(mapping).isEmpty());
    }

    @Test
    void rejectsInvalidMethod() {
        RouteCustomizerProperties.RouteMapping mapping = new RouteCustomizerProperties.RouteMapping();
        mapping.setPath("/v1/api/dothis");
        mapping.setMethod("FETCH");
        mapping.setExposeAs(List.of("/api/dothis"));
        Set<ConstraintViolation<RouteCustomizerProperties.RouteMapping>> violations = validator.validate(mapping);
        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsNoTargetsAndNoFilters() {
        RouteCustomizerProperties.RouteMapping mapping = new RouteCustomizerProperties.RouteMapping();
        mapping.setRouteId("Demo-doThis");
        assertFalse(validator.validate(mapping).isEmpty());
    }

    @Test
    void rejectsReplaceOriginalWithoutTargets() {
        RouteCustomizerProperties.RouteMapping mapping = new RouteCustomizerProperties.RouteMapping();
        mapping.setRouteId("Demo-doThis");
        mapping.setReplaceOriginal(true);
        RouteCustomizerProperties.FilterOverride filter = new RouteCustomizerProperties.FilterOverride();
        filter.setName("SomeFilter");
        mapping.setFilters(List.of(filter));
        assertFalse(validator.validate(mapping).isEmpty());
    }

    @Test
    void acceptsFiltersOnlyMappingWithoutReplaceOriginal() {
        RouteCustomizerProperties.RouteMapping mapping = new RouteCustomizerProperties.RouteMapping();
        mapping.setRouteId("Demo-doThis");
        RouteCustomizerProperties.FilterOverride filter = new RouteCustomizerProperties.FilterOverride();
        filter.setName("SomeFilter");
        mapping.setFilters(List.of(filter));
        assertTrue(validator.validate(mapping).isEmpty());
    }
}
