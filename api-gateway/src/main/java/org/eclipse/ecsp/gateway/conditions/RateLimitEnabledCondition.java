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

package org.eclipse.ecsp.gateway.conditions;

import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition to check if Rate Limiting is enabled for the Gateway.
 *
 * @author Abhishek Kumar
 */
public class RateLimitEnabledCondition implements Condition {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RateLimitEnabledCondition.class);

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String rateLimitEnabled = context.getEnvironment().getProperty(GatewayConstants.RATE_LIMITING_ENABLED, "true");
        LOGGER.info("Gateway request rate limit condition:  enabled: {}", rateLimitEnabled);
        return Boolean.parseBoolean(rateLimitEnabled);
    }

}
