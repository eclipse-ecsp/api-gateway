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

package org.eclipse.ecsp.registry.config;

import org.eclipse.ecsp.registry.events.EventPublisherContext;
import org.eclipse.ecsp.registry.events.data.AbstractEventData;
import org.eclipse.ecsp.registry.events.strategy.EventPublishingStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for event publishing infrastructure.
 */
@Configuration
@EnableConfigurationProperties(EventProperties.class)
public class EventPublisherConfiguration {
    /**
     * Default constructor.
     */
    public EventPublisherConfiguration() {
        // Default constructor
    }
    
    /**
     * Create EventPublisherContext bean.
     *
     * @param strategies list of all available event publishing strategies
     * @return EventPublisherContext instance
     */
    @Bean
    public EventPublisherContext eventPublisherContext(
            List<EventPublishingStrategy<? extends AbstractEventData>> strategies) {
        return new EventPublisherContext(strategies);
    }
}
