package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.model.PublicKeyInfo;
import java.util.Optional;

/**
 * Service interface for managing public keys used in JWT validation.
 * Provides methods to find and refresh public keys.
 *
 * @author Abhishek Kumar
 */
public interface PublicKeyService {
    /**
     * Finds a public key by its ID and issuer.
     *
     * @param keyId the identifier of the public key
     * @param provider the provider of the JWT
     * @return an Optional containing the PublicKey if found, otherwise empty
     */
    Optional<PublicKeyInfo> findPublicKey(String keyId, String provider);

    /**
     * Refreshes the public keys from all configured sources.
     * This method should be called to reload keys when they are updated or changed.
     */
    void refreshPublicKeys();
}

