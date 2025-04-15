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

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.X509CertUtils;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.management.openmbean.InvalidKeyException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Component to load and parse JWT public keys from files.
 * This class is responsible for reading public key files, parsing them,
 * and creating JWT parsers for signature verification.
 */
@Component
public class JwtPublicKeyLoader {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(JwtPublicKeyLoader.class);
    @Getter
    protected final Map<String, JwtParser> jwtParsers = new LinkedHashMap<>();
    @Value("${jwt.publicKeyFiles}")
    private String[] jwtPublicKeyFiles;

    @Value("${jwt.publicKeyFileBasePath}")
    private String jwtPublicKeyFilePath;

    /**
     * prepare the JWT parsers for signature verification.
     *
     * @throws IOException if an error occurs while reading the public key files
     */
    @PostConstruct
    public void init() throws IOException {
        LOGGER.debug("Extract public-key signature");
        for (final String jwtPublicKeyFile : jwtPublicKeyFiles) {
            LOGGER.debug("Parsing {} public-key file", jwtPublicKeyFile);
            PublicKey publicKeySignature;
            try {
                String keyFile = Files.readString(Path.of(jwtPublicKeyFilePath, jwtPublicKeyFile));
                LOGGER.debug("PublicKey file: {} with contents extracted: {}", jwtPublicKeyFile, keyFile);
                if ((!keyFile.contains(GatewayConstants.CERTIFICATE_HEADER_PREFIX))
                        && (!keyFile.contains(GatewayConstants.PUBLICKEY_HEADER_PREFIX))) {
                    LOGGER.info("Parsing key-file:{} without prefix-headers", jwtPublicKeyFile);
                    publicKeySignature = parsePublicKey(keyFile);
                    LOGGER.debug("Extracted signature from public-key: {} ", publicKeySignature);
                } else if (keyFile.contains(GatewayConstants.CERTIFICATE_HEADER_PREFIX)) {
                    // Identify Key Type from Header_Prefix and extract Key Signature
                    LOGGER.debug("Certificate: " + keyFile);
                    publicKeySignature = parseCertificate(keyFile);
                    LOGGER.debug("Extracted signature from certificate: {} ", publicKeySignature);
                } else if (keyFile.contains(GatewayConstants.PUBLICKEY_HEADER_PREFIX)) {
                    LOGGER.debug("PublicKey: " + keyFile);
                    String publicKeyPem =
                            keyFile.replace("-----BEGIN PUBLIC KEY-----", "").replaceAll(System.lineSeparator(), "")
                                    .replace("-----END PUBLIC KEY-----", "");
                    publicKeySignature = parsePublicKey(publicKeyPem);
                    LOGGER.debug("Extracted signature from publicKey: {} ", publicKeySignature);
                } else {
                    throw new InvalidKeyException("Invalid key file");
                }
                // Append all key signature to parser-map
                jwtParsers.put(jwtPublicKeyFile, Jwts.parser().verifyWith(publicKeySignature).build());
                LOGGER.info("PublicKey {} parsed and signature inserted into jwtParser", jwtPublicKeyFile);
            } catch (Exception ex) {
                LOGGER.error("Error while loading the JWT Public Key: {} with exception: {}", jwtPublicKeyFile, ex);
            }
        }
        LOGGER.debug("JWT Parser Map Entry-Set Data: {}", getJwtParsers().entrySet().toString());
    }

    /**
     * Parse the certificate string to extract the public key.
     *
     * @param certStr the certificate string
     * @return the public key
     */
    public PublicKey parseCertificate(String certStr) {
        try {
            X509Certificate cert = X509CertUtils.parse(certStr);
            RSAKey rsaJwk = RSAKey.parse(cert);
            LOGGER.debug("rsaJWK: {}", rsaJwk);
            return cert.getPublicKey();
        } catch (Exception e) {
            LOGGER.error("Error while parsing cert: {}", e);
            throw new InvalidKeyException("Invalid certificate");
        }
    }

    /**
     * Parse the public key string to extract the public key.
     *
     * @param keyFileStr the public key string
     * @return the public key
     */
    public PublicKey parsePublicKey(String keyFileStr) {
        try {
            LOGGER.info("Key: {}", keyFileStr);
            byte[] publicKeyBytes = Base64.decodeBase64(keyFileStr);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            LOGGER.debug("RSA key instance: {}", keyFactory);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            LOGGER.debug("x509EncodedKeySpec: {}", keySpec);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            LOGGER.error("Error while parsing public key: {}", e);
            throw new InvalidKeyException("Invalid public key");
        }
    }
}
