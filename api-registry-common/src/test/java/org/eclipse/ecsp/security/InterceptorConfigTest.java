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

package org.eclipse.ecsp.security;

import org.eclipse.ecsp.config.InterceptorConfig;
import org.eclipse.ecsp.interceptors.HeaderInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

/**
 * InterceptorConfigTest.
 */
@ExtendWith(SpringExtension.class)
class InterceptorConfigTest {

    @Mock
    HeaderInterceptor headerInterceptor;
    @InjectMocks
    private InterceptorConfig interceptorConfig;

    @Test
    void testAddInterceptors() {
        InterceptorRegistry mocked = Mockito.mock(InterceptorRegistry.class);
        interceptorConfig.addInterceptors(mocked);
        Mockito.verify(mocked, Mockito.atLeastOnce()).addInterceptor(headerInterceptor);
    }
}
