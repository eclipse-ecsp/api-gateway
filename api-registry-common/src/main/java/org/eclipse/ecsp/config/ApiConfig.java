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

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.eclipse.ecsp.customizers.CustomGatewayFilterCustomizer;
import org.eclipse.ecsp.security.CachingTagger;
import org.eclipse.ecsp.security.ScopeTagger;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ApiConfig.
 */
@Configuration
public class ApiConfig {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiConfig.class);

    @Value("${openapi.path.include}")
    private String[] pathsInclude;
    @Value("${openapi.path.exclude:}")
    private String[] pathsExclude;
    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${spring.application.version}")
    private String applicationVersion;
    @Value("${api.gatewayUrls:}")
    private String serverUrls;

    /**
     * include api matching with path defined in api doc.
     *
     * @return api paths to be included to api doc.
     */
    public String[] pathsInclude() {
        LOGGER.info("pathsInclude: {}", Arrays.toString(this.pathsInclude));
        return this.pathsInclude;
    }

    /**
     * exclude api matching with path defined from api doc.
     *
     * @return api paths to be excluded from api doc.
     */
    public String[] pathsExclude() {
        LOGGER.info("pathsExclude: {}", Arrays.toString(this.pathsExclude));
        return this.pathsExclude;
    }

    /**
     * applicationName.
     *
     * @return application name.
     */
    public String applicationName() {
        LOGGER.info("applicationName: {}", applicationName);
        return this.applicationName;
    }

    /**
     * applicationVersion.
     *
     * @return application version.
     */
    public String applicationVersion() {
        LOGGER.info("applicationVersion: {}", applicationVersion);
        return this.applicationVersion;
    }

    /**
     * Creating OpenApi bean.
     *
     * @return OpenAPI bean.
     */
    @Bean
    public OpenAPI openApi() {
        List<Server> servers = new ArrayList<>();
        for (String serverUrl : serverUrls.split(",")) {
            Server server = new Server();
            String normalizedUrl = normalizeServerUrl(serverUrl.trim());
            server.setUrl(normalizedUrl);
            servers.add(server);
        }
        LOGGER.info("API serverUrls : {}", Collections.singletonList(serverUrls));
        LOGGER.info("API Servers : {}", servers);
        String desc = "This provides " + applicationName() + " services";
        return new OpenAPI().components(new Components())
                .info(new Info()
                        .title(applicationName().toUpperCase())
                        .version(applicationVersion())
                        .description(desc))
                .servers(servers);
    }

    /**
     * Normalizes server URLs to prevent duplication and ensure proper scheme.
     *
     * @param raw the raw server URL
     * @return the normalized server URL
     */
    private String normalizeServerUrl(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return raw;
        }
        
        String trimmed = raw.trim();
        
        // Check for duplicate domain patterns and clean them up
        if (trimmed.contains("api-gateway.")
                && trimmed.indexOf("api-gateway.") != trimmed.lastIndexOf("api-gateway.")) {
            // Extract the first occurrence of the domain pattern
            int firstIndex = trimmed.indexOf("api-gateway.");
            int secondIndex = trimmed.indexOf("api-gateway.", firstIndex + 1);
            if (secondIndex > firstIndex) {
                trimmed = trimmed.substring(0, secondIndex);
                LOGGER.warn("Detected duplicate domain pattern in URL '{}', cleaned to '{}'", raw, trimmed);
            }
        }
        
        // Add https scheme if no scheme is present
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            if (trimmed.startsWith("//")) {
                // Protocol-relative URL
                trimmed = "https:" + trimmed;
            } else {
                // No scheme at all
                trimmed = "https://" + trimmed;
            }
            LOGGER.debug("Added https scheme to URL '{}' -> '{}'", raw, trimmed);
        }
        
        return trimmed;
    }

    /**
     * method create and returns GroupedOpenApi bean.
     *
     * @param scopeTagger   used to match the scope
     * @param cachingTagger caching tagger
     * @param customGatewayFilterCustomizer add custom gateway filter
     *
     * @return GroupedOpenApi object
     */
    @Bean
    public GroupedOpenApi groupedOpenApi(final ScopeTagger scopeTagger,
                                         final CachingTagger cachingTagger,
                                         final CustomGatewayFilterCustomizer customGatewayFilterCustomizer) {
        LOGGER.info("Calling GroupedOpenApi of " + applicationName() + " \nEndpoints Included "
                + Arrays.toString(this.pathsInclude()) + " \nEndpoints Excluded "
                + Arrays.toString(this.pathsExclude()));
        return GroupedOpenApi.builder()
                .group(applicationName())
                .pathsToMatch(pathsInclude())
                .pathsToExclude(pathsExclude())
                .addOperationCustomizer(scopeTagger)
                .addOperationCustomizer(cachingTagger)
                .addOperationCustomizer(customGatewayFilterCustomizer)
                .build();
    }
}
