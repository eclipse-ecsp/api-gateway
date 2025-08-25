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

package org.eclipse.ecsp.gateway.plugins.keyloaders;

import com.nimbusds.jose.util.X509CertUtils;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.PublicKeyType;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import javax.management.openmbean.InvalidKeyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of PublicKeyLoader for PEM format keys.
 * Supports RSA, EC, and Edwards-curve public keys from certificates and PEM files.
 *
 * @author Abhishek Kumar
 */
@Component
public class PemPublicKeyLoader implements PublicKeyLoader {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PemPublicKeyLoader.class);
    private static final String[] SUPPORTED_ALGORITHMS = {"RSA", "EC", "EdDSA", "Ed25519"};

    /**
     * Loads a public key from PEM format source.
     *
     * @param config the public key source configuration
     * @return the loaded PublicKey
     */
    @Override
    public Map<String, PublicKey> loadKeys(PublicKeySource config) {
        try {
            String keyFile = Files.readString(Path.of(config.getLocation()));
            LOGGER.debug("PublicKey file: {} with contents extracted: {}", config.getLocation(), keyFile);
            PublicKey publicKey = parseKeyFile(keyFile);

            // Create result map - include DEFAULT key if this is a default configuration
            Map<String, PublicKey> keyMap = new HashMap<>();
            keyMap.put(config.getId(), publicKey);
            if (config.isDefault()) {
                LOGGER.info("Loaded default public key with ID: {}", config.getId());
                keyMap.put("DEFAULT", publicKey);
            }
            return keyMap;
        } catch (Exception e) {
            LOGGER.error("Error loading PEM public key from file: {}", config.getLocation(), e);
            throw new InvalidKeyException("Failed to load PEM public key from file: " + config.getLocation());
        }
    }

    @Override
    public PublicKeyType getType() {
        return PublicKeyType.PEM;
    }

    /**
     * Parse the public key from the given key file content.
     * Supports both PEM files and certificate strings.
     *
     * @param keyFile the key file content
     * @return the public key
     */
    private PublicKey parseKeyFile(String keyFile) {
        if (keyFile.contains(GatewayConstants.CERTIFICATE_HEADER_PREFIX)) {
            return parseCertificate(keyFile);
        }

        String cleanKey = cleanPemContent(keyFile);
        return parsePublicKey(cleanKey);
    }

    /**
     * Remove PEM headers, footers, and line breaks from the key file content.
     *
     * @param keyFile the key file content
     * @return the cleaned key content
     */
    private String cleanPemContent(String keyFile) {
        return keyFile
            .replaceAll("-----BEGIN [A-Z ]+-----", "")
            .replaceAll("-----END [A-Z ]+-----", "")
            .replaceAll(System.lineSeparator(), "")
            .trim();
    }

    /**
     * Parse the certificate string to extract the public key.
     *
     * @param certStr the certificate string
     * @return the public key
     */
    private PublicKey parseCertificate(String certStr) {
        try {
            X509Certificate cert = X509CertUtils.parse(certStr);
            PublicKey publicKey = cert.getPublicKey();
            LOGGER.debug("Certificate parsed successfully. Public key algorithm: {}", publicKey.getAlgorithm());
            return publicKey;
        } catch (Exception e) {
            LOGGER.error("Failed to parse certificate: {}", e.getMessage(), e);
            throw new InvalidKeyException("Invalid certificate: " + e);
        }
    }

    /**
     * Parse the public key string to extract the public key.
     *
     * @param keyStr the public key string
     * @return the public key
     */
    private PublicKey parsePublicKey(String keyStr) {
        try {
            byte[] keyBytes = Base64.decodeBase64(keyStr);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

            for (String algorithm : SUPPORTED_ALGORITHMS) {
                PublicKey publicKey = tryParsePublicKeyWithAlgo(algorithm, keySpec);
                if (publicKey != null) {
                    LOGGER.debug("Successfully parsed public key with algorithm: {}", algorithm);
                    return publicKey;
                }
            }
            throw new InvalidKeyException("Unsupported key type or invalid format");
        } catch (InvalidKeyException e) {
            throw e; // Re-throw InvalidKeyException for clarity
        } catch (Exception e) {
            LOGGER.error("Failed to parse public key: {}", e.getMessage(), e);
            throw new InvalidKeyException("Invalid public key: " + e.getMessage());
        }
    }

    private static PublicKey tryParsePublicKeyWithAlgo(String algorithm, X509EncodedKeySpec keySpec) {
        try {
            KeyFactory keyFactory = java.security.KeyFactory.getInstance(algorithm);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            LOGGER.debug("Successfully parsed {} public key", algorithm);
            return publicKey;
        } catch (Exception e) {
            LOGGER.warn("Failed to parse public key with algorithm {}", algorithm);
            // Try next algorithm
        }
        return null;
    }
}
