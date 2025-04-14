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

package org.eclipse.ecsp.security;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ScopeValidator.
 */
@Aspect
@Component
@ConditionalOnProperty(value = "api.security.enabled", havingValue = "true", matchIfMissing = false)
public class ScopeValidator {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ScopeValidator.class);
    @Value("${scopes.override.enabled:false}")
    private boolean isOverrideScopeEnabled;

    /**
     * validate the scopes.
     *
     * @param joinPoint JoinPoint
     */
    @Before(value = "@annotation(io.swagger.v3.oas.annotations.security.SecurityRequirement)")
    public void validate(JoinPoint joinPoint) {
        // Method Information
        LOGGER.info("SecurityRequirement config found");
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        if (method == null) {
            return;
        }
        SecurityRequirement security = method.getAnnotation(SecurityRequirement.class);
        if (security == null) {
            LOGGER.info("SecurityRequirement config is empty");
            return;
        }
        String scopeStr = String.join(",", security.scopes());
        Set<String> methodScopes = Arrays.stream(scopeStr.split(",")).map(String::trim).collect(Collectors.toSet());
        LOGGER.debug("Method Scope Set: {}", methodScopes);
        LOGGER.info("Override Scope Set: {}", HeaderContext.getUserDetails().getOverrideScopes());

        if (isOverrideScopeEnabled) {
            validateScope(HeaderContext.getUserDetails().getOverrideScopes(), methodScopes);
        } else {
            validateScope(HeaderContext.getUserDetails().getScope(), methodScopes);
        }
    }

    private void validateScope(final Set<String> userScopes, final Set<String> methodScopes) {
        if (methodScopes.isEmpty()) {
            return;
        }
        if (userScopes == null) {
            throw new IllegalAccessError("Invalid route/claims");
        }
        LOGGER.debug("User scopes: {} and target method scopes: {}", userScopes, methodScopes);
        // atleast one of the methodScopes should match userScopes
        boolean validScope = userScopes.stream().anyMatch(methodScopes::contains);
        if (!validScope) {
            throw new IllegalAccessError("Insufficient claims");
        }
    }
}