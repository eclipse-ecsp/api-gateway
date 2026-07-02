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

import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.config.ChecksumProperties;
import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import org.eclipse.ecsp.registry.events.EventPublisherContext;
import org.eclipse.ecsp.registry.events.data.RouteChangeEventData;
import org.eclipse.ecsp.registry.repo.ApiRouteRepo;
import org.eclipse.ecsp.registry.utils.ApiRouteUtil;
import org.eclipse.ecsp.registry.utils.RegistryConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to create, read, update, and delete routes.
 *
 * <p>When {@code api.registry.change-detection.enabled=true} (the default), inbound
 * registrations are compared against the stored checksum to classify the change as
 * {@link RouteChangeType#NEW}, {@link RouteChangeType#UPDATED}, or
 * {@link RouteChangeType#UNCHANGED}. Unchanged routes skip the DB write and produce
 * only a {@code DEBUG} log entry. A Micrometer counter {@code api.route.change.total}
 * tagged with {@code changeType} is incremented for every classified event.
 *
 * <p>When {@code change-detection.enabled=false}, the service behaves identically to the
 * pre-feature implementation: every registration triggers an unconditional save (FR-08).
 */
@Service
public class ApiRouteService {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiRouteService.class);

    private static final String METRIC_ROUTE_CHANGE = "api.route.change.total";
    private static final String TAG_CHANGE_TYPE = "changeType";

    private final ApiRouteRepo apiRouteRepo;
    private final EventPublisherContext eventPublisher;
    private final ChecksumProperties checksumProperties;
    private final ChecksumService checksumService;
    private final MeterRegistry meterRegistry;

    @Value("${api.gatewayUrls:}")
    private String apiGatewayUrl;

    /**
     * Constructs the service with all required dependencies.
     *
     * @param apiRouteRepo       route persistence repository
     * @param eventPublisher     optional event publisher; absent when event publishing is disabled
     * @param checksumProperties change-detection configuration
     * @param checksumService    checksum computation strategy
     * @param meterRegistry      Micrometer registry for change-type counters
     */
    public ApiRouteService(ApiRouteRepo apiRouteRepo,
                           Optional<EventPublisherContext> eventPublisher,
                           ChecksumProperties checksumProperties,
                           ChecksumService checksumService,
                           MeterRegistry meterRegistry) {
        this.apiRouteRepo = apiRouteRepo;
        this.eventPublisher = eventPublisher.orElse(null);
        this.checksumProperties = checksumProperties;
        this.checksumService = checksumService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Creates or updates a route definition.
     *
     * <p>When change-detection is enabled, the incoming route is compared against the stored
     * checksum. Unchanged routes exit early without a DB write (FR-03). New or updated routes
     * are persisted, logged, metered, and published.
     *
     * @param model route definition to register; must not be {@code null} and must have an {@code id}
     * @return the persisted route definition
     * @throws IllegalArgumentException if {@code model} is {@code null} or has no {@code id}
     */
    public RouteDefinition createOrUpdate(RouteDefinition model) {
        if (model == null || model.getId() == null) {
            throw new IllegalArgumentException("Invalid route request");
        }
        Optional<ApiRouteEntity> existing = apiRouteRepo.findById(model.getId());
        if (!checksumProperties.isEnabled()) {
            return saveUnconditionally(model, existing);
        }
        return saveWithChangeDetection(model, existing);
    }

    /**
     * Lists all active route definitions.
     *
     * @return list of active routes; never {@code null}
     */
    public List<RouteDefinition> list() {
        return ApiRouteUtil.convertActive(apiRouteRepo.findAll());
    }

    /**
     * Reads a single route definition by identifier.
     *
     * @param id route identifier; must not be {@code null}
     * @return the route definition
     * @throws IllegalArgumentException if {@code id} is {@code null} or not found
     */
    public RouteDefinition read(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Invalid route id");
        }
        Optional<ApiRouteEntity> result = apiRouteRepo.findById(id);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("ApiRoute not found for id: " + id);
        }
        return ApiRouteUtil.convert(result.get());
    }

    /**
     * Deletes a route by identifier, emitting a structured {@code DELETED} log entry
     * and a change event before removal (FR-04, FR-05).
     *
     * @param id route identifier; must not be {@code null}
     * @throws IllegalArgumentException if {@code id} is {@code null} or not found
     */
    public void delete(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Invalid route id");
        }
        Optional<ApiRouteEntity> result = apiRouteRepo.findById(id);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("ApiRoute not found for id: " + id);
        }
        ApiRouteEntity entity = result.get();
        String previousChecksum = entity.getChecksum();
        String correlationId = UUID.randomUUID().toString();

        LOGGER.info("{} | eventId={} | routeId={} | service={} | changeType=DELETED"
                        + " | previousChecksum={} | api-gateway-url={}",
                RegistryConstants.LOG_EVENT_ROUTE_CHANGE, correlationId, entity.getId(),
                entity.getService(), previousChecksum, apiGatewayUrl);

        apiRouteRepo.delete(entity);
        incrementMetric(RouteChangeType.DELETED);

        if (eventPublisher != null && entity.getService() != null) {
            RouteChangeEventData eventData = new RouteChangeEventData(
                    List.of(entity.getService()), List.of(),
                    RouteChangeType.DELETED, null, previousChecksum, correlationId);
            eventPublisher.publishEvent(eventData);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Saves the route unconditionally without any checksum evaluation (FR-08).
     *
     * @param model    incoming route definition
     * @param existing current entity from the repository, if present
     * @return the persisted route definition
     */
    private RouteDefinition saveUnconditionally(RouteDefinition model,
                                                Optional<ApiRouteEntity> existing) {
        ApiRouteEntity entity = existing.orElseGet(ApiRouteEntity::new);
        if (entity.getId() == null) {
            entity.setId(model.getId());
        }
        populateEntity(entity, model, null);
        entity = apiRouteRepo.save(entity);
        LOGGER.info("Created/Updated ApiRoute: {}", entity.getId());
        publishChangeEvent(entity, null, RouteChangeType.UPDATED, null, null);
        return ApiRouteUtil.convert(entity);
    }

    /**
     * Saves the route only if it is new or has changed (FR-01, FR-02, FR-03, FR-07).
     *
     * @param model    incoming route definition
     * @param existing current entity from the repository, if present
     * @return the persisted or unchanged route definition
     */
    private RouteDefinition saveWithChangeDetection(RouteDefinition model,
                                                    Optional<ApiRouteEntity> existing) {
        Optional<String> newChecksumOpt = checksumService.compute(model);
        RouteChangeType changeType = resolveChangeType(existing, newChecksumOpt);

        if (changeType == RouteChangeType.UNCHANGED  && existing.isPresent()) {
            logUnchanged(model, existing.get().getChecksum());
            incrementMetric(RouteChangeType.UNCHANGED);
            return ApiRouteUtil.convert(existing.get());
        }

        ApiRouteEntity entity = existing.orElseGet(ApiRouteEntity::new);
        if (entity.getId() == null) {
            entity.setId(model.getId());
        }
        String previousChecksum = existing.map(ApiRouteEntity::getChecksum).orElse(null);
        populateEntity(entity, model, newChecksumOpt.orElse(null));
        entity = apiRouteRepo.save(entity);

        String correlationId = UUID.randomUUID().toString();
        String currentChecksum = entity.getChecksum();
        logChangeEvent(changeType, entity, model, previousChecksum, currentChecksum, correlationId);
        incrementMetric(changeType);
        publishChangeEvent(entity, correlationId, changeType, currentChecksum, previousChecksum);
        return ApiRouteUtil.convert(entity);
    }

    /**
     * Determines the change type by comparing the stored checksum with the newly computed one.
     *
     * @param existing       current entity from the repository
     * @param newChecksumOpt newly computed checksum; empty if computation failed
     * @return resolved {@link RouteChangeType}
     */
    private RouteChangeType resolveChangeType(Optional<ApiRouteEntity> existing,
                                              Optional<String> newChecksumOpt) {
        if (existing.isEmpty()) {
            return RouteChangeType.NEW;
        }
        if (newChecksumOpt.isEmpty()) {
            return RouteChangeType.UPDATED;
        }
        String stored = existing.get().getChecksum();
        if (stored == null) {
            return RouteChangeType.UPDATED;
        }
        return stored.equals(newChecksumOpt.get()) ? RouteChangeType.UNCHANGED : RouteChangeType.UPDATED;
    }

    /**
     * Emits a structured {@code INFO} log entry for {@code NEW} or {@code UPDATED} events (FR-05).
     *
     * @param changeType       resolved change type
     * @param entity           persisted entity
     * @param model            incoming route definition
     * @param previousChecksum checksum before the change
     * @param currentChecksum  checksum after the change
     * @param correlationId    correlation UUID
     */
    private void logChangeEvent(RouteChangeType changeType,
                                ApiRouteEntity entity,
                                RouteDefinition model,
                                String previousChecksum,
                                String currentChecksum,
                                String correlationId) {
        if (changeType == RouteChangeType.NEW) {
            LOGGER.info("{} | eventId={} | routeId={} | service={} | changeType=NEW"
                            + " | currentChecksum={} | predicates={} | api-gateway-url={}",
                    RegistryConstants.LOG_EVENT_ROUTE_CHANGE, correlationId, entity.getId(),
                    entity.getService(), currentChecksum, model.getPredicates(), apiGatewayUrl);
        } else {
            LOGGER.info("{} | eventId={} | routeId={} | service={} | changeType=UPDATED"
                            + " | previousChecksum={} | currentChecksum={} | api-gateway-url={}",
                    RegistryConstants.LOG_EVENT_ROUTE_CHANGE, correlationId, entity.getId(),
                    entity.getService(), previousChecksum, currentChecksum, apiGatewayUrl);
        }
    }

    /**
     * Emits a structured {@code DEBUG} log entry for {@code UNCHANGED} routes (FR-03).
     *
     * @param model            incoming route definition
     * @param existingChecksum checksum stored in the repository
     */
    private void logUnchanged(RouteDefinition model, String existingChecksum) {
        LOGGER.debug("{} | routeId={} | service={} | changeType=UNCHANGED"
                        + " | checksum={} | skippingWrite=true",
                RegistryConstants.LOG_EVENT_ROUTE_CHANGE,
                model.getId(), model.getService(), existingChecksum);
    }

    /**
     * Populates entity fields from the incoming route definition and the computed checksum.
     *
     * @param entity   entity to populate
     * @param model    source route definition
     * @param checksum computed checksum; {@code null} when change-detection is disabled
     */
    private void populateEntity(ApiRouteEntity entity, RouteDefinition model, String checksum) {
        entity.setRoute(model);
        entity.setService(model.getService());
        entity.setContextPath(model.getContextPath());
        if (Boolean.TRUE.equals(model.getApiDocs())) {
            entity.setApiDocs(model.getApiDocs());
        }
        entity.setActive(Boolean.TRUE);
        entity.setChecksum(checksum);
    }

    /**
     * Publishes a {@link RouteChangeEventData} event if the event publisher is available.
     *
     * @param entity           persisted entity
     * @param correlationId    correlation UUID; may be {@code null}
     * @param changeType       resolved change type
     * @param currentChecksum  checksum after the change; may be {@code null}
     * @param previousChecksum checksum before the change; may be {@code null}
     */
    private void publishChangeEvent(ApiRouteEntity entity,
                                    String correlationId,
                                    RouteChangeType changeType,
                                    String currentChecksum,
                                    String previousChecksum) {
        if (eventPublisher != null && entity.getService() != null) {
            RouteChangeEventData eventData = new RouteChangeEventData(
                    List.of(entity.getService()), List.of(),
                    changeType, currentChecksum, previousChecksum, correlationId);
            eventPublisher.publishEvent(eventData);
        }
    }

    /**
     * Increments the Micrometer change-type counter.
     *
     * @param changeType resolved change type
     */
    private void incrementMetric(RouteChangeType changeType) {
        meterRegistry.counter(METRIC_ROUTE_CHANGE, TAG_CHANGE_TYPE,
                changeType.name().toLowerCase()).increment();
    }
}
