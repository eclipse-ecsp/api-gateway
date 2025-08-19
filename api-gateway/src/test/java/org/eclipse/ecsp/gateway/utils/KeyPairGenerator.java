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

package org.eclipse.ecsp.gateway.utils;

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utility class to generate a public key, encode it in Base64, and print it with and without PEM headers.
 * This class is primarily used for testing and debugging purposes.
 */
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class KeyPairGenerator {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(KeyPairGenerator.class);
    public static final int KEYSIZE = 2048;
    public static final int INT_64 = 64;

    /**
     * Main method to generate a public key, encode it in Base64, and print it with and without PEM headers.
     *
     * @param args command line arguments
     * @throws Exception if any error occurs during key generation or encoding
     */
    public static void main(String[] args) throws Exception {
        // Generate RSA key pair
        java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(KEYSIZE);
        KeyPair keyPair = keyGen.generateKeyPair();

        // Get the public key
        PublicKey publicKey = keyPair.getPublic();

        // Encode as X.509 DER format
        byte[] encoded = publicKey.getEncoded();

        // Convert to Base64
        String base64PublicKey = Base64.getEncoder().encodeToString(encoded);

        // Format for PEM with headers
        LOGGER.info("With headers:");
        LOGGER.info("-----BEGIN PUBLIC KEY-----");
        printBase64Key(base64PublicKey);
        LOGGER.info("-----END PUBLIC KEY-----");

        LOGGER.info("\nWithout headers (Base64 only):");
        printBase64Key(base64PublicKey);

        // Verify the key can be parsed back
        LOGGER.info("\nVerification:");
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey parsedKey = keyFactory.generatePublic(keySpec);
            LOGGER.info("Key validation: SUCCESS - Generated key can be parsed");
            LOGGER.info("Algorithm: " + parsedKey.getAlgorithm());
            LOGGER.info("Format: " + parsedKey.getFormat());
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Key validation: FAILED", e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("No such algorithm for key generation", e);
        }
    }

    private static void printBase64Key(String base64Key) {
        for (int i = 0; i < base64Key.length(); i += INT_64) {
            LOGGER.info(base64Key.substring(i, Math.min(i + INT_64, base64Key.length())));
        }
    }
}
