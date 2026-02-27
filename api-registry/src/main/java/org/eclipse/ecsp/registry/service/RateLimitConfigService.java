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

package org.eclipse.ecsp.registry.service;

import org.eclipse.ecsp.registry.dto.RateLimitConfigDto;
import java.util.List;

/**
 * Service interface for managing rate limit configurations.
 *
 * @author Abhishek Kumar
 */
public interface RateLimitConfigService {
    /** 
     * Method to add or update rate limit configurations.
     *
     * @param config List of RateLimitConfigDto objects to be added or updated
     * @return List of added or updated RateLimitConfigDto objects
     */
    List<RateLimitConfigDto> addOrUpdateRateLimitConfigs(List<RateLimitConfigDto> config);
    
    /**
     * Method to update a rate limit configuration.
     *
     * @param id ID of the rate limit configuration to be updated
     * @param config RateLimitConfigDto object to be updated
     * @return Updated RateLimitConfigDto object
     */
    RateLimitConfigDto updateRateLimitConfig(String id, RateLimitConfigDto config);
    
    /**
     * Method to retrieve all rate limit configurations.
     *
     * @return List of RateLimitConfigDto objects
     */
    List<RateLimitConfigDto> getRateLimitConfigs();
    
    /**
     * Method to delete a rate limit configuration by ID.
     *
     * @param id ID of the rate limit configuration to be deleted
     */
    void deleteRateLimitConfig(String id);
}
