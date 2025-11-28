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

import org.eclipse.ecsp.nosqldao.IgniteCriteria;
import org.eclipse.ecsp.nosqldao.IgniteCriteriaGroup;
import org.eclipse.ecsp.nosqldao.IgniteQuery;
import org.eclipse.ecsp.nosqldao.Operator;
import org.eclipse.ecsp.registry.condition.ConditionalOnNoSqlDatabase;
import org.eclipse.ecsp.registry.entity.RateLimitConfigEntity;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for Rate Limit Configuration using NoSQL database.
 */
@ConditionalOnNoSqlDatabase
@Repository("rateLimitConfigMongoRepository")
public class RateLimitConfigMongoRepository implements RateLimitConfigRepository {

    private final RateLimitConfigDaoImpl rateLimitConfigDao;
    
    /**
     * Constructor to initialize the RateLimitConfigDaoImpl.
     *
     * @param rateLimitConfigDao the RateLimitConfigDaoImpl
     */
    public RateLimitConfigMongoRepository(RateLimitConfigDaoImpl rateLimitConfigDao) {
        this.rateLimitConfigDao = rateLimitConfigDao;
    }


    @Override
    public Optional<RateLimitConfigEntity> findById(String id) {
        return Optional.ofNullable(rateLimitConfigDao.findById(id));
    }

    @Override
    public List<RateLimitConfigEntity> findAll() {
        return rateLimitConfigDao.findAll();
    }

    @Override
    public RateLimitConfigEntity save(RateLimitConfigEntity entity) {
        return rateLimitConfigDao.save(entity);
    }

    @Override
    public void delete(RateLimitConfigEntity entity) {
        rateLimitConfigDao.delete(entity);
    }

    @Override
    public List<RateLimitConfigEntity> saveAll(List<RateLimitConfigEntity> entities) {
        return rateLimitConfigDao.saveAll(entities.toArray(new RateLimitConfigEntity[0]));
    }

    @Override
    public Optional<RateLimitConfigEntity> findByService(String service) {

        IgniteCriteria criteria = new IgniteCriteria();
        criteria.field("service").op(Operator.EQ).val(service);

        IgniteCriteriaGroup criteriaGroup = new IgniteCriteriaGroup(criteria);

        IgniteQuery igniteQuery = new IgniteQuery(criteriaGroup);
        igniteQuery.setPageNumber(0);
        igniteQuery.setPageSize(1);

        List<RateLimitConfigEntity> result = rateLimitConfigDao.find(igniteQuery);
        
        return Optional.ofNullable(result.isEmpty() ? null : result.get(0));
    }

}
