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

package org.eclipse.ecsp.registry.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.ecsp.domain.Version;
import org.eclipse.ecsp.entities.AuditableIgniteEntity;
import org.eclipse.ecsp.entities.IgniteEntity;
import java.time.LocalDateTime;

/**
 * Entity representing Rate Limit Configuration.
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@Entity(name = "rate_limit_configs")
@dev.morphia.annotations.Entity("rate_limit_configs")
public class RateLimitConfigEntity implements AuditableIgniteEntity, IgniteEntity {
    
    @Id
    @dev.morphia.annotations.Id
    private String id;
    private String routeId;
    private String service;
    private long replenishRate;
    private long burstCapacity;
    private boolean includeHeaders;
    private RateLimitType rateLimitType;
    private String headerName;
    private LocalDateTime lastUpdatedTime;
    private Version schemaVersion;

    /**
     * Enumeration for Rate Limit Types.
     */
    public enum RateLimitType {
        CLIENT_IP,
        HEADER,
        ROUTE_PATH,
        ROUTE_NAME
    }

    @Override
    public Version getSchemaVersion() {
        return schemaVersion;
    }

    @Override
    public void setSchemaVersion(Version version) {
        this.schemaVersion = version;
    }

    @Override
    public LocalDateTime getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    @Override
    public void setLastUpdatedTime(LocalDateTime lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }
}
