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

package org.eclipse.ecsp.registry.repo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Unit tests for ClientAccessControlRepository.
 *
 * <p>Tests custom query methods:
 * - findByClientIdAndIsDeletedFalse
 * - findByIsActiveAndIsDeletedFalse
 * - findAllNotDeleted
 * - findByTenantAndIsDeletedFalse
 * - existsByClientIdAndIsDeletedFalse
 *
 */

@SpringBootTest
@Testcontainers
class ClientAccessControlRepositorySqlTest extends AbstractClientAccessControlRepositoryTest {

    private static final String POSTGRES_IMAGE = "postgres:15.3";
    private static final int POSTGRES_PORT = 5432;
    private static final String POSTGRES_USERNAME = "registry";
    private static final String POSTGRES_PASSWORD = "registry";
    private static final String POSTGRES_DB = "ecsp";

    @SuppressWarnings("resource")
    @Container
    private static final GenericContainer<?> POSTGRES_CONTAINER = 
        new GenericContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
            .withExposedPorts(POSTGRES_PORT)
            .withEnv("POSTGRES_USER", POSTGRES_USERNAME)
            .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
            .withEnv("POSTGRES_DB", POSTGRES_DB);

    @DynamicPropertySource
    static void configurePostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("api-registry.database.type", () -> "sql");
        registry.add("api-registry.database.provider", () -> "postgresql");
        registry.add("postgres.jdbc.url", () -> 
            "jdbc:postgresql://" + POSTGRES_CONTAINER.getHost() 
            + ":" + POSTGRES_CONTAINER.getMappedPort(POSTGRES_PORT) + "/" + POSTGRES_DB);
        registry.add("postgres.port", () -> POSTGRES_CONTAINER.getMappedPort(POSTGRES_PORT));
        registry.add("postgres.username", () -> POSTGRES_USERNAME);
        registry.add("postgres.password", () -> POSTGRES_PASSWORD);
    }
}
