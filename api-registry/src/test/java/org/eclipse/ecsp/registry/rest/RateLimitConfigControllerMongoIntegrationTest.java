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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test suite backed by a MongoDB Testcontainer.
 */
@Testcontainers
class RateLimitConfigControllerMongoIntegrationTest extends AbstractRateLimitConfigControllerIntegrationTest {

    private static final String MONGO_IMAGE = "mongo:6.0.22";
    private static final int MONGO_PORT = 27017;
    private static final String MONGO_USERNAME = "registry";
    private static final String MONGO_PASSWORD = "registry";
    private static final String MONGO_AUTH_DB = "admin";
    @SuppressWarnings("resource")
    @Container
    private static final GenericContainer<?> MONGO_CONTAINER = 
        new GenericContainer<>(DockerImageName.parse(MONGO_IMAGE))
            .withExposedPorts(MONGO_PORT)
            .withEnv("MONGO_INITDB_ROOT_USERNAME", MONGO_USERNAME)
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", MONGO_PASSWORD)
            .withEnv("MONGO_INITDB_DATABASE", MONGO_AUTH_DB);

    @DynamicPropertySource
    static void configureMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("api-registry.database.type", () -> "nosql");
        registry.add("api-registry.database.provider", () -> "mongoDB");
        registry.add("no.sql.database.type", () -> "mongoDB");
        registry.add("mongodb.hosts", MONGO_CONTAINER::getHost);
        registry.add("mongodb.port", () -> MONGO_CONTAINER.getMappedPort(MONGO_PORT));
        registry.add("mongodb.username", () -> MONGO_USERNAME);
        registry.add("mongodb.password", () -> MONGO_PASSWORD);
        registry.add("mongodb.auth.db", () -> MONGO_AUTH_DB);
        registry.add("mongodb.name", () -> "ecsp");
    }
}
