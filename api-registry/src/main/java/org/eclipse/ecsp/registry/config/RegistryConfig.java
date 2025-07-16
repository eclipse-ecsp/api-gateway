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

package org.eclipse.ecsp.registry.config;

import org.eclipse.ecsp.nosqldao.spring.config.IgniteDAOMongoConfigWithProps;
import org.eclipse.ecsp.registry.condition.ConditionalOnNoSqlDatabase;
import org.eclipse.ecsp.registry.condition.ConditionalOnSqlDatabase;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import static com.fasterxml.jackson.core.JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION;

/**
 * RegistryConfig.
 */
@AutoConfiguration
public class RegistryConfig {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RegistryConfig.class);

    /**
     * objectMapperBuilderCustomizer to customize ObjectMapper.
     *
     * @return Jackson2ObjectMapperBuilderCustomizer.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer objectMapperBuilderCustomizer() {
        return builder -> builder.featuresToEnable(INCLUDE_SOURCE_IN_LOCATION);
    }

    /**
     * configuration for nosql database.
     */
    @ConditionalOnNoSqlDatabase
    @Configuration
    @Import({IgniteDAOMongoConfigWithProps.class})
    public static class NoSqlDatabaseConfig {
        /**
         * Constructor for NoSqlDatabaseConfig.
         */
        public NoSqlDatabaseConfig() {
            LOGGER.info("NoSqlDatabaseConfig loaded");
        }
    }

    /**
     * configuration for sql database.
     */
    @ConditionalOnSqlDatabase
    @Configuration
    @ComponentScan(basePackages = "org.eclipse.ecsp.sql.*")
    @Import({HibernateJpaAutoConfiguration.class})
    public static class SqlDatabaseConfig {
        /**
         * Constructor for SqlDatabaseConfig.
         */
        public SqlDatabaseConfig() {
            LOGGER.info("SqlDatabaseConfig loaded");
        }
    }

    /**
     * EndpointFilter to restrict exposing endpoints when metrics are not enabled.
     *
     * @return instance of {@link EndpointFilter}
     */
    @Bean
    @ConditionalOnProperty(name = "api-registry.metrics.enabled", havingValue = "false")
    EndpointFilter<ExposableWebEndpoint> registryDisableEndpointFilter() {
        LOGGER.info("Metrics are not enabled, disabling all endpoints.");
        // This filter will disable all endpoints when metrics are not enabled.
        return (endpoint -> false);
    }

}
