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

package org.eclipse.ecsp.gateway.config;

import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link RouteOrderOverrideProperties}.
 */
class RouteOrderOverridePropertiesTest {

    @Test
    void defaultsAreDisabledWithNoMappings() {
        RouteOrderOverrideProperties properties = new RouteOrderOverrideProperties();

        assertFalse(properties.isEnabled());
        assertTrue(properties.getMappings().isEmpty());
        assertDoesNotThrow(properties::validate);
    }

    @Test
    void bindsEnabledAndMappingsFromConfiguration() {
        Map<String, String> source = Map.of(
                GatewayConstants.ROUTE_ORDER_OVERRIDE_PREFIX + ".enabled", "true",
                GatewayConstants.ROUTE_ORDER_OVERRIDE_PREFIX + ".mappings[0].route-id", "Users-getUser",
                GatewayConstants.ROUTE_ORDER_OVERRIDE_PREFIX + ".mappings[0].order", "-10",
                GatewayConstants.ROUTE_ORDER_OVERRIDE_PREFIX + ".mappings[1].route-id", "Order Service-getOrder",
                GatewayConstants.ROUTE_ORDER_OVERRIDE_PREFIX + ".mappings[1].order", "100");

        RouteOrderOverrideProperties properties = new Binder(new MapConfigurationPropertySource(source))
                .bind(GatewayConstants.ROUTE_ORDER_OVERRIDE_PREFIX, RouteOrderOverrideProperties.class)
                .get();

        assertTrue(properties.isEnabled());
        assertEquals(2, properties.getMappings().size());
        assertEquals("Users-getUser", properties.getMappings().get(0).getRouteId());
        assertEquals(-10, properties.getMappings().get(0).getOrder());
        assertEquals("Order Service-getOrder", properties.getMappings().get(1).getRouteId());
        assertEquals(100, properties.getMappings().get(1).getOrder());
        assertDoesNotThrow(properties::validate);
    }

    @Test
    void validateAcceptsValidMappingsWhenEnabled() {
        RouteOrderOverrideProperties properties = enabledProperties(
                mapping("Users-getUser", -10), mapping("Users-listUsers", 0));

        assertDoesNotThrow(properties::validate);
    }

    @Test
    void validateIgnoresInvalidMappingsWhenDisabled() {
        RouteOrderOverrideProperties properties = new RouteOrderOverrideProperties();
        properties.setMappings(new ArrayList<>(List.of(mapping(" ", null))));

        assertDoesNotThrow(properties::validate);
    }

    @Test
    void validateRejectsBlankRouteId() {
        RouteOrderOverrideProperties properties = enabledProperties(mapping(" ", 1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, properties::validate);

        assertTrue(exception.getMessage().contains("index 0"));
    }

    @Test
    void validateRejectsMissingRouteId() {
        RouteOrderOverrideProperties properties = enabledProperties(mapping(null, 1));

        assertThrows(IllegalArgumentException.class, properties::validate);
    }

    @Test
    void validateRejectsNullMapping() {
        RouteOrderOverrideProperties properties = new RouteOrderOverrideProperties();
        properties.setEnabled(true);
        List<RouteOrderOverrideProperties.Mapping> mappings = new ArrayList<>();
        mappings.add(null);
        properties.setMappings(mappings);

        assertThrows(IllegalArgumentException.class, properties::validate);
    }

    @Test
    void validateRejectsMissingOrder() {
        RouteOrderOverrideProperties properties = enabledProperties(mapping("Users-getUser", null));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, properties::validate);

        assertTrue(exception.getMessage().contains("Users-getUser"));
    }

    @Test
    void validateRejectsDuplicateRouteId() {
        RouteOrderOverrideProperties properties = enabledProperties(
                mapping("Users-getUser", 1), mapping("Users-getUser", 10));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, properties::validate);

        assertTrue(exception.getMessage().contains("Duplicate"));
    }

    private static RouteOrderOverrideProperties enabledProperties(RouteOrderOverrideProperties.Mapping... mappings) {
        RouteOrderOverrideProperties properties = new RouteOrderOverrideProperties();
        properties.setEnabled(true);
        properties.setMappings(new ArrayList<>(List.of(mappings)));
        return properties;
    }

    private static RouteOrderOverrideProperties.Mapping mapping(String routeId, Integer order) {
        RouteOrderOverrideProperties.Mapping mapping = new RouteOrderOverrideProperties.Mapping();
        mapping.setRouteId(routeId);
        mapping.setOrder(order);
        return mapping;
    }
}
