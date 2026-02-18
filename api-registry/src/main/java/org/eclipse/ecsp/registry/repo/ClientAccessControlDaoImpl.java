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
}
