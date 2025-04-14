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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class for HeaderContext.
 */
@ExtendWith(SpringExtension.class)
class HeaderContextTest {

    @Test
    void testClear() {
        // Set user details
        HeaderContext.setUser("testUser", Set.of("scope1", "scope2"), Set.of("overrideScope1"));

        // Ensure user details are set
        assertNotNull(HeaderContext.getUserDetails());

        // Clear the context
        HeaderContext.clear();

        // Ensure user details are cleared
        assertNull(HeaderContext.getUserDetails());
    }
}