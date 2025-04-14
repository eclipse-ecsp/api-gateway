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

package org.eclipse.ecsp.registry.metrics;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * ServiceMetricsEvent event pojo.
 *
 * @author Abhishek Kumar
 */
@Getter
public class ServiceMetricsEvent extends ApplicationEvent {
    /**
     * event for the service.
     */
    private final String serviceName;
    /**
     * active status of the services .
     */
    private final boolean status;
    /**
     * total api count for the service.
     */
    private final int totalRoutes;

    /**
     * Constructor to initialize {@link ServiceMetricsEvent}.
     *
     * @param source      event source.
     * @param serviceName event for the service.
     * @param status      status of the services.
     * @param totalRoutes total route count of the service.
     */
    public ServiceMetricsEvent(Object source, String serviceName, boolean status, int totalRoutes) {
        super(source);
        this.serviceName = serviceName;
        this.status = status;
        this.totalRoutes = totalRoutes;
    }
}
