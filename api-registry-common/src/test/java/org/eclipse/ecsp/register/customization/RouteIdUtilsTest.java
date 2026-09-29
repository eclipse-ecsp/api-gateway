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
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteIdUtilsTest {

    @Test
    void computeRouteIdNormalizesUnderscoresAndJoinsTagAndOperationId() {
        Operation operation = new Operation();
        operation.setTags(List.of("Demo"));
        operation.setOperationId("do_this");
        assertEquals("Demo-do-this", RouteIdUtils.computeRouteId(operation));
    }

    @Test
    void computeRouteIdReturnsNullWhenOperationIdMissing() {
        Operation operation = new Operation();
        operation.setTags(List.of("Demo"));
        assertNull(RouteIdUtils.computeRouteId(operation));
    }

    @Test
    void computeRouteIdReturnsNullWhenTagsMissing() {
        Operation operation = new Operation();
        operation.setOperationId("doThis");
        assertNull(RouteIdUtils.computeRouteId(operation));
    }

    @Test
    void computeRouteIdReturnsNullWhenTagsEmpty() {
        Operation operation = new Operation();
        operation.setTags(List.of());
        operation.setOperationId("doThis");
        assertNull(RouteIdUtils.computeRouteId(operation));
    }

    @Test
    void computeRouteIdReturnsNullForNullOperation() {
        assertNull(RouteIdUtils.computeRouteId(null));
    }

    @Test
    void requireRouteIdReturnsSameValueAsComputeRouteId() {
        Operation operation = new Operation();
        operation.setTags(List.of("Demo"));
        operation.setOperationId("doThis");
        assertEquals(RouteIdUtils.computeRouteId(operation), RouteIdUtils.requireRouteId(operation));
    }

    @Test
    void requireRouteIdThrowsWhenRouteIdCannotBeComputed() {
        Operation operation = new Operation();
        assertThrows(NullPointerException.class, () -> RouteIdUtils.requireRouteId(operation));
    }
}
