package org.eclipse.ecsp.gateway.plugins.keyloaders;

import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.PublicKeyType;
import java.security.PublicKey;
import java.util.Map;

/**
 * Loads a public key from a given source configuration.
 *
 * @author Abhishek Kumar
 */
public interface PublicKeyLoader {
    /**
     * Loads a public key from the provided configuration.
     *
     * @param config the public key source configuration
     * @return the loaded PublicKey
     */
    Map<String, PublicKey> loadKeys(PublicKeySource config);

    /**
     * Returns the type of public key this loader supports.
     *
     * @return the PublicKeyType
     */
    PublicKeyType getType();
}

