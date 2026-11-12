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

package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.config.RouteOrderOverrideProperties;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link RouteOrderResolver} is registered only when the route order override is enabled.
 */
class RouteOrderResolverConditionTest {

    private static final String ENABLED = GatewayConstants.ROUTE_ORDER_OVERRIDE_PREFIX + ".enabled=";
    private static final String ROUTE_ID = GatewayConstants.ROUTE_ORDER_OVERRIDE_PREFIX + ".mappings[0].route-id=";
    private static final String ORDER = GatewayConstants.ROUTE_ORDER_OVERRIDE_PREFIX + ".mappings[0].order=";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void resolverIsAbsentByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(RouteOrderResolver.class));
    }

    @Test
    void resolverIsAbsentWhenDisabled() {
        contextRunner.withPropertyValues(ENABLED + "false")
                .run(context -> assertThat(context).doesNotHaveBean(RouteOrderResolver.class));
    }

    @Test
    void resolverIsPresentWhenEnabled() {
        contextRunner.withPropertyValues(ENABLED + "true", ROUTE_ID + "Users-getUser", ORDER + "-10")
                .run(context -> assertThat(context).hasSingleBean(RouteOrderResolver.class));
    }

    @Test
    void contextFailsToStartWhenEnabledWithInvalidMapping() {
        contextRunner.withPropertyValues(ENABLED + "true", ROUTE_ID + "Users-getUser")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(RouteOrderOverrideProperties.class)
    @Import(RouteOrderResolver.class)
    static class TestConfig {
    }
}
