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

package org.eclipse.ecsp.registry.events.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.ecsp.registry.events.RouteEventType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Event data for route configuration changes.
 * Contains service names and route IDs that were modified.
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class RouteChangeEventData extends AbstractEventData {
    private static final long serialVersionUID = 1L;
    
    private final List<String> services;
    private final List<String> routes;
    
    /**
     * Constructor for RouteChangeEventData.
     *
     * @param services list of service names that changed
     * @param routes list of route IDs that changed
     */
    public RouteChangeEventData(List<String> services, List<String> routes) {
        super();
        this.services = Collections.unmodifiableList(new ArrayList<>(services));
        this.routes = Collections.unmodifiableList(new ArrayList<>(routes));
    }
    
    @Override
    public RouteEventType getEventType() {
        return RouteEventType.ROUTE_CHANGE;
    }
}
