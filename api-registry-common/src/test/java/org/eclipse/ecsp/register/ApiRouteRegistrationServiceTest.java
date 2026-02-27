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

package org.eclipse.ecsp.register;

import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.utils.RegistryCommonTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for ApiRouteRegistrationService.
 */
@ExtendWith(MockitoExtension.class)
class ApiRouteRegistrationServiceTest {

    @Mock
    ApiRoutesLoader apiRoutesLoader;
    
    @Mock
    private RestTemplate restTemplate;
    
    private ApiRouteRegistrationService apiRouteRegistrationService;
    
    @BeforeEach
    void setUp() {
        apiRouteRegistrationService = new ApiRouteRegistrationService(apiRoutesLoader, restTemplate);
    }

    @Test
    void testRegister() throws Exception {
        List<RouteDefinition> routeDefinitionList = new ArrayList<>();
        routeDefinitionList.add(RegistryCommonTestUtil.getRouteDefination());
        Mockito.when(apiRoutesLoader.getApiRoutes()).thenReturn(routeDefinitionList);
        apiRouteRegistrationService.register();
        Mockito.verify(restTemplate, Mockito.atLeastOnce())
                .postForEntity(Mockito.anyString(), Mockito.any(), Mockito.any());
    }
}
