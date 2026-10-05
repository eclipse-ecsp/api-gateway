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
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotDigestUtilTest {

    private RouteDefinition route(String id) {
        RouteDefinition route = new RouteDefinition();
        route.setId(id);
        route.setUri(URI.create("http://svc:8080/"));
        route.setMetadata(new HashMap<>());
        return route;
    }

    @Test
    void computeIsOrderIndependent() {
        List<RouteDefinition> ordered = List.of(route("a"), route("b"));
        List<RouteDefinition> reversed = List.of(route("b"), route("a"));
        assertEquals(SnapshotDigestUtil.compute(1, ordered), SnapshotDigestUtil.compute(1, reversed));
    }

    @Test
    void computeDiffersByGeneration() {
        List<RouteDefinition> routes = List.of(route("a"));
        assertNotEquals(SnapshotDigestUtil.compute(1, routes), SnapshotDigestUtil.compute(2, routes));
    }

    @Test
    void computeDiffersByContent() {
        assertNotEquals(
                SnapshotDigestUtil.compute(1, List.of(route("a"))),
                SnapshotDigestUtil.compute(1, List.of(route("b"))));
    }

    @Test
    void computeStartsWithSha256Prefix() {
        assertTrue(SnapshotDigestUtil.compute(1, List.of(route("a"))).startsWith("sha256:"));
    }

    @Test
    void computeIsStableAcrossRepeatedCalls() {
        List<RouteDefinition> routes = List.of(route("a"), route("b"));
        assertEquals(SnapshotDigestUtil.compute(5, routes), SnapshotDigestUtil.compute(5, routes));
    }

    @Test
    void routeIdDigestIsOrderIndependentAndDependsOnGenerationAndIds() {
        String digest = SnapshotDigestUtil.computeRouteIds(1, List.of("a", "b"));
        assertEquals(digest, SnapshotDigestUtil.computeRouteIds(1, List.of("b", "a")));
        assertNotEquals(digest, SnapshotDigestUtil.computeRouteIds(2, List.of("a", "b")));
        assertNotEquals(digest, SnapshotDigestUtil.computeRouteIds(1, List.of("a")));
        assertTrue(digest.startsWith("sha256:"));
    }
}
