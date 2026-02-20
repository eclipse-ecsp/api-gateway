package org.eclipse.ecsp.registry.repo;

import org.eclipse.ecsp.nosqldao.IgniteCriteria;
import org.eclipse.ecsp.nosqldao.IgniteCriteriaGroup;
import org.eclipse.ecsp.nosqldao.IgniteQuery;
import org.eclipse.ecsp.nosqldao.Operator;
import org.eclipse.ecsp.registry.condition.ConditionalOnNoSqlDatabase;
import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for ClientAccessControlEntity.
 *
 * <p>Provides CRUD operations and custom queries for client access control in MongoDB.
 * Soft delete pattern: is_deleted flag instead of physical DELETE.
 */
@Repository
@ConditionalOnNoSqlDatabase
public class ClientAccessControlMongoRepository implements ClientAccessControlRepository {

    private static final String IS_ACTIVE = "isActive";
    private static final String CLIENT_ID = "clientId";
    private static final String IS_DELETED = "isDeleted";
    private final ClientAccessControlDaoImpl clientAccessControlDao;

    /**
     * Constructor to initialize the ClientAccessControlDaoImpl.
     *
     * @param clientAccessControlDao the ClientAccessControlDaoImpl
     */
    public ClientAccessControlMongoRepository(ClientAccessControlDaoImpl clientAccessControlDao) {
        this.clientAccessControlDao = clientAccessControlDao;
    }

    @Override
    public Optional<ClientAccessControlEntity> findByClientIdAndIsDeletedFalse(String clientId) {
        IgniteCriteria clientIdCriteria = new IgniteCriteria(CLIENT_ID, Operator.EQ, clientId);
        IgniteCriteria isDeletedCriteria = new IgniteCriteria(IS_DELETED, Operator.EQ, false);

        IgniteCriteriaGroup criteriaGroup = new IgniteCriteriaGroup(clientIdCriteria).and(isDeletedCriteria);

        IgniteQuery igniteQuery = new IgniteQuery(criteriaGroup);
        igniteQuery.setPageNumber(1);
        igniteQuery.setPageSize(1);

        List<ClientAccessControlEntity> result = clientAccessControlDao.find(igniteQuery);
        
        return Optional.ofNullable(result.isEmpty() ? null : result.get(0));
    }

    @Override
    public List<ClientAccessControlEntity> findByIsActiveAndIsDeletedFalse(boolean isActive) {
        IgniteCriteria isActiveCriteria = new IgniteCriteria(IS_ACTIVE, Operator.EQ, isActive);

        IgniteCriteria isDeletedCriteria = new IgniteCriteria(IS_DELETED, Operator.EQ, false);

        IgniteCriteriaGroup criteriaGroup = new IgniteCriteriaGroup(isActiveCriteria).and(isDeletedCriteria);

        IgniteQuery igniteQuery = new IgniteQuery(criteriaGroup);
        igniteQuery.setPageNumber(1);
        igniteQuery.setPageSize(Integer.MAX_VALUE);

        return clientAccessControlDao.find(igniteQuery);
    }

    @Override
    public List<ClientAccessControlEntity> findByIsDeletedFalse() {
        IgniteCriteria isDeletedCriteria = new IgniteCriteria(IS_DELETED, Operator.EQ, false);

        IgniteCriteriaGroup criteriaGroup = new IgniteCriteriaGroup(isDeletedCriteria);

        IgniteQuery igniteQuery = new IgniteQuery(criteriaGroup);
        igniteQuery.setPageNumber(1);
        igniteQuery.setPageSize(Integer.MAX_VALUE);

        return clientAccessControlDao.find(igniteQuery);
    }

    @Override
    public boolean existsByClientIdAndIsDeletedFalse(String clientId) {
        IgniteCriteria clientIdCriteria = new IgniteCriteria(CLIENT_ID, Operator.EQ, clientId);
        IgniteCriteria isDeletedCriteria = new IgniteCriteria(IS_DELETED, Operator.EQ, false);

        IgniteCriteriaGroup criteriaGroup = new IgniteCriteriaGroup(clientIdCriteria).and(isDeletedCriteria);

        IgniteQuery igniteQuery = new IgniteQuery(criteriaGroup);
        igniteQuery.setPageNumber(1);
        igniteQuery.setPageSize(1);

        List<ClientAccessControlEntity> result = clientAccessControlDao.find(igniteQuery);
        
        return !result.isEmpty();
    }

    @Override
    public ClientAccessControlEntity save(ClientAccessControlEntity entity) {
        return clientAccessControlDao.save(entity);
    }

    @Override
    public List<ClientAccessControlEntity> saveAll(Iterable<ClientAccessControlEntity> entities) {
        List<ClientAccessControlEntity> entityList = new ArrayList<>();
        entities.forEach(entityList::add);
        return clientAccessControlDao.saveAll(entityList.toArray(new ClientAccessControlEntity[0]));
    }

    @Override
    public void delete(ClientAccessControlEntity entity) {
        clientAccessControlDao.delete(entity);
    }

    @Override
    public Optional<ClientAccessControlEntity> findByClientIdAndIsDeletedTrue(String clientId) {
        IgniteCriteria clientIdCriteria = new IgniteCriteria(CLIENT_ID, Operator.EQ, clientId);
        IgniteCriteria isDeletedCriteria = new IgniteCriteria(IS_DELETED, Operator.EQ, true);

        IgniteCriteriaGroup criteriaGroup = new IgniteCriteriaGroup(clientIdCriteria).and(isDeletedCriteria);

        IgniteQuery igniteQuery = new IgniteQuery(criteriaGroup);
        igniteQuery.setPageNumber(1);
        igniteQuery.setPageSize(1);

        List<ClientAccessControlEntity> result = clientAccessControlDao.find(igniteQuery);
        
        return Optional.ofNullable(result.isEmpty() ? null : result.get(0));
    }

    @Override
    public void deleteAll() {
        clientAccessControlDao.deleteAll();
    }

}

