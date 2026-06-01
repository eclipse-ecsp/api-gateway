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

package org.eclipse.ecsp.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link ValidationConfigProperties}.
 */
@ExtendWith(MockitoExtension.class)
class ValidationConfigPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(ValidationConfigProperties.class)
    static class TestConfig {
    }

    @Test
    void shouldBindDefaultValues() {
        ValidationConfigProperties props = new ValidationConfigProperties();
        Assertions.assertFalse(props.getSecurity().isEnabled());
        Assertions.assertTrue(props.getTokenPropagation().getRestTemplate().isEnabled());
        Assertions.assertTrue(props.getTokenPropagation().getWebClient().isEnabled());
        Assertions.assertTrue(props.getTokenPropagation().getRestClient().isEnabled());
        Assertions.assertFalse(props.getTokenPropagation().isAllowExternalHosts());
        Assertions.assertTrue(props.getTokenPropagation().getIncludeHosts().isEmpty());
        Assertions.assertTrue(props.getTokenPropagation().getExcludeHosts().isEmpty());
    }

    @Test
    void shouldBindCustomValues() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                "api.registry.security.enabled=true",
                "api.registry.token-propagation.rest-template.enabled=false",
                "api.registry.token-propagation.web-client.enabled=false",
                "api.registry.token-propagation.rest-client.enabled=false",
                "api.registry.token-propagation.allow-external-hosts=true",
                "api.registry.token-propagation.include-hosts=internal.svc",
                "api.registry.token-propagation.exclude-hosts=external.com"
            )
            .withUserConfiguration(TestConfig.class);

        contextRunner.run(ctx -> {
            ValidationConfigProperties props = ctx.getBean(ValidationConfigProperties.class);
            Assertions.assertTrue(props.getSecurity().isEnabled());
            Assertions.assertFalse(props.getTokenPropagation().getRestTemplate().isEnabled());
            Assertions.assertFalse(props.getTokenPropagation().getWebClient().isEnabled());
            Assertions.assertFalse(props.getTokenPropagation().getRestClient().isEnabled());
            Assertions.assertTrue(props.getTokenPropagation().isAllowExternalHosts());
            Assertions.assertFalse(props.getTokenPropagation().getIncludeHosts().isEmpty());
            Assertions.assertFalse(props.getTokenPropagation().getExcludeHosts().isEmpty());
        });
    }
}
