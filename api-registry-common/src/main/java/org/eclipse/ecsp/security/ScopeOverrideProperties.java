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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for scope-override behaviour.
 *
 * <p>Binds to the {@code scopes} prefix and centralises the two scope-related
 * settings that were previously duplicated across {@code ScopeTagger},
 * {@code ApiRoutesLoader}, {@code ScopeValidator}, and
 * {@code TokenValidationInterceptor}:
 *
 * <ul>
 *   <li>{@code scopes.override.enabled} — whether the override-scope feature is active.
 *   <li>{@code scopes.scopes-map} — per-route override scope lists, keyed by route ID.
 * </ul>
 *
 * <p>Registered as a {@code @Component} so it is available in any application context
 * regardless of which optional features are enabled.
 */
@Component
@ConfigurationProperties(prefix = "scopes")
public class ScopeOverrideProperties {

    /**
     * Default constructor.
     */
    public ScopeOverrideProperties() {
        // Default constructor
    }

    private Override override = new Override();

    /**
     * Per-route override scope lists.
     *
     * <p>Keys are route IDs in the form {@code <tag>-<operationId>}.
     * Values are the replacement scope lists to apply when override is enabled.
     */
    private Map<String, List<String>> scopesMap;

    /**
     * Returns the override sub-properties.
     *
     * @return the override configuration
     */
    public Override getOverride() {
        return override;
    }

    /**
     * Sets the override sub-properties.
     *
     * @param override the override configuration
     */
    public void setOverride(Override override) {
        this.override = override;
    }

    /**
     * Returns the per-route override scope map.
     *
     * @return map of route ID to override scope list, or {@code null} if not configured
     */
    public Map<String, List<String>> getScopesMap() {
        return scopesMap;
    }

    /**
     * Sets the per-route override scope map.
     *
     * @param scopesMap map of route ID to override scope list
     */
    public void setScopesMap(Map<String, List<String>> scopesMap) {
        this.scopesMap = scopesMap;
    }

    /**
     * Nested properties for the {@code scopes.override} sub-key.
     */
    public static class Override {

        /**
         * Default constructor.
         */
        public Override() {
            // Default constructor
        }

        private boolean enabled = false;

        /**
         * Returns whether the override-scope feature is enabled.
         *
         * @return {@code true} if override is active
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether the override-scope feature is enabled.
         *
         * @param enabled {@code true} to activate override
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
