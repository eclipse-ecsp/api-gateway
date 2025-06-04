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

package org.eclipse.ecsp.security;

import io.swagger.v3.oas.models.Operation;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import java.util.HashMap;
import java.util.Map;

/**
 * CachingTagger is a Spring component that customizes OpenAPI operations.
 * It adds caching metadata to the operation based on the @CacheData annotation.
 */
@Component
public class CachingTagger implements OperationCustomizer {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(CachingTagger.class);
    /**
     * The extension key used to store caching metadata in the OpenAPI operation.
     */
    public static final String CACHE_EXTENSION = "x-cache-filter";

    /**
     * Customizes the given OpenAPI operation by adding caching metadata.
     *
     * @param operation     the OpenAPI operation to customize
     * @param handlerMethod the handler method associated with the operation
     * @return the customized operation with caching metadata
     */
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        LOGGER.info("inside the CachingTagger....");

        // Retrieve the CacheData annotation from the handler method
        CacheData data = handlerMethod.getMethodAnnotation(CacheData.class);

        // If the CacheData annotation is present, add its values to the cacheMap
        if (data != null) {
            // Create a map to hold caching metadata
            Map<String, Object> cacheMap = new HashMap<>();
            cacheMap.put("cacheKey", data.key());
            cacheMap.put("cacheTll", data.ttl());
            cacheMap.put("cacheSize", data.size());
            // Set the caching metadata as extensions on the operation
            operation.addExtension(CACHE_EXTENSION, cacheMap);
        }
        return operation;
    }
}