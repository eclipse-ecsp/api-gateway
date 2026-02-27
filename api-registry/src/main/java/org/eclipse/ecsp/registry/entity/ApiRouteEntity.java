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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.ecsp.domain.Version;
import org.eclipse.ecsp.entities.IgniteEntity;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Defines the structure of ApiRouteEntity.
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@Entity(name = "api_routes")
@dev.morphia.annotations.Entity("api_routes")
public class ApiRouteEntity implements IgniteEntity {

    /**
     * api active status.
     */
    protected Boolean active;
    @Transient
    private Version schemaVersion;
    @Id
    @dev.morphia.annotations.Id
    private String id;
    private String service;

    @Column(name = "contextpath", nullable = true)
    private String contextPath;

    @Column(name = "apidocs")
    private Boolean apiDocs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private RouteDefinition route;

    @Override
    public Version getSchemaVersion() {
        return schemaVersion;
    }

    @Override
    public void setSchemaVersion(Version version) {
        this.schemaVersion = version;
    }
}