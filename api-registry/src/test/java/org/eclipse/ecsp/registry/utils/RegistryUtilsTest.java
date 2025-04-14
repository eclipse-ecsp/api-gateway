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

package org.eclipse.ecsp.registry.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test RegistryUtils.
 */
@ExtendWith(SpringExtension.class)
class RegistryUtilsTest {

    public static final int CODE_600 = 600;
    public static final int CODE_500 = 500;
    public static final int CODE_401 = 401;
    public static final int CODE_200 = 200;

    @Test
    void testSuccessOutcome() {
        String result = RegistryUtils.getOutcomeFromHttpStatus(HttpStatusCode.valueOf(CODE_200));
        Assertions.assertEquals("SUCCESS", result);
    }

    @Test
    void testClientFailureOutcome() {
        String result = RegistryUtils.getOutcomeFromHttpStatus(HttpStatusCode.valueOf(CODE_401));
        Assertions.assertEquals("CLIENT_ERROR", result);
    }

    @Test
    void testServerErrorOutcome() {
        String result = RegistryUtils.getOutcomeFromHttpStatus(HttpStatusCode.valueOf(CODE_500));
        Assertions.assertEquals("SERVER_ERROR", result);
    }

    @Test
    void testUnknownOutcome() {
        String result = RegistryUtils.getOutcomeFromHttpStatus(HttpStatusCode.valueOf(CODE_600));
        Assertions.assertEquals("UNKNOWN", result);
    }
}
