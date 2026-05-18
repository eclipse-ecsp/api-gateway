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

package org.eclipse.ecsp.config;

import org.eclipse.ecsp.interceptors.HeaderInterceptor;
import org.eclipse.ecsp.interceptors.TokenValidationInterceptor;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Optional;

/**
 * InterceptorConfig.
 */
@Configuration
@Order(10)
public class InterceptorConfig implements WebMvcConfigurer {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(InterceptorConfig.class);
    private final HeaderInterceptor headerInterceptor;
    private final Optional<TokenValidationInterceptor> tokenValidationInterceptor;

    /**
     * Constructor to initialize the InterceptorConfig.
     *
     * @param headerInterceptor          the HeaderInterceptor
     * @param tokenValidationInterceptor the optional TokenValidationInterceptor
     */
    public InterceptorConfig(HeaderInterceptor headerInterceptor,
                             Optional<TokenValidationInterceptor> tokenValidationInterceptor) {
        this.headerInterceptor = headerInterceptor;
        this.tokenValidationInterceptor = tokenValidationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(headerInterceptor);
        LOGGER.debug("Added HeaderInterceptor to the interceptor registry");
        tokenValidationInterceptor.ifPresent(interceptor -> {
            registry.addInterceptor(interceptor);
            LOGGER.debug("Added TokenValidationInterceptor to the interceptor registry");
        });
    }
}