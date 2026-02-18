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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.ecsp.registry.events.RouteEventType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Event data for client access control configuration changes.
 * Contains list of client IDs whose configurations were modified.
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class ClientAccessControlEventData extends AbstractEventData {
    private static final long serialVersionUID = 1L;
    
    private final List<String> clientIds;
    
    /**
     * Operation type (CREATE, UPDATE, DELETE) - optional metadata.
     */
    private final String operation;
    
    /**
     * Constructor for ClientAccessControlEventData.
     *
     * @param clientIds list of client IDs that changed
     */
    public ClientAccessControlEventData(List<String> clientIds) {
        this(clientIds, null);
    }
    
    /**
     * Constructor for ClientAccessControlEventData.
     *
     * @param clientIds list of client IDs that changed
     * @param operation operation type (CREATE, UPDATE, DELETE)
     */
    public ClientAccessControlEventData(List<String> clientIds, String operation) {
        super();
        this.clientIds = Collections.unmodifiableList(new ArrayList<>(clientIds));
        this.operation = operation;
    }
    
    /**
     * Full constructor for ClientAccessControlEventData (for deserialization).
     *
     * @param eventId unique event identifier
     * @param timestamp event generation time
     * @param clientIds list of client IDs that changed
     * @param operation operation type
     */
    @JsonCreator
    public ClientAccessControlEventData(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("clientIds") List<String> clientIds,
            @JsonProperty("operation") String operation) {
        super();
        this.clientIds = Collections.unmodifiableList(new ArrayList<>(clientIds));
        this.operation = operation;
    }
    
    @Override
    public RouteEventType getEventType() {
        return RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED;
    }
}
