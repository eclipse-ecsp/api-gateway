package org.eclipse.ecsp.gateway.plugins.keysources;

import org.eclipse.ecsp.gateway.model.PublicKeySource;
import java.util.List;

/**
 * Provides sources for public keys used in JWT validation.
 *
 * @author Abhishek Kumar
 */
public interface PublicKeySourceProvider {
    /**
     * Returns a list of public key sources.
     * This method is used to retrieve the configurations for public key sources
     * that can be used by the PublicKeyLoader to load public keys.
     *
     * @return list of PublicKeySource
     */
    List<PublicKeySource> keySources();
}

