package org.eclipse.ecsp.gateway.utils;

import lombok.NoArgsConstructor;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utility class to generate a public key, encode it in Base64, and print it with and without PEM headers.
 * This class is primarily used for testing and debugging purposes.
 */
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
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
        for (int i = 0; i < base64PublicKey.length(); i += INT_64) {
            int end = Math.min(i + INT_64, base64PublicKey.length());
            System.out.println(base64PublicKey.substring(i, end));
        }
        LOGGER.info("-----END PUBLIC KEY-----");

        LOGGER.info("\nWithout headers (Base64 only):");
        for (int i = 0; i < base64PublicKey.length(); i += INT_64) {
            int end = Math.min(i + INT_64, base64PublicKey.length());
            LOGGER.info(base64PublicKey.substring(i, end));
        }

        // Verify the key can be parsed back
        LOGGER.info("\nVerification:");
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey parsedKey = keyFactory.generatePublic(keySpec);
            LOGGER.info("Key validation: SUCCESS - Generated key can be parsed");
            LOGGER.info("Algorithm: " + parsedKey.getAlgorithm());
            LOGGER.info("Format: " + parsedKey.getFormat());
        } catch (Exception e) {
            LOGGER.info("Key validation: FAILED - " + e.getMessage());
        }
    }
}
