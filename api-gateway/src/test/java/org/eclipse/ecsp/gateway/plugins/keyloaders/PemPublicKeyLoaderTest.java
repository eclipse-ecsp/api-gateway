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

import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.PublicKeyType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.management.openmbean.InvalidKeyException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Map;

/**
 * Unit tests for PemPublicKeyLoader.
 * Tests cover loading public keys from PEM files, handling various formats,
 * and ensuring correct error handling for invalid inputs.
 */
@ExtendWith(SpringExtension.class)
class PemPublicKeyLoaderTest {

    public static final int TWO = 2;
    private PemPublicKeyLoader pemPublicKeyLoader;

    @TempDir
    Path tempDir;

    private static final String VALID_PUBLIC_KEY_WITH_HEADERS = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlq4Vi0Mimutc59zZiggT
            qk8e9wxDVqhSC8KnYwMMhKy7qvcEPEdxVr3wH/tyCLTkxcALIfj7GSzHFFhQGOyc
            FuTJeOLIgds1zgdHJW3J5Y06q/KDR9eQ3FVbULS2tmSqh1Sin4WyAHsPaSRlov2L
            aZsgGxhNpkdKPZIT68zdoXB4jDyLeXeRaVdA+Chc9Vj7pWkXyfmbHfQClTGR0rGj
            A+wWpWvRHAvv51QYV8i2G6LYFaJdEM+9lzR20JFunXYTUpAZ3kpM9I4j0nEv7xhA
            VgQNiXjbH0mIORh5lPaFJ9lTflncsE5cxgLV3S+ylR1edQJQTbm88DgzGJKpB+rP
            jwIDAQAB
            -----END PUBLIC KEY-----
            """;

    private static final String VALID_PUBLIC_KEY_WITHOUT_HEADERS = """
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlq4Vi0Mimutc59zZiggT
            qk8e9wxDVqhSC8KnYwMMhKy7qvcEPEdxVr3wH/tyCLTkxcALIfj7GSzHFFhQGOyc
            FuTJeOLIgds1zgdHJW3J5Y06q/KDR9eQ3FVbULS2tmSqh1Sin4WyAHsPaSRlov2L
            aZsgGxhNpkdKPZIT68zdoXB4jDyLeXeRaVdA+Chc9Vj7pWkXyfmbHfQClTGR0rGj
            A+wWpWvRHAvv51QYV8i2G6LYFaJdEM+9lzR20JFunXYTUpAZ3kpM9I4j0nEv7xhA
            VgQNiXjbH0mIORh5lPaFJ9lTflncsE5cxgLV3S+ylR1edQJQTbm88DgzGJKpB+rP
            jwIDAQAB
            """;

    private static final String VALID_CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIIDQTCCAimgAwIBAgITBmyfz5m/jAo54vB4ikPmljZbyjANBgkqhkiG9w0BAQsF
            ADA5MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRkwFwYDVQQDExBBbWF6
            b24gUm9vdCBDQSAxMB4XDTE1MDUyNjAwMDAwMFoXDTM4MDExNzAwMDAwMFowOTEL
            MAkGA1UEBhMCVVMxDzANBgNVBAoTBkFtYXpvbjEZMBcGA1UEAxMQQW1hem9uIFJv
            b3QgQ0EgMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALJ4gHHKeNXj
            ca9HgFB0fW7Y14h29Jlo91ghYPl0hAEvrAIthtOgQ3pOsqTQNroBvo3bSMgHFzZM
            9O6II8c+6zf1tRn4SWiw3te5djgdYZ6k/oI2peVKVuRF4fn9tBb6dNqcmzU5L/qw
            IFAGbHrQgLKm+a/sRxmPUDgH3KKHOVj4utWp+UhnMJbulHheb4mjUcAwhmahRWa6
            VOujw5H5SNz/0egwLX0tdHA114gk957EWW67c4cX8jJGKLhD+rcdqsq08p8kDi1L
            93FcXmn/6pUCyziKrlA4b9v7LWIbxcceVOF34GfID5yHI9Y/QCB/IIDEgEw+OyQm
            jgSubJrIqg0CAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMC
            AYYwHQYDVR0OBBYEFIQYzIU07LwMlJQuCFmcx7IQTgoIMA0GCSqGSIb3DQEBCwUA
            A4IBAQCY8jdaQZChGsV2USggNiMOruYou6r4lK5IpDB/G/wkjUu0yKGX9rbxenDI
            U5PMCCjjmCXPI6T53iHTfIuJruydjsw2hUwsqdXGhYczqZr8HdWvdxYwZOVKLq+Q
            KzjSmjTXtBggKLbMNl8VGlxX0YWFwBGKSXzBN9uS6yOgf0ew/3XjYp9xwu+0fX2z
            +jKa8h+0QgKCLu8Ay5GNJSnCODPzRWXPJAyRFn6Dy0gFIFD3LqhYQlY9AZMA9VUM
            rRHGNOxcJXjeBFKi6XD/xLrMrUlRWgH0ug7WllHcVBqbLcA6g0OWsI6Wn2Lzv6Uz
            TKTrVF9O9xPjBkPKP0yAUGfWOKG9
            -----END CERTIFICATE-----""";

    @BeforeEach
    void setUp() {
        pemPublicKeyLoader = new PemPublicKeyLoader();
    }

    @Test
    void loadsPublicKeyWithHeadersSuccessfully() throws IOException {
        Path keyFile = tempDir.resolve("public_key.pem");
        Files.writeString(keyFile, VALID_PUBLIC_KEY_WITH_HEADERS);

        PublicKeySource config = createPublicKeySource("test-key", keyFile.toString(), false);

        Map<String, PublicKey> result = pemPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.containsKey("test-key"));
        Assertions.assertNotNull(result.get("test-key"));
    }

    @Test
    void loadsPublicKeyWithoutHeadersSuccessfully() throws IOException {
        Path keyFile = tempDir.resolve("public_key_no_headers.pem");
        Files.writeString(keyFile, VALID_PUBLIC_KEY_WITHOUT_HEADERS);

        PublicKeySource config = createPublicKeySource("test-key", keyFile.toString(), false);

        Map<String, PublicKey> result = pemPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.containsKey("test-key"));
        Assertions.assertNotNull(result.get("test-key"));
    }

    @Test
    void loadsDefaultPublicKeyWithBothKeysInMap() throws IOException {
        Path keyFile = tempDir.resolve("default_key.pem");
        Files.writeString(keyFile, VALID_PUBLIC_KEY_WITH_HEADERS);

        PublicKeySource config = createPublicKeySource("default-key", keyFile.toString(), true);

        Map<String, PublicKey> result = pemPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TWO, result.size());
        Assertions.assertTrue(result.containsKey("default-key"));
        Assertions.assertTrue(result.containsKey("DEFAULT"));
        Assertions.assertSame(result.get("default-key"), result.get("DEFAULT"));
    }

    @Test
    void loadsCertificateSuccessfully() throws IOException {
        Path certFile = tempDir.resolve("certificate.pem");
        Files.writeString(certFile, VALID_CERTIFICATE);

        PublicKeySource config = createPublicKeySource("cert-key", certFile.toString(), false);

        Map<String, PublicKey> result = pemPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.containsKey("cert-key"));
        Assertions.assertNotNull(result.get("cert-key"));
    }

    @Test
    void throwsExceptionWhenFileNotFound() {
        PublicKeySource config = createPublicKeySource("test-key", "/nonexistent/path/key.pem", false);

        InvalidKeyException exception = Assertions.assertThrows(InvalidKeyException.class,
                () -> pemPublicKeyLoader.loadKeys(config));

        Assertions.assertTrue(exception.getMessage().contains("Failed to load PEM public key from file"));
        Assertions.assertTrue(exception.getMessage().contains(config.getLocation()));
    }

    @Test
    void throwsExceptionWhenFileContainsInvalidPublicKey() throws IOException {
        Path keyFile = tempDir.resolve("invalid_key.pem");
        Files.writeString(keyFile, "invalid key content");

        PublicKeySource config = createPublicKeySource("test-key", keyFile.toString(), false);

        InvalidKeyException exception = Assertions.assertThrows(InvalidKeyException.class,
                () -> pemPublicKeyLoader.loadKeys(config));

        Assertions.assertTrue(exception.getMessage().contains("Failed to load PEM public key from file"));
    }

    @Test
    void throwsExceptionWhenFileContainsInvalidCertificate() throws IOException {
        Path certFile = tempDir.resolve("invalid_cert.pem");
        Files.writeString(certFile, """
                -----BEGIN CERTIFICATE-----
                invalid certificate content
                -----END CERTIFICATE-----
                """);

        PublicKeySource config = createPublicKeySource("test-key", certFile.toString(), false);

        InvalidKeyException exception = Assertions.assertThrows(InvalidKeyException.class,
                () -> pemPublicKeyLoader.loadKeys(config));

        Assertions.assertTrue(exception.getMessage().contains("Failed to load PEM public key from file"));
    }

    @Test
    void throwsExceptionWhenFileIsEmpty() throws IOException {
        Path emptyFile = tempDir.resolve("empty.pem");
        Files.writeString(emptyFile, "");

        PublicKeySource config = createPublicKeySource("test-key", emptyFile.toString(), false);

        InvalidKeyException exception = Assertions.assertThrows(InvalidKeyException.class,
                () -> pemPublicKeyLoader.loadKeys(config));

        Assertions.assertTrue(exception.getMessage().contains("Failed to load PEM public key from file"));
    }

    @Test
    void returnsCorrectKeyType() {
        PublicKeyType result = pemPublicKeyLoader.getType();

        Assertions.assertEquals(PublicKeyType.PEM, result);
    }

    @Test
    void handlesPublicKeyWithExtraWhitespace() throws IOException {
        String keyWithWhitespace = "  \n" + VALID_PUBLIC_KEY_WITH_HEADERS + "  \n  ";
        Path keyFile = tempDir.resolve("key_with_whitespace.pem");
        Files.writeString(keyFile, keyWithWhitespace);

        PublicKeySource config = createPublicKeySource("test-key", keyFile.toString(), false);

        Map<String, PublicKey> result = pemPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.containsKey("test-key"));
        Assertions.assertNotNull(result.get("test-key"));
    }

    @Test
    void handlesPublicKeyWithDifferentLineEndings() throws IOException {
        String keyWithWindowsLineEndings = VALID_PUBLIC_KEY_WITH_HEADERS.replace("\n", "\r\n");
        Path keyFile = tempDir.resolve("key_windows_endings.pem");
        Files.writeString(keyFile, keyWithWindowsLineEndings);

        PublicKeySource config = createPublicKeySource("test-key", keyFile.toString(), false);

        Map<String, PublicKey> result = pemPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.containsKey("test-key"));
        Assertions.assertNotNull(result.get("test-key"));
    }

    private PublicKeySource createPublicKeySource(String id, String location, boolean isDefault) {
        PublicKeySource source = new PublicKeySource();
        source.setId(id);
        source.setLocation(location);
        source.setDefault(isDefault);
        source.setType(PublicKeyType.PEM);
        return source;
    }

    @Test
    void loadsRsaPublicKeySuccessfully() throws IOException {
        Path keyFile = tempDir.resolve("rsa_public_key.pem");
        Files.writeString(keyFile, VALID_PUBLIC_KEY_WITH_HEADERS);

        PublicKeySource config = createPublicKeySource("rsa-key", keyFile.toString(), false);
        Map<String, PublicKey> result = pemPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result.get("rsa-key"));
        Assertions.assertEquals("RSA", result.get("rsa-key").getAlgorithm());
    }

    @Test
    void loadsEcPublicKeySuccessfully() throws IOException {
        String ecPublicKey = """
            -----BEGIN PUBLIC KEY-----
            MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEYWnm/eplO9BFtXEUulbDuaiCx7e7
            4C7+YKXXqNQqTwmvdOJvBeFGJaGRxc3HMhWvdyIRPPxlG+qbTaEEQpPGYQ==
            -----END PUBLIC KEY-----
            """;
        Path keyFile = tempDir.resolve("ec_public_key.pem");
        Files.writeString(keyFile, ecPublicKey);

        PublicKeySource config = createPublicKeySource("ec-key", keyFile.toString(), false);
        Map<String, PublicKey> result = pemPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result.get("ec-key"));
        Assertions.assertEquals("EC", result.get("ec-key").getAlgorithm());
    }
}
