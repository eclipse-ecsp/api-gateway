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

package org.eclipse.ecsp.register.customization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pure, Spring/OpenAPI-free path-template functions shared by
 * {@code RouteCustomizerOpenApiCustomizer} (target-path validation) and {@code ApiRoutesLoader}
 * (downstream {@code RewritePath} construction).
 *
 * <p>Each {@code {name}} token must occupy an entire {@code /}-delimited path segment (the
 * common Spring path-variable convention); mixed segments such as {@code file-{name}.json} are
 * rejected as malformed rather than partially supported.
 */
public final class PathTemplateUtils {

    private static final String REASON_INVALID_TEMPLATE = "invalid_path_template";
    private static final String REASON_RESERVED_PATH = "reserved_path";
    private static final List<String> RESERVED_PATH_PREFIXES = List.of("/v3/api-docs", "/actuator");
    private static final int MIN_BRACED_SEGMENT_LENGTH = 2;
    private static final int KEEP_TRAILING_EMPTY_SEGMENTS = -1;

    private PathTemplateUtils() {
    }

    /**
     * Validates a configured {@code exposeAs} target path template against the original OpenAPI
     * path it is derived from.
     *
     * <p>Rejects malformed braces, query/fragment text, wildcard segments, duplicate variable
     * names, dot segments, repeated separators, a path not starting with {@code /}, a reserved
     * internal path, or a variable-name set that differs from the original path's.
     *
     * @param originalPath   the pre-fan-out OpenAPI path template, e.g. {@code /v1/api/dothis/{id}}
     * @param targetTemplate the configured target path, possibly with unresolved whitespace
     * @return a valid result carrying the normalized target path, or an invalid result with a
     *     typed rejection reason
     */
    public static TargetPathValidation validate(String originalPath, String targetTemplate) {
        if (targetTemplate == null || targetTemplate.isBlank()) {
            return TargetPathValidation.invalid(REASON_INVALID_TEMPLATE);
        }
        String normalized = targetTemplate.trim();
        if (!isWellFormedAbsolutePath(normalized)) {
            return TargetPathValidation.invalid(REASON_INVALID_TEMPLATE);
        }
        if (isReservedPath(normalized)) {
            return TargetPathValidation.invalid(REASON_RESERVED_PATH);
        }
        return validateVariables(originalPath, normalized);
    }

    private static TargetPathValidation validateVariables(String originalPath, String normalized) {
        Set<String> originalVariables;
        Set<String> targetVariables;
        try {
            originalVariables = extractVariableNames(originalPath);
            targetVariables = extractVariableNames(normalized);
        } catch (IllegalArgumentException ex) {
            return TargetPathValidation.invalid(REASON_INVALID_TEMPLATE);
        }
        if (!originalVariables.equals(targetVariables)) {
            return TargetPathValidation.invalid(REASON_INVALID_TEMPLATE);
        }
        return TargetPathValidation.valid(normalized);
    }

    /**
     * Returns whether the given path falls under a reserved, internally-owned prefix
     * (springdoc's {@code /v3/api-docs} or Spring Boot Actuator's {@code /actuator}) that a
     * mapping must never be allowed to shadow.
     *
     * @param normalizedPath an already syntax-validated, normalized absolute path
     * @return {@code true} if the path is reserved
     */
    public static boolean isReservedPath(String normalizedPath) {
        for (String prefix : RESERVED_PATH_PREFIXES) {
            if (normalizedPath.equals(prefix) || normalizedPath.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the anchored {@code (regex, replacement)} pair that rewrites a customized, exposed
     * path back to the controller's real, original path.
     *
     * @param exposedPath  the path the gateway actually matched, e.g. {@code /api/dothis/{id}}
     * @param originalPath the real controller path, e.g. {@code /v1/api/dothis/{id}}
     * @param contextPath  the configured backend context path prefix, or {@code null}/blank
     * @return the rewrite spec
     */
    public static RewriteSpec buildRewrite(String exposedPath, String originalPath, String contextPath) {
        StringBuilder regex = new StringBuilder("^");
        StringBuilder literal = new StringBuilder();
        java.util.Map<String, String> variableToGroup = new java.util.LinkedHashMap<>();
        int groupIndex = 0;
        for (String segment : splitSegments(exposedPath)) {
            literal.append('/');
            String variableName = extractSingleVariable(segment);
            if (variableName == null) {
                literal.append(segment);
                continue;
            }
            flushLiteral(regex, literal);
            String groupName = "v" + groupIndex++;
            variableToGroup.put(variableName, groupName);
            regex.append("(?<").append(groupName).append(">[^/]+)");
        }
        flushLiteral(regex, literal);
        regex.append('$');
        return new RewriteSpec(regex.toString(), buildReplacement(originalPath, contextPath, variableToGroup));
    }

    private static String buildReplacement(String originalPath, String contextPath,
                                            java.util.Map<String, String> variableToGroup) {
        StringBuilder replacement = new StringBuilder();
        if (contextPath != null && !contextPath.isBlank()) {
            replacement.append(contextPath);
        }
        for (String segment : splitSegments(originalPath)) {
            replacement.append('/');
            String variableName = extractSingleVariable(segment);
            if (variableName == null) {
                replacement.append(segment);
            } else {
                replacement.append("${").append(variableToGroup.get(variableName)).append('}');
            }
        }
        return replacement.toString();
    }

    private static void flushLiteral(StringBuilder regex, StringBuilder literal) {
        if (!literal.isEmpty()) {
            regex.append(Pattern.quote(literal.toString()));
            literal.setLength(0);
        }
    }

    private static boolean isWellFormedAbsolutePath(String path) {
        if (!path.startsWith("/") || path.contains("?") || path.contains("#")
                || path.contains("*") || path.contains("//")) {
            return false;
        }
        for (String segment : splitSegments(path)) {
            if (!isWellFormedSegment(segment)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWellFormedSegment(String segment) {
        if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
            return false;
        }
        boolean hasOpenBrace = segment.contains("{");
        boolean hasCloseBrace = segment.contains("}");
        if (hasOpenBrace != hasCloseBrace) {
            return false;
        }
        if (!hasOpenBrace) {
            return true;
        }
        String variableName = extractSingleVariable(segment);
        return variableName != null && !variableName.isEmpty()
                && !variableName.contains("{") && !variableName.contains("}");
    }

    private static Set<String> extractVariableNames(String path) {
        Set<String> names = new LinkedHashSet<>();
        for (String segment : splitSegments(path)) {
            String name = extractSingleVariable(segment);
            if (name != null && !names.add(name)) {
                throw new IllegalArgumentException("Duplicate path variable: " + name);
            }
        }
        return names;
    }

    private static String extractSingleVariable(String segment) {
        if (segment.length() > MIN_BRACED_SEGMENT_LENGTH
                && segment.charAt(0) == '{' && segment.charAt(segment.length() - 1) == '}') {
            return segment.substring(1, segment.length() - 1);
        }
        return null;
    }

    private static List<String> splitSegments(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(Arrays.asList(trimmed.split("/", KEEP_TRAILING_EMPTY_SEGMENTS)));
    }
}
