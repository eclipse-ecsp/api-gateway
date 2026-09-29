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

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.utils.ObjectMapperUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * Computes the canonical content digest ({@code snapshotId}) of a route-snapshot reconciliation
 * request.
 *
 * <p>Used identically on both sides: the client ({@code ApiRouteRegistrationService}) computes
 * it before sending; the server ({@code ApiRouteService.reconcile}) independently recomputes it
 * over the received request and rejects a mismatch, so the digest can never be spoofed by a
 * client bug or tampering in transit.
 *
 * <p>Canonicalization sorts routes by id and recursively sorts JSON object field names, so byte-
 * identical route content always produces the same digest regardless of map/list iteration order.
 */
public final class SnapshotDigestUtil {

    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String DIGEST_PREFIX = "sha256:";

    private SnapshotDigestUtil() {
    }

    /**
     * Computes the canonical snapshot digest for a deployment generation and route set.
     *
     * @param generation the deployment generation the snapshot is submitted at
     * @param routes     the desired route set (order-independent; sorted internally by id)
     * @return the digest, prefixed with {@value #DIGEST_PREFIX}
     */
    public static String compute(long generation, List<RouteDefinition> routes) {
        List<RouteDefinition> sorted = new ArrayList<>(routes);
        sorted.sort(Comparator.comparing(RouteDefinition::getId, Comparator.nullsFirst(Comparator.naturalOrder())));
        JsonNode routesNode = ObjectMapperUtil.getObjectMapper().valueToTree(sorted);
        String canonical = generation + "|" + canonicalize(routesNode);
        return DIGEST_PREFIX + digestHex(canonical);
    }

    private static String digestHex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(DIGEST_ALGORITHM + " must be available on every supported JVM", ex);
        }
    }

    private static String canonicalize(JsonNode node) {
        if (node.isObject()) {
            return canonicalizeObject(node);
        }
        if (node.isArray()) {
            return canonicalizeArray(node);
        }
        return node.toString();
    }

    private static String canonicalizeObject(JsonNode node) {
        List<String> fieldNames = new ArrayList<>();
        node.fieldNames().forEachRemaining(fieldNames::add);
        fieldNames.sort(Comparator.naturalOrder());
        StringBuilder builder = new StringBuilder("{");
        for (String field : fieldNames) {
            builder.append('"').append(field).append("\":").append(canonicalize(node.get(field))).append(',');
        }
        return builder.append('}').toString();
    }

    private static String canonicalizeArray(JsonNode node) {
        StringBuilder builder = new StringBuilder("[");
        for (JsonNode child : node) {
            builder.append(canonicalize(child)).append(',');
        }
        return builder.append(']').toString();
    }
}
