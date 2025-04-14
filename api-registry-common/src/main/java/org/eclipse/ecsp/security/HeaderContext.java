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

import java.util.Set;

/**
 * HeaderContext is a utility class that manages the user context for the current thread.
 * It allows setting, getting, and clearing user details associated with the current thread.
 */
public class HeaderContext {
    private static final ThreadLocal<UserDetails> USER_CONTEXT = new ThreadLocal<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private HeaderContext() {
    }

    /**
     * Sets the user details in the current thread's context.
     *
     * @param userId         the user ID
     * @param scopes         the set of scopes associated with the user
     * @param overrideScopes the set of override scopes associated with the user
     */
    public static void setUser(String userId, Set<String> scopes, Set<String> overrideScopes) {
        UserDetails user = new UserDetails(userId, scopes, overrideScopes);
        USER_CONTEXT.set(user);
    }

    /**
     * Sets the user details in the current thread's context.
     *
     * @return userDetails the user details to set
     */
    public static UserDetails getUserDetails() {
        return USER_CONTEXT.get();
    }

    /**
     * clear the users from the current thread's context.
     */
    public static void clear() {
        USER_CONTEXT.remove();
    }
}
