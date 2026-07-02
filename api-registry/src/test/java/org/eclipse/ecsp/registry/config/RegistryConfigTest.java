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

package org.eclipse.ecsp.registry.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link RegistryConfig}.
 */
class RegistryConfigTest {

    @Test
    void objectMapperHasExpectedFeaturesEnabled() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertNotNull(mapper);
        assertTrue(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        assertTrue(mapper.isEnabled(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION));
        assertTrue(mapper.isEnabled(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES));
    }

    @Test
    void registryDisableEndpointFilterDisablesAllEndpoints() {
        RegistryConfig config = new RegistryConfig();
        ExposableWebEndpoint endpoint = mock(ExposableWebEndpoint.class);
        EndpointId endpointId = mock(EndpointId.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        when(endpointId.toLowerCaseString()).thenReturn("health");

        EndpointFilter<ExposableWebEndpoint> filter = config.registryDisableEndpointFilter();
        
        assertNotNull(filter);
        assertFalse(filter.match(endpoint));
    }

    @Test
    void noSqlDatabaseConfigCanBeInstantiated() {
        RegistryConfig.NoSqlDatabaseConfig config = new RegistryConfig.NoSqlDatabaseConfig();
        
        assertNotNull(config);
    }

    @Test
    void sqlDatabaseConfigCanBeInstantiated() {
        RegistryConfig.SqlDatabaseConfig config = new RegistryConfig.SqlDatabaseConfig();
        
        assertNotNull(config);
    }
}
