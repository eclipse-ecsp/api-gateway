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

package org.eclipse.ecsp.registry.repo;

import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory {@link ApiRouteRepo} for tests that need the behavior of a repository instead of stubbed answers.
 *
 * <p>Entities are copied on the way in and on the way out, as a database does, so changing an entity that was read
 * does not change what is stored until it is saved. The repository counts its reads of all routes and its saves.
 */
public class InMemoryApiRouteRepo implements ApiRouteRepo {

    private final Map<String, ApiRouteEntity> store = new ConcurrentSkipListMap<>();
    private final AtomicInteger findAllCalls = new AtomicInteger();
    private final AtomicInteger saveCalls = new AtomicInteger();
    private volatile Runnable beforeSave = () -> { };

    @Override
    public Optional<ApiRouteEntity> findById(String id) {
        return Optional.ofNullable(store.get(id)).map(InMemoryApiRouteRepo::copy);
    }

    @Override
    public List<ApiRouteEntity> findAll() {
        findAllCalls.incrementAndGet();
        return store.values().stream().map(InMemoryApiRouteRepo::copy).toList();
    }

    @Override
    public ApiRouteEntity save(ApiRouteEntity entity) {
        beforeSave.run();
        saveCalls.incrementAndGet();
        store.put(entity.getId(), copy(entity));
        return copy(entity);
    }

    @Override
    public void delete(ApiRouteEntity entity) {
        store.remove(entity.getId());
    }

    /**
     * Runs an action at the start of every save, such as a pause that makes a slow database.
     *
     * @param action the action to run before a route is saved
     */
    public void beforeSave(Runnable action) {
        this.beforeSave = action;
    }

    /**
     * Puts an entity into the store without counting a save, as data that was there before the test.
     *
     * @param entity the entity to store
     */
    public void seed(ApiRouteEntity entity) {
        store.put(entity.getId(), copy(entity));
    }

    /**
     * Marks a stored route as inactive, as the service health monitor does when a service stops answering.
     *
     * @param id the route id
     */
    public void deactivate(String id) {
        store.get(id).setActive(Boolean.FALSE);
    }

    /**
     * Returns how often all routes were read.
     *
     * @return the number of calls of {@link #findAll()}
     */
    public int findAllCalls() {
        return findAllCalls.get();
    }

    /**
     * Returns how often a route was saved.
     *
     * @return the number of calls of {@link #save(ApiRouteEntity)}
     */
    public int saveCalls() {
        return saveCalls.get();
    }

    /**
     * Returns how many routes are stored.
     *
     * @return the number of stored routes
     */
    public int size() {
        return store.size();
    }

    private static ApiRouteEntity copy(ApiRouteEntity source) {
        ApiRouteEntity copy = new ApiRouteEntity();
        copy.setId(source.getId());
        copy.setService(source.getService());
        copy.setContextPath(source.getContextPath());
        copy.setApiDocs(source.getApiDocs());
        copy.setRoute(source.getRoute());
        copy.setChecksum(source.getChecksum());
        copy.setActive(source.getActive());
        return copy;
    }
}
