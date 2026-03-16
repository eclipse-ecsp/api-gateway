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

