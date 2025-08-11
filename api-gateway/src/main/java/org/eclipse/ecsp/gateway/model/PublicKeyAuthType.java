package org.eclipse.ecsp.gateway.model;

/**
 * Enumeration of supported authentication types for public key sources.
 *
 * @author Abhishek Kumar
 */
public enum PublicKeyAuthType {
    /**
     * No authentication required.
     */
    NONE,

    /**
     * Basic authentication using username and password.
     */
    BASIC,

    /**
     * OAuth2 client credentials flow.
     */
    CLIENT_CREDENTIALS
}
