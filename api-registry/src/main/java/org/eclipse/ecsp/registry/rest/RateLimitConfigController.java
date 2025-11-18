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

package org.eclipse.ecsp.registry.rest;

import org.eclipse.ecsp.registry.dto.GenericResponseDto;
import org.eclipse.ecsp.registry.dto.RateLimitConfigDto;
import org.eclipse.ecsp.registry.service.RateLimitConfigService;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * REST controller for managing rate limit configurations.
 *
 * @author Abhishek Kumar
 */
@RestController
@RequestMapping("/v1/config/rate-limits")
public class RateLimitConfigController {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RateLimitConfigController.class);

    private final RateLimitConfigService rateLimitConfigService;

    /**
     * Constructor to initialize RateLimitConfigController.
     *
     * @param rateLimitConfigService the RateLimitConfigService
     */
    public RateLimitConfigController(RateLimitConfigService rateLimitConfigService) {
        this.rateLimitConfigService = rateLimitConfigService;
    }

    /**
     * Get all rate limit configurations.
     *
     * @return List of RateLimitConfigDto
     */
    @GetMapping
    public List<RateLimitConfigDto> getRateLimitConfigs() {
        LOGGER.info("Fetching rate limit configurations");
        List<RateLimitConfigDto> rateLimitConfigs = rateLimitConfigService.getRateLimitConfigs();
        LOGGER.info("Fetched {} rate limit configurations", rateLimitConfigs.size());
        return rateLimitConfigs;
    }


    /**
     * Add or update rate limit configurations.
     *
     * @param configs List of RateLimitConfigDto to add or update
     * @return List of updated RateLimitConfigDto
     */
    @PostMapping
    public List<RateLimitConfigDto> addOrUpdateRateLimitConfigs(@RequestBody List<RateLimitConfigDto> configs) {
        LOGGER.info("Adding or updating {} rate limit configurations", configs.size());
        List<RateLimitConfigDto> updatedConfigs = rateLimitConfigService.addOrUpdateRateLimitConfigs(configs);
        LOGGER.info("Added or updated {} rate limit configurations", updatedConfigs.size());
        return updatedConfigs;
    }
    

    /**
     * Update a rate limit configuration.
     *
     * @param id ID of the rate limit configuration to update
     * @param rateLimitConfigDto RateLimitConfigDto with updated data
     * @return Updated RateLimitConfigDto
     */
    @PutMapping("/{id}")
    public RateLimitConfigDto updateRateLimitConfig(@PathVariable String id,
            @RequestBody RateLimitConfigDto rateLimitConfigDto) {
        LOGGER.info("Updating rate limit configuration with id: {}", id);
        RateLimitConfigDto updatedConfig = rateLimitConfigService.updateRateLimitConfig(id, rateLimitConfigDto);
        LOGGER.info("Updated rate limit configuration: {}", updatedConfig);
        return updatedConfig;
    }
    

    /**
     * Delete a rate limit configuration by ID.
     *
     * @param id ID of the rate limit configuration to delete
     * @return ResponseEntity with deletion status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto> deleteRateLimitConfig(@PathVariable String id) {
        LOGGER.info("Deleting rate limit configuration with id: {}", id);
        rateLimitConfigService.deleteRateLimitConfig(id);
        LOGGER.info("Deleted rate limit configuration with id: {}", id);
        GenericResponseDto response = new GenericResponseDto();
        response.setMessage("Rate limit configuration deleted successfully");
        return ResponseEntity.ok(response);
    }
    
}