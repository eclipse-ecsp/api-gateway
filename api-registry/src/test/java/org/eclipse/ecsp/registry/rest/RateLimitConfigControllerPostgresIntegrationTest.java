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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.registry.rest;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test suite backed by a PostgreSQL Testcontainer.
 */
@Testcontainers
@ContextConfiguration(initializers = RateLimitConfigControllerPostgresIntegrationTest.Initializer.class)
class RateLimitConfigControllerPostgresIntegrationTest extends AbstractRateLimitConfigControllerIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @SuppressWarnings("resource")
    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("ecsp")
            .withUsername("postgres")
            .withPassword("postgres");

    /**
     * Application context initializer to configure Postgres properties before context refresh.
     * This ensures properties are available during @ConfigurationProperties binding.
     */
    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            // Ensure testcontainer is started before setting properties
            POSTGRES_CONTAINER.start();
            
            TestPropertyValues.of(
                "api-registry.database.type=sql",
                "api-registry.database.provider=postgres",
                "postgres.jdbc.url=" + POSTGRES_CONTAINER.getJdbcUrl(),
                "postgres.username=" + POSTGRES_CONTAINER.getUsername(),
                "postgres.password=" + POSTGRES_CONTAINER.getPassword(),
                // Tenant-specific properties using kebab-case as expected by sql-dao
                "tenants.profile.default.jdbc-url=" + POSTGRES_CONTAINER.getJdbcUrl(),
                "tenants.profile.default.user-name=" + POSTGRES_CONTAINER.getUsername(),
                "tenants.profile.default.password=" + POSTGRES_CONTAINER.getPassword(),
                "tenants.profile.default.driver-class-name=org.postgresql.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false",
                "multitenancy.enabled=false"
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}
