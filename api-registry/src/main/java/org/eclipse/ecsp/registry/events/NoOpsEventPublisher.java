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

package org.eclipse.ecsp.registry.events;

import org.eclipse.ecsp.registry.events.data.AbstractEventData;
import org.eclipse.ecsp.registry.utils.RegistryConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-operation implementation of EventPublisher.
 * Used when events are disabled. Logs events but does not publish them.
 */
@Component
@ConditionalOnProperty(name = RegistryConstants.REGISTRY_EVENT_ENABLED, havingValue = "false", matchIfMissing = true)
public class NoOpsEventPublisher implements EventPublisher {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(NoOpsEventPublisher.class);

    /**
     * Constructor for NoOpsEventPublisher.
     */
    public NoOpsEventPublisher() {
        LOGGER.info("NoOpsEventPublisher initialized - events are disabled");
    }

    @Override
    public <T extends AbstractEventData> boolean publishEvent(T eventData) {
        LOGGER.debug("Event publishing disabled - skipping event: eventId={}, eventType={}",
                eventData.getEventId(), eventData.getEventType());
        return true; // Return true as there's nothing to fail
    }
}
