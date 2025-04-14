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

package org.eclipse.ecsp.register.model;

import dev.morphia.annotations.Entity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * POJO defines RouteDefinition object.
 *
 * <p>This class represents a route definition with various properties
 * such as id, predicates, filters, uri, metadata, etc.
 * It includes validation annotations to ensure the integrity of the data.
 *
 * @author Sbala2
 */
@Validated
@Entity
public class RouteDefinition {

    @NotEmpty
    private String id;

    @NotEmpty
    @Valid
    private List<PredicateDefinition> predicates = new ArrayList<>();

    @Valid
    private List<FilterDefinition> filters = new ArrayList<>();

    @NotNull
    private URI uri;

    private Map<String, Object> metadata = new HashMap<>();

    private String service;
    private String contextPath;
    private Boolean apiDocs = false;
    private int order = 0;

    /**
     * Default constructor for RouteDefinition.
     */
    public RouteDefinition() {
    }

    private String cacheKey;

    private String cacheSize;

    /**
     * Gets the cache size.
     *
     * @return the cache size
     */
    public String getCacheSize() {
        return cacheSize;
    }

    /**
     * Sets the cache size.
     *
     * @param cacheSize the cache size to set
     */
    public void setCacheSize(String cacheSize) {
        this.cacheSize = cacheSize;
    }

    private String cacheTtl;

    /**
     * Gets the cache TTL (Time To Live).
     *
     * @return the cache TTL
     */
    public String getCacheTtl() {
        return cacheTtl;
    }

    /**
     * Sets the cache TTL (Time To Live).
     *
     * @param cacheTtl the cache TTL to set
     */
    public void setCacheTtl(String cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    /**
     * Gets the cache key.
     *
     * @return the cache key
     */
    public String getCacheKey() {
        return cacheKey;
    }

    /**
     * Sets the cache key.
     *
     * @param cacheKey the cache key to set
     */
    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the context path.
     *
     * @return the context path
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Sets the context path.
     *
     * @param contextPath the context path to set
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * Gets the list of predicates.
     *
     * @return the list of predicates
     */
    public List<PredicateDefinition> getPredicates() {
        return predicates;
    }

    /**
     * Sets the list of predicates.
     *
     * @param predicates the list of predicates to set
     */
    public void setPredicates(List<PredicateDefinition> predicates) {
        this.predicates = predicates;
    }

    /**
     * Gets the list of filters.
     *
     * @return the list of filters
     */
    public List<FilterDefinition> getFilters() {
        return filters;
    }

    /**
     * Sets the list of filters.
     *
     * @param filters the list of filters to set
     */
    public void setFilters(List<FilterDefinition> filters) {
        this.filters = filters;
    }

    /**
     * Gets the URI.
     *
     * @return the URI
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Sets the URI.
     *
     * @param uri the URI to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Gets the order.
     *
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * Sets the order.
     *
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * Gets the metadata.
     *
     * @return the metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata.
     *
     * @param metadata the metadata to set
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    public String getService() {
        return service;
    }

    /**
     * Sets the service.
     *
     * @param service the service to set
     */
    public void setService(String service) {
        this.service = service;
    }

    /**
     * Gets the API documentation flag.
     *
     * @return the API documentation flag
     */
    public Boolean getApiDocs() {
        return apiDocs;
    }

    /**
     * Sets the API documentation flag.
     *
     * @param apiDocs the API documentation flag to set
     */
    public void setApiDocs(Boolean apiDocs) {
        this.apiDocs = apiDocs;
    }

    /**
     * Checks if this RouteDefinition is equal to another object.
     *
     * @param o the object to compare
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RouteDefinition that = (RouteDefinition) o;
        return this.order == that.order && Objects.equals(this.id, that.id)
                && Objects.equals(this.predicates, that.predicates) && Objects.equals(this.filters, that.filters)
                && Objects.equals(this.uri, that.uri) && Objects.equals(this.metadata, that.metadata);
    }

    /**
     * Generates a hash code for this RouteDefinition.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.predicates, this.filters, this.uri, this.metadata, this.order);
    }

    /**
     * Returns a string representation of this RouteDefinition.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "RouteDefinition [id=" + id + ", predicates=" + predicates + ", filters=" + filters + ", uri=" + uri
                + ", metadata=" + metadata + ", service=" + service + ", apiDocs=" + apiDocs + ", order=" + order + "]";
    }

}