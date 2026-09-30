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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.eclipse.ecsp.utils.ObjectMapperUtil;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link GlobalOpenApiCustomizer} that fans out and/or renames OpenAPI {@link Paths} entries
 * according to {@link RouteCustomizerProperties} &mdash; before anything else (springdoc's own
 * {@code /v3/api-docs} serving, or {@code ApiRoutesLoader}) consumes the document.
 *
 * <p>This is an opt-in overlay: with zero configured mappings, {@link #customise(OpenAPI)} is a
 * no-op and route derivation is unchanged.
 */
@Component
@ConditionalOnProperty(value = "api.registry.route-customizer.enabled", havingValue = "true", matchIfMissing = false)
public class RouteCustomizerOpenApiCustomizer implements GlobalOpenApiCustomizer, EmbeddedValueResolverAware {

    /** Vendor extension marking a cloned operation and pointing back to its source route id. */
    public static final String ALIAS_OF_EXTENSION = "x-route-alias-of";

    /**
     * Vendor extension carrying the pre-fan-out path template &mdash; read by
     * {@code ApiRoutesLoader.setRewritePathFilter} to build the targeted downstream rewrite.
     */
    public static final String ORIGINAL_PATH_EXTENSION = "x-route-original-path";

    /**
     * Vendor extension carrying the index (in {@code mappings}) of the mapping that produced an
     * alias, so that mapping's filters apply to its own aliases only.
     */
    public static final String MAPPING_INDEX_EXTENSION = "x-route-mapping-index";

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteCustomizerOpenApiCustomizer.class);
    private static final List<HttpMethod> SCANNED_METHODS = List.of(HttpMethod.GET, HttpMethod.POST,
            HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.OPTIONS);

    private final RouteCustomizerProperties properties;
    private StringValueResolver embeddedValueResolver;

    /**
     * Constructor for RouteCustomizerOpenApiCustomizer.
     *
     * @param properties the route-customizer configuration
     */
    public RouteCustomizerOpenApiCustomizer(RouteCustomizerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    @Override
    public void customise(OpenAPI openApi) {
        Paths paths = openApi.getPaths();
        if (paths == null || properties.getMappings().isEmpty()) {
            return;
        }
        List<RouteCustomizerProperties.RouteMapping> mappings = properties.getMappings();
        for (int index = 0; index < mappings.size(); index++) {
            applyMapping(paths, mappings.get(index), index);
        }
    }

    private void applyMapping(Paths paths, RouteCustomizerProperties.RouteMapping mapping, int mappingIndex) {
        MatchedOperation match = findOperation(paths, mapping, mappingIndex);
        if (match == null) {
            LOGGER.warn("ROUTE_CUSTOMIZER_NO_MATCH | routeId={} | path={}",
                    mapping.getRouteId(), mapping.getPath());
            return;
        }
        boolean anyApplied = false;
        for (String targetTemplate : mapping.getExposeAs()) {
            String targetPath = resolvePlaceholders(targetTemplate);
            if (targetPath == null) {
                LOGGER.error("ROUTE_CUSTOMIZER_SKIPPED | reason=unresolved_placeholder | operationId={} "
                        + "| targetPath={}", match.operationId(), targetTemplate);
                continue;
            }
            if (applyTarget(paths, mapping, match, targetPath)) {
                anyApplied = true;
            }
        }
        finalizeReplaceOriginal(paths, mapping, match, anyApplied);
    }

    private boolean applyTarget(Paths paths, RouteCustomizerProperties.RouteMapping mapping,
                                 MatchedOperation match, String targetTemplate) {
        TargetPathValidation validation = PathTemplateUtils.validate(match.path(), targetTemplate);
        if (!validation.valid()) {
            LOGGER.error("ROUTE_CUSTOMIZER_SKIPPED | reason={} | targetPath={}",
                    validation.reason(), targetTemplate);
            return false;
        }
        String targetPath = validation.normalizedTargetPath();
        if (hasOperation(paths, targetPath, match.httpMethod())) {
            LOGGER.error("ROUTE_CUSTOMIZER_SKIPPED | reason=method_path_collision | targetPath={}", targetPath);
            return false;
        }
        if (!hasCompatiblePathItemMetadata(paths, match, targetPath)) {
            LOGGER.error("ROUTE_CUSTOMIZER_SKIPPED | reason=path_metadata_collision | targetPath={}", targetPath);
            return false;
        }
        return cloneAndLog(match, targetPath, paths, mapping.isReplaceOriginal());
    }

    private boolean cloneAndLog(MatchedOperation match, String targetPath, Paths paths, boolean replaceOriginal) {
        try {
            cloneOperationToNewPath(paths, match, targetPath);
        } catch (IllegalArgumentException ex) {
            LOGGER.error("ROUTE_CUSTOMIZER_SKIPPED | reason=clone_failure | targetPath={}", targetPath, ex);
            return false;
        }
        LOGGER.info("ROUTE_CUSTOMIZER_APPLIED | operationId={} | originalPath={} | exposedAs={} "
                        + "| replaceOriginal={}",
                match.operationId(), match.path(), targetPath, replaceOriginal);
        return true;
    }

    private void finalizeReplaceOriginal(Paths paths, RouteCustomizerProperties.RouteMapping mapping,
                                          MatchedOperation match, boolean anyApplied) {
        if (!mapping.isReplaceOriginal()) {
            return;
        }
        if (anyApplied) {
            removeOperation(paths, match);
        } else {
            LOGGER.error("ROUTE_CUSTOMIZER_SKIPPED | reason=all_targets_failed_original_retained "
                    + "| operationId={}", match.operationId());
        }
    }

    private MatchedOperation findOperation(Paths paths, RouteCustomizerProperties.RouteMapping mapping,
                                           int mappingIndex) {
        if (mapping.getRouteId() != null && !mapping.getRouteId().isBlank()) {
            return findByRouteId(paths, mapping.getRouteId(), mappingIndex);
        }
        return findByPath(paths, mapping.getPath(), mapping.getMethod(), mappingIndex);
    }

    private MatchedOperation findByRouteId(Paths paths, String routeId, int mappingIndex) {
        for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
            for (HttpMethod method : SCANNED_METHODS) {
                Operation candidate = operationFor(entry.getValue(), method);
                if (candidate == null || isAliasClone(candidate)) {
                    continue;
                }
                if (routeId.equals(RouteIdUtils.computeRouteId(candidate))) {
                    return new MatchedOperation(candidate, entry.getKey(), method, entry.getValue(), mappingIndex);
                }
            }
        }
        return null;
    }

    private MatchedOperation findByPath(Paths paths, String path, String configuredMethod, int mappingIndex) {
        PathItem pathItem = paths.get(path);
        if (pathItem == null) {
            return null;
        }
        List<MatchedOperation> candidates = new ArrayList<>();
        for (HttpMethod method : SCANNED_METHODS) {
            Operation candidate = operationFor(pathItem, method);
            boolean methodAccepted = configuredMethod == null || configuredMethod.equalsIgnoreCase(method.name());
            if (candidate != null && !isAliasClone(candidate) && methodAccepted) {
                candidates.add(new MatchedOperation(candidate, path, method, pathItem, mappingIndex));
            }
        }
        if (candidates.size() != 1) {
            if (candidates.size() > 1) {
                LOGGER.error("ROUTE_CUSTOMIZER_SKIPPED | reason=ambiguous_method | path={}", path);
            }
            return null;
        }
        return candidates.get(0);
    }

    private static boolean isAliasClone(Operation operation) {
        return operation.getExtensions() != null && operation.getExtensions().containsKey(ALIAS_OF_EXTENSION);
    }

    private static boolean hasOperation(Paths paths, String targetPath, HttpMethod method) {
        PathItem existing = paths.get(targetPath);
        return existing != null && operationFor(existing, method) != null;
    }

    private static boolean hasCompatiblePathItemMetadata(Paths paths, MatchedOperation match, String targetPath) {
        PathItem existing = paths.get(targetPath);
        if (existing == null) {
            return true;
        }
        PathItem source = match.pathItem();
        return Objects.equals(existing.getParameters(), source.getParameters())
                && Objects.equals(existing.getDescription(), source.getDescription())
                && Objects.equals(existing.getSummary(), source.getSummary())
                && Objects.equals(existing.getServers(), source.getServers());
    }

    private static Operation operationFor(PathItem pathItem, HttpMethod method) {
        return switch (method.name()) {
            case "GET" -> pathItem.getGet();
            case "POST" -> pathItem.getPost();
            case "PUT" -> pathItem.getPut();
            case "DELETE" -> pathItem.getDelete();
            case "PATCH" -> pathItem.getPatch();
            case "OPTIONS" -> pathItem.getOptions();
            default -> null;
        };
    }

    private static void setOperationFor(PathItem pathItem, HttpMethod method, Operation operation) {
        switch (method.name()) {
            case "GET" -> pathItem.setGet(operation);
            case "POST" -> pathItem.setPost(operation);
            case "PUT" -> pathItem.setPut(operation);
            case "DELETE" -> pathItem.setDelete(operation);
            case "PATCH" -> pathItem.setPatch(operation);
            case "OPTIONS" -> pathItem.setOptions(operation);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    /**
     * Deep-clones the matched operation and preserves compatible source {@link PathItem}
     * metadata. Vendor extensions identify the clone's source. {@code summary}/{@code
     * description} are also rewritten: {@link org.eclipse.ecsp.security.ScopeTagger} (an
     * {@code OperationCustomizer}) bakes the source route id and literal path text into those
     * fields before this {@code GlobalOpenApiCustomizer} ever runs, and never sees this
     * synthetic clone, so without rewriting them here the alias would permanently display the
     * original operation's route id and path in its docs.
     */
    private void cloneOperationToNewPath(Paths paths, MatchedOperation match, String targetPath) {
        Operation clone = ObjectMapperUtil.getObjectMapper().convertValue(match.operation(), Operation.class);
        String originalRouteId = RouteIdUtils.computeRouteId(match.operation());
        clone.setOperationId(AliasIdUtils.createOperationId(match.operation(), match.httpMethod(), targetPath));
        retagDocumentation(clone, match.httpMethod(), match.path(), targetPath, originalRouteId);
        clone.addExtension(ALIAS_OF_EXTENSION, originalRouteId);
        clone.addExtension(ORIGINAL_PATH_EXTENSION, match.path());
        clone.addExtension(MAPPING_INDEX_EXTENSION, match.mappingIndex());
        putOperationWithPathMetadata(paths, targetPath, match, clone);
    }

    /**
     * Rewrites literal references to the original path and route id that {@link
     * org.eclipse.ecsp.security.ScopeTagger} (or the source annotations) baked into
     * {@code summary}/{@code description} text, so the alias's own docs are self-consistent.
     * A no-op wherever the old text isn't actually present.
     */
    private static void retagDocumentation(Operation clone, HttpMethod httpMethod, String originalPath,
                                            String targetPath, String originalRouteId) {
        String aliasRouteId = RouteIdUtils.computeRouteId(clone);
        clone.setSummary(retagSummary(clone.getSummary(), httpMethod, originalPath, targetPath));
        String description = replaceIfPresent(clone.getDescription(), originalPath, targetPath);
        description = replaceIfPresent(description, originalRouteId, aliasRouteId);
        clone.setDescription(description);
    }

    /**
     * Rewrites the summary when it spells out the full original path, or when it is exactly
     * {@code "<METHOD> <path>"} with {@code <path>} a suffix of the original path (a hand-written
     * summary that omits the controller's class-level prefix). Any other summary is left as written.
     */
    private static String retagSummary(String summary, HttpMethod httpMethod, String originalPath,
                                        String targetPath) {
        if (summary == null) {
            return null;
        }
        String replaced = summary.replace(originalPath, targetPath);
        if (!replaced.equals(summary)) {
            return replaced;
        }
        String methodPrefix = httpMethod.name() + " ";
        if (summary.startsWith(methodPrefix)) {
            String summaryPath = summary.substring(methodPrefix.length()).trim();
            if (summaryPath.startsWith("/") && originalPath.endsWith(summaryPath)) {
                return methodPrefix + targetPath;
            }
        }
        return summary;
    }

    private static String replaceIfPresent(String text, String oldValue, String newValue) {
        return text == null || oldValue == null || newValue == null ? text : text.replace(oldValue, newValue);
    }

    private void putOperationWithPathMetadata(Paths paths, String targetPath, MatchedOperation match,
                                               Operation clone) {
        PathItem targetPathItem = paths.get(targetPath);
        if (targetPathItem == null) {
            targetPathItem = ObjectMapperUtil.getObjectMapper().convertValue(match.pathItem(), PathItem.class);
            clearAllOperations(targetPathItem);
            paths.put(targetPath, targetPathItem);
        }
        setOperationFor(targetPathItem, match.httpMethod(), clone);
    }

    private static void clearAllOperations(PathItem pathItem) {
        pathItem.setGet(null);
        pathItem.setPost(null);
        pathItem.setPut(null);
        pathItem.setDelete(null);
        pathItem.setPatch(null);
        pathItem.setOptions(null);
        pathItem.setHead(null);
        pathItem.setTrace(null);
    }

    private static void removeOperation(Paths paths, MatchedOperation match) {
        PathItem pathItem = match.pathItem();
        setOperationFor(pathItem, match.httpMethod(), null);
        if (isEmpty(pathItem)) {
            paths.remove(match.path());
        }
    }

    private static boolean isEmpty(PathItem pathItem) {
        return pathItem.getGet() == null && pathItem.getPost() == null && pathItem.getPut() == null
                && pathItem.getDelete() == null && pathItem.getPatch() == null && pathItem.getOptions() == null
                && pathItem.getHead() == null && pathItem.getTrace() == null;
    }

    private String resolvePlaceholders(String template) {
        if (embeddedValueResolver == null) {
            return template;
        }
        try {
            return embeddedValueResolver.resolveStringValue(template);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * A matched source operation, together with the OpenAPI path key, HTTP method, and
     * {@link PathItem} it currently lives at, and the index of the mapping that matched it.
     */
    private record MatchedOperation(Operation operation, String path, HttpMethod httpMethod, PathItem pathItem,
                                    int mappingIndex) {

        private String operationId() {
            return operation.getOperationId();
        }
    }
}
