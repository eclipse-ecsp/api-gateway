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
import org.eclipse.ecsp.register.conflict.RouteKey;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.config.RouteConflictProperties;
import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import org.eclipse.ecsp.registry.exception.RouteConflictException;
import org.eclipse.ecsp.registry.repo.ApiRouteRepo;
import org.eclipse.ecsp.registry.utils.RegistryConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Detects a route registration that conflicts with a registered route.
 *
 * <p>A route is identified by its {@link RouteKey}: the HTTP method and the path with the names of the path
 * variables left out, so {@code POST /v1/vehicle/{vehicleId}} and {@code POST /v1/vehicle/{vin}} are the same
 * route. api-gateway can use only one of two routes with the same key, so the registry accepts the first one and
 * rejects every other.
 *
 * <p>The one exception is the same API registered again: a registered route with the same service name and the
 * same route id is never a conflict, which is what every replica of a service does when it starts. A route with
 * another route id is a conflict even when it belongs to the same service, and so is a route with the same route
 * id that belongs to another service. A registered route counts whether it is active or not, because a route that
 * the health monitor deactivated returns when its service recovers.
 *
 * <p>Routes that are not the same are told apart by route precedence and are no conflict: a static path and a
 * parameterized path ({@code /v1/vehicle/status} and {@code /v1/vehicle/{id}}), and the same path with another
 * method.
 *
 * <p>Only a registration that claims a key is checked: a new route, or a route that moves to another method or
 * path. A route that keeps the key it already owns is not checked again, so duplicates registered before conflict
 * detection existed keep working.
 */
@Component
public class RouteConflictValidator {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteConflictValidator.class);
    private static final String METRIC_ROUTE_CONFLICT = "api.route.conflict.total";
    private static final String TAG_OUTCOME = "outcome";
    private static final String OUTCOME_REJECTED = "rejected";
    private static final String OUTCOME_ACCEPTED = "accepted";
    private static final String ANY_METHOD = "ANY";

    private final ApiRouteRepo apiRouteRepo;
    private final RouteConflictProperties properties;
    private final MeterRegistry meterRegistry;

    /**
     * Constructs the validator with all required dependencies.
     *
     * @param apiRouteRepo  route persistence repository holding the registered routes
     * @param properties    conflict detection configuration
     * @param meterRegistry Micrometer registry for the conflict counter
     */
    public RouteConflictValidator(ApiRouteRepo apiRouteRepo, RouteConflictProperties properties,
                                  MeterRegistry meterRegistry) {
        this.apiRouteRepo = apiRouteRepo;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Checks a registration against the registered routes.
     *
     * <p>In {@code REJECT} mode a conflict is counted, logged and rejected. In {@code WARN} mode it is counted and
     * logged and the registration goes on. In {@code OFF} mode nothing is checked.
     *
     * @param incoming the route to register
     * @param existing the registered route with the same route id, if any
     * @throws RouteConflictException in {@code REJECT} mode, when another route has the same method and path
     */
    public void validate(RouteDefinition incoming, Optional<ApiRouteEntity> existing) {
        if (properties.getMode() == RouteConflictProperties.Mode.OFF) {
            return;
        }
        Optional<RouteKey> key = RouteKey.of(incoming);
        if (key.isEmpty() || keepsItsKey(incoming, existing, key)) {
            return;
        }
        List<ApiRouteEntity> conflicts = findConflicts(incoming, key.get());
        if (conflicts.isEmpty()) {
            warnWhenOwnerChanges(incoming, existing);
        } else {
            report(incoming, conflicts);
        }
    }

    private static boolean keepsItsKey(RouteDefinition incoming, Optional<ApiRouteEntity> existing,
                                       Optional<RouteKey> key) {
        return existing.filter(stored -> isSameApi(incoming, stored))
                .flatMap(stored -> RouteKey.of(stored.getRoute()))
                .equals(key);
    }

    /**
     * The same service registering the same route id is the same API, as it is when a replica starts.
     */
    private static boolean isSameApi(RouteDefinition incoming, ApiRouteEntity stored) {
        return Objects.equals(stored.getId(), incoming.getId())
                && Objects.equals(stored.getService(), incoming.getService());
    }

    private List<ApiRouteEntity> findConflicts(RouteDefinition incoming, RouteKey key) {
        List<ApiRouteEntity> conflicts = new ArrayList<>();
        List<ApiRouteEntity> registered = apiRouteRepo.findAll();
        if (registered != null) {
            for (ApiRouteEntity stored : registered) {
                if (!isSameApi(incoming, stored) && RouteKey.of(stored.getRoute()).filter(key::equals).isPresent()) {
                    conflicts.add(stored);
                }
            }
        }
        return conflicts;
    }

    private void report(RouteDefinition incoming, List<ApiRouteEntity> conflicts) {
        boolean reject = properties.getMode() == RouteConflictProperties.Mode.REJECT;
        String event = reject
                ? RegistryConstants.LOG_EVENT_ROUTE_CONFLICT_REJECTED
                : RegistryConstants.LOG_EVENT_ROUTE_CONFLICT_ACCEPTED;
        conflicts.forEach(stored -> LOGGER.warn("{} | routeId={} | service={} | api={} | conflictingRouteId={}"
                        + " | conflictingService={} | conflictingApi={} | conflictingActive={}",
                event, incoming.getId(), incoming.getService(), describe(incoming), stored.getId(),
                stored.getService(), describe(stored.getRoute()), stored.getActive()));
        meterRegistry.counter(METRIC_ROUTE_CONFLICT, TAG_OUTCOME, reject ? OUTCOME_REJECTED : OUTCOME_ACCEPTED)
                .increment();
        if (reject) {
            throw new RouteConflictException(message(incoming, conflicts),
                    conflicts.stream().map(ApiRouteEntity::getId).toList());
        }
    }

    /**
     * A route id that another service registered is no conflict as long as the method and path differ, but the
     * registration replaces that service's route, which is worth a line in the log.
     */
    private void warnWhenOwnerChanges(RouteDefinition incoming, Optional<ApiRouteEntity> existing) {
        existing.filter(stored -> !Objects.equals(stored.getService(), incoming.getService()))
                .ifPresent(stored -> LOGGER.warn("{} | routeId={} | previousService={} | service={}",
                        RegistryConstants.LOG_EVENT_ROUTE_OWNER_CHANGED, incoming.getId(), stored.getService(),
                        incoming.getService()));
    }

    private static String message(RouteDefinition incoming, List<ApiRouteEntity> conflicts) {
        String owners = conflicts.stream().map(RouteConflictValidator::describeOwner)
                .collect(Collectors.joining(", "));
        return "Route '" + incoming.getId() + "' of service '" + incoming.getService() + "' cannot be registered: "
                + describe(incoming) + " has the same method and path as " + owners
                + ". Use another method or path, or delete the registered route if it is stale.";
    }

    private static String describeOwner(ApiRouteEntity stored) {
        return "route '" + stored.getId() + "' of service '" + stored.getService() + "' ("
                + describe(stored.getRoute()) + (Boolean.TRUE.equals(stored.getActive()) ? "" : ", inactive") + ")";
    }

    private static String describe(RouteDefinition route) {
        String method = RouteKey.methodOf(route);
        return (StringUtils.hasText(method) ? method.trim().toUpperCase(Locale.ROOT) : ANY_METHOD)
                + " " + RouteKey.pathOf(route);
    }
}
