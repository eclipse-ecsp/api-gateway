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

package org.eclipse.ecsp.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class for {@link ApiRoutesRefreshScheduler} functionality.
 */
@ExtendWith(SpringExtension.class)
class ApiRouteRefreshSchedulerTest {

    private ApiRoutesRefreshScheduler apiRoutesRefreshScheduler;

    @Mock
    private IgniteRouteLocator igniteRouteLocator;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        apiRoutesRefreshScheduler = new ApiRoutesRefreshScheduler(igniteRouteLocator);
    }

    @Test
    void testReload() {
        apiRoutesRefreshScheduler.reload();
        Mockito.verify(igniteRouteLocator, Mockito.atLeastOnce()).refreshRoutes();
    }
}
