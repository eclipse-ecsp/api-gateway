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

import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import java.util.List;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliasIdUtilsTest {

    private Operation operation(String operationId) {
        Operation operation = new Operation();
        operation.setTags(List.of("Demo"));
        operation.setOperationId(operationId);
        return operation;
    }

    @Test
    void createOperationIdIsDeterministic() {
        Operation op = operation("doThis");
        String first = AliasIdUtils.createOperationId(op, HttpMethod.GET, "/api/dothis");
        String second = AliasIdUtils.createOperationId(op, HttpMethod.GET, "/api/dothis");
        assertEquals(first, second);
    }

    @Test
    void createOperationIdDiffersByTargetPath() {
        Operation op = operation("doThis");
        String first = AliasIdUtils.createOperationId(op, HttpMethod.GET, "/api/dothis");
        String second = AliasIdUtils.createOperationId(op, HttpMethod.GET, "/api/perform");
        assertNotEquals(first, second);
    }

    @Test
    void createOperationIdDiffersByMethod() {
        Operation op = operation("doThis");
        String first = AliasIdUtils.createOperationId(op, HttpMethod.GET, "/api/dothis");
        String second = AliasIdUtils.createOperationId(op, HttpMethod.POST, "/api/dothis");
        assertNotEquals(first, second);
    }

    @Test
    void createOperationIdHasExpectedShape() {
        Operation op = operation("do_this");
        String id = AliasIdUtils.createOperationId(op, HttpMethod.GET, "/api/dothis");
        assertTrue(Pattern.matches("do-this-alias-[0-9a-f]{12}", id), id);
    }

    @Test
    void differentPunctuationPathsThatSanitizeSameDoNotCollide() {
        Operation op = operation("doThis");
        String first = AliasIdUtils.createOperationId(op, HttpMethod.GET, "/api-dothis");
        String second = AliasIdUtils.createOperationId(op, HttpMethod.GET, "/api.dothis");
        assertNotEquals(first, second);
    }
}
