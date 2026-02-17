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

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test suite backed by a PostgreSQL Testcontainer.
 */
@Testcontainers
class RateLimitConfigControllerPostgresIntegrationTest extends AbstractRateLimitConfigControllerIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @SuppressWarnings("resource")
    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("ecsp")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configurePostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("api-registry.database.type", () -> "sql");
        registry.add("api-registry.database.provider", () -> "postgres");
        // Set both top-level and tenant-specific properties for compatibility
        registry.add("postgres.jdbc.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("postgres.username", POSTGRES_CONTAINER::getUsername);
        registry.add("postgres.password", POSTGRES_CONTAINER::getPassword);
        // Set tenant-specific properties for the "default" tenant used when multitenancy is disabled
        registry.add("tenants.profile.default.jdbc.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("tenants.profile.default.username", POSTGRES_CONTAINER::getUsername);
        registry.add("tenants.profile.default.password", POSTGRES_CONTAINER::getPassword);
        registry.add("tenants.profile.default.driver.class.name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        // Disable multitenancy for tests
        registry.add("multitenancy.enabled", () -> "false");
    }
}
