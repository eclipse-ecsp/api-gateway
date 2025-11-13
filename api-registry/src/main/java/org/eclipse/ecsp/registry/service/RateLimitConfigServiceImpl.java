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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.registry.dto.RateLimitConfigDto;
import org.eclipse.ecsp.registry.entity.RateLimitConfigEntity;
import org.eclipse.ecsp.registry.repo.RateLimitConfigRepository;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementation for managing rate limit configurations.
 *
 * @author Abhishek Kumar
 */
@Service
public class RateLimitConfigServiceImpl implements RateLimitConfigService {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RateLimitConfigServiceImpl.class);

    private final RateLimitConfigRepository rateLimitConfigRepository;  

    /**
     * Constructor to initialize RateLimitConfigServiceImpl.
     *
     * @param rateLimitConfigRepository the RateLimitConfigRepository
     */
    public RateLimitConfigServiceImpl(RateLimitConfigRepository rateLimitConfigRepository) {
        this.rateLimitConfigRepository = rateLimitConfigRepository;
    }

    @Override
    public List<RateLimitConfigDto> addOrUpdateRateLimitConfigs(List<RateLimitConfigDto> config) {
        LOGGER.info("Adding or updating {} rate limit configurations.", config.size());
        // validate config
        config.forEach(this::validateConfig);

        // check for duplicate services
        validateDuplicateServices(config);

        // check for duplicate routeIds
        validateDuplicateRoutes(config);

        List<RateLimitConfigEntity> entities = config.stream()
            .map(this::convertToEntity)
            .collect(Collectors.toList());

        // save all configs
        List<RateLimitConfigDto> updatedDtos = rateLimitConfigRepository.saveAll(entities).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        LOGGER.info("Added or updated {} rate limit configurations.", updatedDtos.size());
        return updatedDtos;
    }


    @Override
    public RateLimitConfigDto updateRateLimitConfig(String id, RateLimitConfigDto config) {
        LOGGER.info("Updating rate limit configuration for id: {}", id);
        validateConfig(config);

        // fetch existing config, id can be either id or service
        if (StringUtils.isAllBlank(id)) {
            LOGGER.error("Id is blank for update operation.");
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Id cannot be blank for update operation."
            );
        }

        Optional<RateLimitConfigEntity> existingEntity = rateLimitConfigRepository.findById(id);

        if (existingEntity.isEmpty()) {
            LOGGER.error("Rate limit configuration not found for id: {}", id);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Rate limit configuration not found for id: " + id
            );
        }

        // update entity
        RateLimitConfigEntity entityToUpdate = existingEntity.get();
        String updatedRouteId = StringUtils.defaultIfBlank(config.getRouteId(), entityToUpdate.getRouteId());
        String updatedService = StringUtils.defaultIfBlank(config.getService(), entityToUpdate.getService());

        if (StringUtils.isNotBlank(updatedRouteId)) {
            entityToUpdate.setId(updatedRouteId);
            entityToUpdate.setRouteId(updatedRouteId);
            entityToUpdate.setService(null);
        } else {
            entityToUpdate.setId(updatedService);
            entityToUpdate.setService(updatedService);
            entityToUpdate.setRouteId(null);
        }
        entityToUpdate.setReplenishRate(config.getReplenishRate());
        entityToUpdate.setBurstCapacity(config.getBurstCapacity());
        entityToUpdate.setIncludeHeaders(config.isIncludeHeaders());
        entityToUpdate.setKeyResolver(config.getKeyResolver());
        entityToUpdate.setArgs(config.getArgs());
        entityToUpdate.setRequestedTokens(config.getRequestedTokens());
        entityToUpdate.setDenyEmptyKey(config.getDenyEmptyKey());
        entityToUpdate.setEmptyKeyStatus(config.getEmptyKeyStatus());
        RateLimitConfigEntity updatedEntity = rateLimitConfigRepository.save(entityToUpdate);

        LOGGER.info("Rate limit configuration updated for id: {}", id);
        return convertToDto(updatedEntity);
    }

    @Override
    public List<RateLimitConfigDto> getRateLimitConfigs() {
        LOGGER.info("Retrieving all rate limit configurations.");
        List<RateLimitConfigDto> configs = rateLimitConfigRepository.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        LOGGER.info("Retrieved {} rate limit configurations.", configs.size());
        return configs.isEmpty() ? List.of() : configs;
    }

    @Override
    public void deleteRateLimitConfig(String id) {
        LOGGER.info("Deleting rate limit configuration for id: {}", id);
        Optional<RateLimitConfigEntity> entity = rateLimitConfigRepository.findById(id);
        if (entity.isEmpty()) {
            LOGGER.warn("Rate limit configuration not found for id: {}", id);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Rate limit configuration not found for id: " + id
            );
        }

        rateLimitConfigRepository.delete(entity.get());
        LOGGER.info("Rate limit configuration deleted for id: {}", id);
    }

    /**
     * Validate the rate limit configuration.
     *
     * @param config RateLimitConfigDto to validate
     * @return true if valid, else throws exception
     */
    private boolean validateConfig(RateLimitConfigDto config) {
        // validate if routeId or service is should be present not both
        if (StringUtils.isNotBlank(config.getRouteId())
            && StringUtils.isNotBlank(config.getService())) {
            LOGGER.error("Both routeId and service are present in the config: {}", config);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Either routeId or service should be present, not both."
            );
        }

        // validate if either routeId or service is present
        if (StringUtils.isBlank(config.getRouteId())
            && StringUtils.isBlank(config.getService())) {
            LOGGER.error("Both routeId and service are missing in the config: {}", config);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Either routeId or service should be present"
            );
        }

        // validate if the replenish rate and burst capacity are positive integers

        if (config.getReplenishRate() <= 0 || config.getBurstCapacity() <= 0) {
            LOGGER.error("Invalid replenish rate or burst capacity in the config: {}, {}, "
                + "should be positive integers", config,
                "ReplenishRate: " + config.getReplenishRate()
                + ", BurstCapacity: " + config.getBurstCapacity());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Replenish rate and burst capacity must be positive integers."
            );
        }
        // validate the burst capacity is greater than or equal to replenish rate
        if (config.getBurstCapacity() < config.getReplenishRate()) {
            LOGGER.error("Burst capacity is less than replenish rate in the config: {}, {}, "
                + "burst capacity should be greater than or equal to replenish rate", config,
                "ReplenishRate: " + config.getReplenishRate()
                + ", BurstCapacity: " + config.getBurstCapacity());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Burst capacity must be greater than or equal to replenish rate."
            );
        }       
        return true;
    }

    /**
     * Convert RateLimitConfigEntity to RateLimitConfigDto.
     *
     * @param entity RateLimitConfigEntity to convert
     * @return Converted RateLimitConfigDto
     */
    private RateLimitConfigDto convertToDto(RateLimitConfigEntity entity) {
        RateLimitConfigDto dto = new RateLimitConfigDto();

        if (entity.getRouteId() != null) {
            dto.setRouteId(entity.getRouteId());
        } else {
            dto.setService(entity.getService());
        }
        dto.setReplenishRate(entity.getReplenishRate());
        dto.setBurstCapacity(entity.getBurstCapacity());
        dto.setIncludeHeaders(entity.isIncludeHeaders());
        dto.setKeyResolver(entity.getKeyResolver());
        dto.setArgs(entity.getArgs());
        dto.setRequestedTokens(entity.getRequestedTokens());
        dto.setDenyEmptyKey(entity.getDenyEmptyKey());
        dto.setEmptyKeyStatus(entity.getEmptyKeyStatus());
        return dto;
    }

    /**
     * Validate for duplicate routeIds in the provided configurations.
     *
     * @param config List of RateLimitConfigDto to validate
     */
    private void validateDuplicateRoutes(List<RateLimitConfigDto> config) {
        // validate for duplicate routeIds
        List<String> routeIds = config.stream()
            .map(RateLimitConfigDto::getRouteId)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.groupingBy(r -> r, Collectors.counting()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 1)
            .map(e -> e.getKey())
            .collect(Collectors.toList());

        if (!routeIds.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Duplicate routeId entries found: " + String.join(", ", routeIds)
            );
        }
    }

    /**
     * Validate for duplicate services in the provided configurations.
     *
     * @param config List of RateLimitConfigDto to validate
     */
    private void validateDuplicateServices(List<RateLimitConfigDto> config) {
        // check for duplicate service entries
        List<String> services = config.stream()
            .map(RateLimitConfigDto::getService)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 1)
            .map(e -> e.getKey())
            .collect(Collectors.toList());
        
        if (!services.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Duplicate service entries found: " + String.join(", ", services)
            );
        }
    }

    /**
     * Convert RateLimitConfigDto to RateLimitConfigEntity.
     *
     * @param dto RateLimitConfigDto to convert
     * @return Converted RateLimitConfigEntity
     */
    private RateLimitConfigEntity convertToEntity(RateLimitConfigDto dto) {
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        if (dto.getRouteId() != null) {
            entity.setId(dto.getRouteId());
            entity.setRouteId(dto.getRouteId());
        } else {
            entity.setId(dto.getService());
            entity.setService(dto.getService());
        }
        entity.setReplenishRate(dto.getReplenishRate());
        entity.setBurstCapacity(dto.getBurstCapacity());
        entity.setIncludeHeaders(dto.isIncludeHeaders());
        entity.setKeyResolver(dto.getKeyResolver());
        entity.setArgs(dto.getArgs());
        entity.setRequestedTokens(dto.getRequestedTokens());
        entity.setDenyEmptyKey(dto.getDenyEmptyKey());
        entity.setEmptyKeyStatus(dto.getEmptyKeyStatus());
        return entity;
    }

}
