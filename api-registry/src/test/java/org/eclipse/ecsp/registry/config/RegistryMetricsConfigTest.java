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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link RegistryMetricsConfig}.
 */
class RegistryMetricsConfigTest {

    private RegistryMetricsConfig config;

    @Mock
    private ConfigurableEnvironment environment;

    private Map<String, Object> systemProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        systemProperties = new java.util.HashMap<>();
        when(environment.getSystemProperties()).thenReturn(systemProperties);
        config = new RegistryMetricsConfig(environment);
    }

    @Test
    void constructor_WithValidEnvironment_SetsManagementProperties() {
        // Verify exposure include contains health and prometheus
        assertTrue(systemProperties.containsKey("management.endpoints.web.exposure.include"));
        String exposureInclude = (String) systemProperties.get("management.endpoints.web.exposure.include");
        assertTrue(exposureInclude.contains("health"));
        assertTrue(exposureInclude.contains("prometheus"));
    }

    @Test
    void constructor_WithValidEnvironment_SetsJmxExcludeAll() {
        assertTrue(systemProperties.containsKey("management.endpoints.jmx.exposure.exclude"));
        String jmxExclude = (String) systemProperties.get("management.endpoints.jmx.exposure.exclude");
        assertTrue(jmxExclude.equals("*"));
    }

    @Test
    void constructor_WithValidEnvironment_SetsWebExclusionList() {
        assertTrue(systemProperties.containsKey("management.endpoints.web.exposure.exclude"));
        String webExclude = (String) systemProperties.get("management.endpoints.web.exposure.exclude");
        assertTrue(webExclude.contains("gateway"));
        assertTrue(webExclude.contains("beans"));
        assertTrue(webExclude.contains("metrics"));
    }

    @Test
    void constructor_WithValidEnvironment_DisablesDefaultMetrics() {
        assertTrue(systemProperties.containsKey("management.endpoints.access.default"));
        assertTrue(systemProperties.containsKey("management.defaults.metrics.export.enabled"));
        
        String accessDefault = (String) systemProperties.get("management.endpoints.access.default");
        String metricsExport = (String) systemProperties.get("management.defaults.metrics.export.enabled");
        
        assertTrue(accessDefault.equals("none"));
        assertTrue(metricsExport.equals("false"));
    }

    @Test
    void registryEndpointFilter_WithHealthEndpoint_ReturnsTrue() {
        ExposableWebEndpoint endpoint = mock(ExposableWebEndpoint.class);
        EndpointId endpointId = mock(EndpointId.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        when(endpointId.toLowerCaseString()).thenReturn("health");

        EndpointFilter<ExposableWebEndpoint> filter = config.registryEndpointFilter();
        
        assertNotNull(filter);
        assertTrue(filter.match(endpoint));
    }

    @Test
    void registryEndpointFilter_WithPrometheusEndpoint_ReturnsTrue() {
        ExposableWebEndpoint endpoint = mock(ExposableWebEndpoint.class);
        EndpointId endpointId = mock(EndpointId.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        when(endpointId.toLowerCaseString()).thenReturn("prometheus");

        EndpointFilter<ExposableWebEndpoint> filter = config.registryEndpointFilter();
        
        assertNotNull(filter);
        assertTrue(filter.match(endpoint));
    }

    @Test
    void registryEndpointFilter_WithMetricsEndpoint_ReturnsFalse() {
        ExposableWebEndpoint endpoint = mock(ExposableWebEndpoint.class);
        EndpointId endpointId = mock(EndpointId.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        when(endpointId.toLowerCaseString()).thenReturn("metrics");

        EndpointFilter<ExposableWebEndpoint> filter = config.registryEndpointFilter();
        
        assertNotNull(filter);
        assertFalse(filter.match(endpoint));
    }

    @Test
    void registryEndpointFilter_WithBeansEndpoint_ReturnsFalse() {
        ExposableWebEndpoint endpoint = mock(ExposableWebEndpoint.class);
        EndpointId endpointId = mock(EndpointId.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        when(endpointId.toLowerCaseString()).thenReturn("beans");

        EndpointFilter<ExposableWebEndpoint> filter = config.registryEndpointFilter();
        
        assertNotNull(filter);
        assertFalse(filter.match(endpoint));
    }

    @Test
    void registryEndpointFilter_WithGatewayEndpoint_ReturnsFalse() {
        ExposableWebEndpoint endpoint = mock(ExposableWebEndpoint.class);
        EndpointId endpointId = mock(EndpointId.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        when(endpointId.toLowerCaseString()).thenReturn("gateway");

        EndpointFilter<ExposableWebEndpoint> filter = config.registryEndpointFilter();
        
        assertNotNull(filter);
        assertFalse(filter.match(endpoint));
    }

    @Test
    void registryEndpointFilter_WithUnknownEndpoint_ReturnsFalse() {
        ExposableWebEndpoint endpoint = mock(ExposableWebEndpoint.class);
        EndpointId endpointId = mock(EndpointId.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        when(endpointId.toLowerCaseString()).thenReturn("unknown");

        EndpointFilter<ExposableWebEndpoint> filter = config.registryEndpointFilter();
        
        assertNotNull(filter);
        assertFalse(filter.match(endpoint));
    }

    @Test
    void registryEndpointFilter_CalledMultipleTimes_ReturnsSameLogic() {
        ExposableWebEndpoint healthEndpoint = mock(ExposableWebEndpoint.class);
        EndpointId healthId = mock(EndpointId.class);
        when(healthEndpoint.getEndpointId()).thenReturn(healthId);
        when(healthId.toLowerCaseString()).thenReturn("health");

        ExposableWebEndpoint metricsEndpoint = mock(ExposableWebEndpoint.class);
        EndpointId metricsId = mock(EndpointId.class);
        when(metricsEndpoint.getEndpointId()).thenReturn(metricsId);
        when(metricsId.toLowerCaseString()).thenReturn("metrics");

        EndpointFilter<ExposableWebEndpoint> filter = config.registryEndpointFilter();
        
        // First call
        assertTrue(filter.match(healthEndpoint));
        assertFalse(filter.match(metricsEndpoint));
        
        // Second call - should be consistent
        assertTrue(filter.match(healthEndpoint));
        assertFalse(filter.match(metricsEndpoint));
    }
}
