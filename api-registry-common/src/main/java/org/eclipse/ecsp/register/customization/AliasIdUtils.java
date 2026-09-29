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
import org.springframework.http.HttpMethod;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Builds deterministic, collision-resistant {@code operationId}s for route-customizer
 * fan-out clones.
 *
 * <p>The id is derived from the HTTP method and the normalized target path rather than from a
 * lossy sanitization of the target path alone, so two different target paths can never
 * accidentally sanitize to the same clone id. The id is still not treated as proof of
 * uniqueness by itself &mdash; {@link org.eclipse.ecsp.register.RouteSetValidator} independently
 * validates the final, complete route set before registration.
 */
public final class AliasIdUtils {

    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final int DIGEST_HEX_LENGTH = 12;

    private AliasIdUtils() {
    }

    /**
     * Creates a deterministic {@code operationId} for a fan-out clone.
     *
     * @param originalOperation the matched original operation being cloned
     * @param method            the HTTP method the alias is registered under
     * @param targetPath        the normalized target path
     * @return a new, deterministic {@code operationId} distinct from the original
     */
    public static String createOperationId(Operation originalOperation, HttpMethod method, String targetPath) {
        String normalizedOriginalId = originalOperation.getOperationId().replace("_", "-");
        String digestInput = method.name() + "\n" + targetPath;
        return normalizedOriginalId + "-alias-" + digest(digestInput);
    }

    private static String digest(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] hash = messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(hash);
            return hex.substring(0, DIGEST_HEX_LENGTH);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(DIGEST_ALGORITHM + " must be available on every supported JVM", ex);
        }
    }
}
