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

import org.eclipse.ecsp.restclient.RestTemplateErrorHandler;
import org.eclipse.ecsp.restclient.RestTemplateLogInterceptor;
import org.eclipse.ecsp.restclient.RestTemplateTokenInterceptor;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for RestTemplate.
 */
@Configuration
public class RestTemplateConfig {
    /**
     * Default constructor.
     */
    public RestTemplateConfig() {
        // Default constructor
    }


    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RestTemplateConfig.class);


    /**
     * Create and returns the object of RestTemplate.
     *
     * <p>If a {@link RestTemplateTokenInterceptor} bean is available it is added to the
     * interceptor chain so that Bearer tokens are forwarded to downstream services.
     *
     * @param tokenInterceptorProvider provider for the optional token propagation interceptor
     * @return RestTemplate Object
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate(ObjectProvider<RestTemplateTokenInterceptor> tokenInterceptorProvider) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new RestTemplateErrorHandler());
        if (LOGGER.isDebugEnabled()) {
            restTemplate.getInterceptors().add(new RestTemplateLogInterceptor());
        }
        RestTemplateTokenInterceptor tokenInterceptor = tokenInterceptorProvider.getIfAvailable();
        if (tokenInterceptor != null) {
            restTemplate.getInterceptors().add(tokenInterceptor);
        }
        return restTemplate;
    }

}
