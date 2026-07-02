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

package org.eclipse.ecsp.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.register.model.FilterDefinition;
import org.eclipse.ecsp.register.model.PredicateDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.config.ChecksumProperties;
import org.eclipse.ecsp.registry.utils.RegistryConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * SHA-256 based implementation of {@link ChecksumService}.
 *
 * <p>Produces a deterministic, 64-character lowercase hex checksum by normalising
 * selected {@link RouteDefinition} fields into a canonical JSON representation
 * before hashing. The effective field set is governed by
 * {@link ChecksumProperties.Checksum} {@code includeFields}.
 */
@Service
public class Sha256ChecksumService implements ChecksumService {

    private static final IgniteLogger LOGGER =
            IgniteLoggerFactory.getLogger(Sha256ChecksumService.class);

    private static final String FIELD_PREDICATES = "predicates";
    private static final String FIELD_FILTERS = "filters";
    private static final String FIELD_URI = "uri";
    private static final String FIELD_ACTIVE = "active";
    private static final String FIELD_ORDER = "order";
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_ID = "id";
    private static final String FIELD_SERVICE = "service";
    private static final String FIELD_CONTEXT_PATH = "contextPath";

    private static final Set<String> DEFAULT_FIELDS =
            Set.of(FIELD_PREDICATES, FIELD_FILTERS, FIELD_URI, FIELD_ACTIVE);

    private static final Set<String> ALL_FIELDS =
            Set.of(FIELD_PREDICATES, FIELD_FILTERS, FIELD_URI, FIELD_ACTIVE,
                    FIELD_ORDER, FIELD_METADATA, FIELD_ID, FIELD_SERVICE, FIELD_CONTEXT_PATH);

    private static final String WILDCARD = "*";

    private final ChecksumProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the service with required dependencies.
     *
     * @param properties   checksum configuration properties
     * @param objectMapper Jackson ObjectMapper used for canonical JSON serialisation
     */
    public Sha256ChecksumService(ChecksumProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>On any failure, logs a {@code WARN} entry and returns {@code Optional.empty()},
     * which callers must treat as {@link RouteChangeType#UPDATED}.
     */
    @Override
    public Optional<String> compute(RouteDefinition route) {
        try {
            String canonical = buildCanonicalJson(route);
            MessageDigest digest = MessageDigest.getInstance(properties.getChecksum().getAlgorithm());
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return Optional.of(HexFormat.of().formatHex(hash));
        } catch (NoSuchAlgorithmException | JsonProcessingException ex) {
            LOGGER.warn("{} | routeId={} | reason={} | fallback=UPDATED",
                    RegistryConstants.LOG_EVENT_CHECKSUM_FAILURE,
                    route.getId(), ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Builds the canonical JSON string from the normalised route fields.
     *
     * @param route source route definition
     * @return canonical JSON string
     * @throws JsonProcessingException on serialisation failure
     */
    private String buildCanonicalJson(RouteDefinition route) throws JsonProcessingException {
        TreeMap<String, Object> fieldsMap = buildChecksumMap(route);
        return objectMapper.writeValueAsString(fieldsMap);
    }

    /**
     * Assembles a sorted map of the effective route fields for hashing.
     *
     * @param route source route definition
     * @return sorted map of field name → normalised value
     */
    private TreeMap<String, Object> buildChecksumMap(RouteDefinition route) {
        Set<String> effectiveFields = getEffectiveFields();
        TreeMap<String, Object> map = new TreeMap<>();
        for (String field : ALL_FIELDS) {
            if (effectiveFields.contains(field)) {
                map.put(field, getFieldValue(route, field));
            }
        }
        return map;
    }

    /**
     * Resolves the effective set of field names to include in the checksum.
     *
     * @return set of field names; never {@code null}
     */
    private Set<String> getEffectiveFields() {
        List<String> configured = properties.getChecksum().getIncludeFields();
        if (configured == null || configured.isEmpty()) {
            return DEFAULT_FIELDS;
        }
        if (configured.size() == 1 && WILDCARD.equals(configured.get(0))) {
            return ALL_FIELDS;
        }
        return new HashSet<>(configured);
    }

    /**
     * Extracts and normalises the value of a single field from the route definition.
     *
     * @param route route definition
     * @param field field name
     * @return normalised value; never {@code null}
     */
    private Object getFieldValue(RouteDefinition route, String field) {
        switch (field) {
            case FIELD_PREDICATES:   return normalizePredicates(route.getPredicates());
            case FIELD_FILTERS:      return normalizeFilters(route.getFilters());
            case FIELD_URI:          return normalizeUri(route.getUri());
            case FIELD_METADATA:     return normalizeMetadata(route.getMetadata());
            case FIELD_ORDER:        return String.valueOf(route.getOrder());
            case FIELD_ID:           return route.getId() != null ? route.getId() : "";
            case FIELD_SERVICE:      return route.getService() != null ? route.getService() : "";
            case FIELD_CONTEXT_PATH: return route.getContextPath() != null ? route.getContextPath() : "";
            case FIELD_ACTIVE:       return Boolean.TRUE.toString();
            default:                 return "";
        }
    }

    /**
     * Normalises the list of predicates: sorts by name, uppercases Method values.
     *
     * @param predicates raw predicate list; may be {@code null}
     * @return sorted, normalised list
     */
    private List<Map<String, Object>> normalizePredicates(List<PredicateDefinition> predicates) {
        if (predicates == null) {
            return new ArrayList<>();
        }
        return predicates.stream()
                .map(this::normalizeOnePredicate)
                .sorted(Comparator.comparing(m -> String.valueOf(m.get("name"))))
                .toList();
    }

    /**
     * Normalises a single predicate definition.
     *
     * @param predicate predicate to normalise
     * @return sorted map with {@code name} and {@code args} keys
     */
    private Map<String, Object> normalizeOnePredicate(PredicateDefinition predicate) {
        TreeMap<String, Object> map = new TreeMap<>();
        String name = predicate.getName() != null ? predicate.getName() : "";
        map.put("name", name);
        TreeMap<String, String> args = new TreeMap<>();
        if (predicate.getArgs() != null) {
            boolean isMethod = "Method".equalsIgnoreCase(name);
            predicate.getArgs().forEach((k, v) -> {
                String value = v != null ? v : "";
                args.put(k, isMethod ? value.toUpperCase() : value);
            });
        }
        map.put("args", args);
        return map;
    }

    /**
     * Normalises the list of filters: sorts by name with args keys sorted.
     *
     * @param filters raw filter list; may be {@code null}
     * @return sorted, normalised list
     */
    private List<Map<String, Object>> normalizeFilters(List<FilterDefinition> filters) {
        if (filters == null) {
            return new ArrayList<>();
        }
        return filters.stream()
                .map(this::normalizeOneFilter)
                .sorted(Comparator.comparing(m -> String.valueOf(m.get("name"))))
                .toList();
    }

    /**
     * Normalises a single filter definition.
     *
     * @param filter filter to normalise
     * @return sorted map with {@code name} and {@code args} keys
     */
    private Map<String, Object> normalizeOneFilter(FilterDefinition filter) {
        TreeMap<String, Object> map = new TreeMap<>();
        map.put("name", filter.getName() != null ? filter.getName() : "");
        TreeMap<String, String> args = new TreeMap<>();
        if (filter.getArgs() != null) {
            filter.getArgs().forEach((k, v) -> args.put(k, v != null ? v : ""));
        }
        map.put("args", args);
        return map;
    }

    /**
     * Normalises a URI: lowercases and strips a trailing slash.
     *
     * @param uri URI to normalise; may be {@code null}
     * @return normalised URI string, or empty string for {@code null}
     */
    private String normalizeUri(URI uri) {
        if (uri == null) {
            return "";
        }
        String value = uri.toString().toLowerCase();
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * Normalises metadata by sorting keys lexicographically.
     *
     * @param metadata raw metadata map; may be {@code null}
     * @return sorted TreeMap, or empty map for {@code null}
     */
    private TreeMap<String, Object> normalizeMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return new TreeMap<>();
        }
        return new TreeMap<>(metadata);
    }
}
