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

package org.eclipse.ecsp.gateway.model;

import lombok.Getter;
import java.security.PublicKey;
import java.util.Map;

/**
 * Data model representing public key information with associated metadata.
 * This class encapsulates a public key and its related information used for JWT token validation.
 * Instances are typically cached and should be treated as immutable after creation.
 *
 * @author Abhishek Kumar
 */
@Getter
public class PublicKeyInfo {
    /** The key identifier (kid) from JWT header used to locate the correct public key. */
    private String kid;
    
    /** The actual public key used for JWT signature verification. */
    private PublicKey publicKey;
    
    /** The type of public key source (JWKS, PEM, etc.). */
    private PublicKeyType type;
    
    /** The issuer that provided this public key. */
    private String issuer;
    
    /** Unique identifier of the source that loaded this key. */
    private String sourceId;
    
    /** Additional metadata associated with this public key. */
    private Map<String, Object> additionalMetaData;

    /**
     * Sets the key identifier with null validation.
     *
     * @param kid the key identifier, cannot be null
     * @throws IllegalArgumentException if kid is null
     */
    public void setKid(String kid) {
        if (kid == null) {
            throw new IllegalArgumentException("Kid cannot be null");
        }
        this.kid = kid;
    }

    /**
     * Sets the public key with null validation.
     *
     * @param publicKey the public key, cannot be null
     * @throws IllegalArgumentException if publicKey is null
     */
    public void setPublicKey(PublicKey publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("PublicKey cannot be null");
        }
        this.publicKey = publicKey;
    }

    /**
     * Sets the public key type with null validation.
     *
     * @param type the public key type, cannot be null
     * @throws IllegalArgumentException if type is null
     */
    public void setType(PublicKeyType type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        this.type = type;
    }

    /**
     * Sets the issuer (nullable).
     *
     * @param issuer the issuer
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Sets the source identifier with null validation.
     *
     * @param sourceId the source identifier, cannot be null
     * @throws IllegalArgumentException if sourceId is null
     */
    public void setSourceId(String sourceId) {
        if (sourceId == null) {
            throw new IllegalArgumentException("SourceId cannot be null");
        }
        this.sourceId = sourceId;
    }

    /**
     * Sets the additional metadata (nullable).
     *
     * @param additionalMetaData the additional metadata
     */
    public void setAdditionalMetaData(Map<String, Object> additionalMetaData) {
        this.additionalMetaData = additionalMetaData;
    }
}