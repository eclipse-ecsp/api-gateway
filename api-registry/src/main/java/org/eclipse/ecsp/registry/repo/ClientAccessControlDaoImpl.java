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

package org.eclipse.ecsp.registry.repo;

import org.eclipse.ecsp.nosqldao.mongodb.IgniteBaseDAOMongoImpl;
import org.eclipse.ecsp.registry.condition.ConditionalOnNoSqlDatabase;
import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.springframework.stereotype.Repository;

/**
 * MongoDB repository for GatewayClientAccessControl entity.
 *
 * <p>Provides CRUD operations and custom queries for client access control management.
 * Soft delete pattern: is_deleted flag instead of physical DELETE.
 */
@ConditionalOnNoSqlDatabase
@Repository
public class ClientAccessControlDaoImpl extends IgniteBaseDAOMongoImpl<String, ClientAccessControlEntity> {
    /**
     * Default constructor.
     */
    public ClientAccessControlDaoImpl() {
        // Default constructor
    }
}
