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

import org.eclipse.ecsp.nosqldao.IgniteQuery;
import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiRouteMongoRepositoryTest {

    @Test
    void findAllByServiceQueriesInsteadOfLoadingAllRoutes() {
        ApiRouteMongoDaoImpl dao = mock(ApiRouteMongoDaoImpl.class);
        ApiRouteEntity entity = new ApiRouteEntity();
        when(dao.find(any(IgniteQuery.class))).thenReturn(List.of(entity));

        final List<ApiRouteEntity> result = new ApiRouteMongoRepository(dao).findAllByService("demo-service");

        ArgumentCaptor<IgniteQuery> query = ArgumentCaptor.forClass(IgniteQuery.class);
        verify(dao).find(query.capture());
        verify(dao, never()).findAll();
        assertEquals(1, query.getValue().getPageNumber());
        assertEquals(Integer.MAX_VALUE, query.getValue().getPageSize());
        assertEquals(List.of(entity), result);
    }
}
