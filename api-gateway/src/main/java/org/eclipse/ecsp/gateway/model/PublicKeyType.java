package org.eclipse.ecsp.gateway.model;

/**
 * Enumeration of supported public key types.
 *
 * @author Abhishek Kumar
 */
public enum PublicKeyType {
    /**
     * PEM format public key.
     */
    PEM,

    /**
     * JSON Web Key Set (JWKS) format.
     */
    JWKS
}
