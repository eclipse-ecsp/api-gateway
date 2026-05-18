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

package org.eclipse.ecsp.security;


import org.eclipse.ecsp.tokenvalidator.PublicKeySourceProvider;
import org.eclipse.ecsp.tokenvalidator.model.PublicKeySource;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Default implementation of {@link PublicKeySourceProvider} that retrieves key sources from
 * the application's configuration properties.
 *
 * <p>This class is registered as a Spring component and will be used by the token validation
 * framework to obtain public key sources for JWT validation. The key sources are defined in
 * the application's configuration under the prefix {@code api-registry.security.key-sources}.
 *
 * @author Abhishek Kumar
 */
@Component
@ConditionalOnProperty(
    prefix = RegistryCommonConstants.API_REGISTRY_SECURITY_PREFIX,
    name = "enabled", havingValue = "true")
public class ApiRegistryPublicKeySourceProvider implements PublicKeySourceProvider {

    private final ValidationConfigProperties config;

    /**
     * Constructor that accepts the validation configuration properties.
     *
     * @param config the validation configuration properties
     */
    public ApiRegistryPublicKeySourceProvider(ValidationConfigProperties config) {
        this.config = config;
    }

    @Override
    public List<PublicKeySource> keySources() {
        return config.getSecurity().getKeySources();
    }
}
