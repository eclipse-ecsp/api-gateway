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

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link RegistryConfig}.
 */
class RegistryConfigTest {

    @Test
    void objectMapperBuilderCustomizerReturnsCustomizer() {
        RegistryConfig config = new RegistryConfig();
        
        assertNotNull(config.objectMapperBuilderCustomizer());
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
