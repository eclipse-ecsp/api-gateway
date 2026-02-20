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

import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Service for matching access rules against service and route requests.
 *
 * <p>Rule evaluation logic:
 * 1. Check if ANY deny rule matches → DENY
 * 2. Else check if ANY allow rule matches → ALLOW
 * 3. Else → DENY (deny-by-default)
 *
 * <p>Rule format: [!]service:route
 * - ! prefix = deny rule
 * - * wildcard supported for service and/or route
 * - Order-independent (deny always overrides allow)
 *
 * <p>Examples:
 * - "user-service:*" → Allow all routes in user-service
 * - "!user-service:ban-user" → Deny specific route
 * - "*:*" → Allow all services and routes
 * - "user-service:get-*" → Allow routes matching pattern
 */
public class AccessRuleMatcherService {
    /**
     * Default constructor.
     */
    public AccessRuleMatcherService() {
        // Default constructor
    }

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(AccessRuleMatcherService.class);
    private static final int RULE_PARTS_COUNT = 2;

    /**
     * Evaluate access rules for a service and route request.
     *
     * @param rules List of access rules
     * @param service Service name from route metadata path or filter argument configuration
     * @param route RouteId from route metadata or filter argument configuration
     * @return true if access allowed, false if denied
     */
    public boolean isAllowed(List<AccessRule> rules, String service, String route) {
        // Empty/null rules → deny by default
        if (rules == null || rules.isEmpty()) {
            LOGGER.debug("No rules configured → deny by default");
            return false;
        }

        // Step 1: Check deny rules first (highest priority)
        for (AccessRule rule : rules) {
            if (rule.isDeny() && matchesRule(rule, service, route)) {
                LOGGER.info("Access DENIED by deny rule: {} for service={}, route={}", 
                        rule.getOriginalRule(), service, route);
                return false;
            }
        }

        // Step 2: Check allow rules
        for (AccessRule rule : rules) {
            if (!rule.isDeny() && matchesRule(rule, service, route)) {
                LOGGER.info("Access ALLOWED by allow rule: {} for service={}, route={}", 
                        rule.getOriginalRule(), service, route);
                return true;
            }
        }

        // Step 3: No allow rule matched → deny by default
        LOGGER.info("Access DENIED (no matching allow rule) for service={}, route={}", service, route);
        return false;
    }

    /**
     * Check if a rule matches the given service and route.
     *
     * @param rule Access rule to check
     * @param service Service name from request
     * @param route Route path from request
     * @return true if rule matches
     */
    private boolean matchesRule(AccessRule rule, String service, String route) {
        boolean serviceMatches = matchesPattern(rule.getService(), service);
        boolean routeMatches = matchesPattern(rule.getRoute(), route);

        return serviceMatches && routeMatches;
    }

    /**
     * Match a pattern (with wildcard support) against a value.
     *
     * <p>Supports simple wildcard matching:
     * - Exact match if pattern equals value
     * - * matches everything
     *
     * @param pattern Pattern (may contain * wildcard)
     * @param value Value to match against
     * @return true if matches
     */
    private boolean matchesPattern(String pattern, String value) {
        if (pattern == null || value == null) {
            return false;
        }

        // Exact match
        return pattern.equals(value) || pattern.equals("*");
    }

    /**
     * Parse a rule string into an AccessRule object.
     *
     * <p>Format: [!]service:route
     *
     * @param ruleString Rule string (e.g., "user-service:*", "!payment-service:refund")
     * @return Parsed AccessRule, or null if invalid format
     */
    public AccessRule parseRule(String ruleString) {
        if (ruleString == null || ruleString.isBlank()) {
            return null;
        }

        String trimmedRule = ruleString.trim();

        // Check for deny prefix
        boolean isDeny = trimmedRule.startsWith("!");
        String ruleWithoutPrefix = isDeny ? trimmedRule.substring(1) : trimmedRule;

        // Split by colon
        String[] parts = ruleWithoutPrefix.split(":", RULE_PARTS_COUNT);
        if (parts.length != RULE_PARTS_COUNT) {
            LOGGER.warn("Invalid rule format (expected 'service:route'): {}", ruleString);
            return null;
        }

        return AccessRule.builder()
                .service(parts[0].trim())
                .route(parts[1].trim())
                .deny(isDeny)
                .originalRule(trimmedRule)
                .build();
    }

    /**
     * Parse a list of rule strings into AccessRule objects.
     *
     * @param ruleStrings List of rule strings
     * @return List of parsed AccessRule objects (invalid rules are skipped)
     */
    public List<AccessRule> parseRules(List<String> ruleStrings) {
        if (ruleStrings == null) {
            return List.of();
        }
        return ruleStrings.stream()
                .map(this::parseRule)
                .filter(Objects::nonNull)
                .toList();
    }
}
