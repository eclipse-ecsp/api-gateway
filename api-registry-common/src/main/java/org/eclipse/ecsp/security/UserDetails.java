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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * UserDetails is a utility class that holds user information and associated metadata.
 * It provides methods to manage user details, including user ID, scope, and override scopes.
 */
@Getter
@ToString
public class UserDetails {
    /**
     * userId is the unique identifier for the user.
     */
    protected final String userId;
    /**
     * scope is a set of permissions or roles associated with the user.
     */
    protected final Set<String> scope;
    /**
     * overrideScopes is a set of permissions or roles that can override the default scope.
     */
    protected final Set<String> overrideScopes;
    /**
     * metadata is a map that holds additional information about the user.
     */
    @Setter
    protected Map<String, String> metadata;

    /**
     * Parameterized constructor.
     *
     * @param userId         userId
     * @param scope          scope
     * @param overrideScopes overrideScopes
     */
    public UserDetails(String userId, Set<String> scope, Set<String> overrideScopes) {
        this.userId = userId;
        this.scope = scope;
        this.overrideScopes = overrideScopes;
        this.metadata = new HashMap<>();
    }

    /**
     * getScopeAsString returns scope in String format.
     *
     * @return scope in String
     */
    public String getScopeAsString() {
        StringBuilder s = new StringBuilder();
        scope.forEach(e -> s.append(e).append(","));
        return s.toString();
    }
}
