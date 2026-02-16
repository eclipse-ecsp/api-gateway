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

package org.eclipse.ecsp.registry.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test configuration to enable JPA and DataSource for integration tests.
 *
 * <p>
 * The main RegistryApplication excludes JPA/DataSource auto-configuration,
 * but ClientAccessControl integration tests need them enabled.
 */
@TestConfiguration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "org.eclipse.ecsp.registry.repository")
public class TestJpaConfiguration {
    // Configuration is done via annotations
}
