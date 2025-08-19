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

package org.eclipse.ecsp.gateway.plugins.keysources;

import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Implementation of PublicKeySourceProvider that gets key sources from properties configuration.
 *
 * @author Abhishek Kumar
 */
@Component
public class PropertiesPublicKeySourceProvider implements PublicKeySourceProvider {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PropertiesPublicKeySourceProvider.class);
    private final JwtProperties jwtProperties;

    /**
     * Constructor with JWT properties dependency.
     *
     * @param jwtProperties the JWT configuration properties
     */
    public PropertiesPublicKeySourceProvider(JwtProperties jwtProperties) {
        LOGGER.info("PropertiesPublicKeySourceProvider initialized with JWT properties");






































        this.jwtProperties = jwtProperties;
    }

    /**
     * Returns a list of public key sources from properties configuration.
     *
     * @return list of PublicKeySource from properties
     */
    @Override
    public List<PublicKeySource> keySources() {
        return jwtProperties.getKeySources() != null ? jwtProperties.getKeySources() : List.of();
    }
}
