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

package org.eclipse.ecsp.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.ecsp.security.HeaderContext;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * HeaderInterceptor to intercept the header in each request.
 */
@Component
public class HeaderInterceptor implements HandlerInterceptor {
    /**
     * Default constructor.
     */
    public HeaderInterceptor() {
        // Default constructor
    }


    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(HeaderInterceptor.class);

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response, @NotNull Object handler) {
        if (!RegistryCommonConstants.HEALTH_URL.equals(request.getContextPath())
                && !request.getContextPath().startsWith(RegistryCommonConstants.OPEN_API_URL)) {
            String scope = request.getHeader(RegistryCommonConstants.SCOPE);
            String overrideScope = request.getHeader(RegistryCommonConstants.OVERRIDE_SCOPE);
            LOGGER.debug("Override scope-config received from request: {}", overrideScope);
            Set<String> userScopes = new HashSet<>();
            Set<String> overrideScopes = new HashSet<>();

            if (overrideScope != null) {
                overrideScopes.addAll(Arrays.asList(overrideScope.split(",")));
            }
            if (scope != null) {
                userScopes.addAll(Arrays.asList(scope.split(",")));
            }
            String userId = request.getHeader(RegistryCommonConstants.USER_ID);
            HeaderContext.setUser(userId, userScopes, overrideScopes);
            LOGGER.debug("Request from user: {} ", HeaderContext.getUserDetails());
        }
        return true;
    }
}