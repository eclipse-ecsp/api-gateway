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
import org.apache.commons.lang3.Strings;
import org.eclipse.ecsp.registry.config.RateLimitProperties;
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
    
    private final RateLimitProperties rateLimitProperties;

    /**
     * Constructor to initialize RateLimitConfigServiceImpl.
     *
     * @param rateLimitConfigRepository the RateLimitConfigRepository
     */
    public RateLimitConfigServiceImpl(RateLimitConfigRepository rateLimitConfigRepository,
            RateLimitProperties rateLimitProperties) {
        this.rateLimitConfigRepository = rateLimitConfigRepository;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    public List<RateLimitConfigDto> addOrUpdateRateLimitConfigs(List<RateLimitConfigDto> config) {
        LOGGER.info("Adding or updating {} rate limit configurations.", config.size());

        if (config.isEmpty()) {
            LOGGER.warn("No rate limit configurations provided to add or update.");
            return List.of();
        }

        // validate config
        config.forEach(this::validateConfig);

        // check for duplicate services
        validateDuplicateServices(config);

        // check for duplicate routeIds
        validateDuplicateRoutes(config);

        // Log the difference between existing (what is changed) and new configs (whats added)
        LOGGER.info("Comparing existing and new rate limit configurations for changes.");
        List<RateLimitConfigEntity> existingConfigs = rateLimitConfigRepository.findAll();
        for (RateLimitConfigDto newConfig : config) {
            Optional<RateLimitConfigEntity> existingConfigOpt = existingConfigs.stream()
                .filter(e -> {
                    if (StringUtils.isNotBlank(newConfig.getRouteId())) {
                        return Strings.CS.equals(e.getRouteId(), newConfig.getRouteId());
                    } else {
                        return Strings.CS.equals(e.getService(), newConfig.getService());
                    }
                }).findFirst();
            if (existingConfigOpt.isPresent()) {
                RateLimitConfigEntity existingConfig = existingConfigOpt.get();
                LOGGER.info("Updating existing rate limit configuration: {} to new configuration: {}",
                    existingConfig, newConfig);
            } else {
                LOGGER.info("Adding new rate limit configuration: {}", newConfig);
            }
        }

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
        String identifier = getConfigIdentifier(config);
        
        validateRouteOrService(config, identifier);
        validateLimitConfig(config);
        validateKeyResolver(config, identifier);
        validateRequestedTokens(config, identifier);
        validateHeaderKeyResolverArgs(config, identifier);
        validateEmptyKeyStatus(config, identifier);

        return true;
    }

    /**
     * Validate that either routeId or service is present, but not both.
     *
     * @param config RateLimitConfigDto to validate
     * @param identifier Config identifier for error messages
     */
    private void validateRouteOrService(RateLimitConfigDto config, String identifier) {
        // validate if routeId or service is should be present not both
        if (StringUtils.isNotBlank(config.getRouteId())
            && StringUtils.isNotBlank(config.getService())) {
            LOGGER.error("Both routeId and service are present in the config: {}", config);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format("Either routeId or service should be present, not both for %s.", identifier)
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
    }

    /**
     * Validate key resolver is present and is in the allowed list.
     *
     * @param config RateLimitConfigDto to validate
     * @param identifier Config identifier for error messages
     */
    private void validateKeyResolver(RateLimitConfigDto config, String identifier) {
        // validate key resolver is present
        if (StringUtils.isBlank(config.getKeyResolver())) {
            LOGGER.error("Key resolver is missing in the config: {}", config);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format("Key resolver must be specified for %s.", identifier)
            );
        }

        // validate key resolver is among the allowed ones
        if (!rateLimitProperties.getKeyResolvers().contains(config.getKeyResolver())) {
            LOGGER.error("Invalid key resolver in the config: {}, allowed key resolvers: {}", config,
                rateLimitProperties.getKeyResolvers());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format("Invalid key resolver for %s. Allowed key resolvers are: %s.",
                    identifier, String.join(", ", rateLimitProperties.getKeyResolvers()))
            );
        }
    }

    /**
     * Validate requested tokens is within valid range.
     *
     * @param config RateLimitConfigDto to validate
     * @param identifier Config identifier for error messages
     */
    private void validateRequestedTokens(RateLimitConfigDto config, String identifier) {
        if (config.getRequestedTokens() <= 0
            || config.getRequestedTokens() > rateLimitProperties.getMaxRequestedTokens()) {
            LOGGER.error("Invalid requested tokens in the config: {}, should be positive and "
                + "less than max requested tokens: {}", config,
                rateLimitProperties.getMaxRequestedTokens());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format("Requested tokens must be positive and less than %d for %s.",
                    rateLimitProperties.getMaxRequestedTokens(), identifier)
            );
        }
    }

    /**
     * Validate header key resolver has required args.
     *
     * @param config RateLimitConfigDto to validate
     * @param identifier Config identifier for error messages
     */
    private void validateHeaderKeyResolverArgs(RateLimitConfigDto config, String identifier) {
        // validate if the key resolver is header and args contains header name
        if (config.getKeyResolver().equalsIgnoreCase("header") 
            || config.getKeyResolver().equals("headerKeyResolver")) {
            if (config.getArgs() == null || !config.getArgs().containsKey("headerName")
                || StringUtils.isBlank(config.getArgs().get("headerName"))) {
                LOGGER.error("Header name is missing in args for header key resolver in the config: {}", config);
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Header name must be specified in args for header key resolver for %s.",
                        identifier)
                );
            }
        }
    }

    /**
     * Validate empty key status is a valid HTTP status code.
     *
     * @param config RateLimitConfigDto to validate
     * @param identifier Config identifier for error messages
     */
    private void validateEmptyKeyStatus(RateLimitConfigDto config, String identifier) {
        // validate empty response code is matching with http status codes
        if (config.getDenyEmptyKey()) {
            try {
                HttpStatus.resolve(Integer.valueOf(config.getEmptyKeyStatus()));
            } catch (IllegalArgumentException e) {
                LOGGER.error("Invalid empty key status code in the config: {}," 
                    + " must be a valid HTTP status code", config);
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Empty key status must be a valid HTTP status code for %s.", identifier)
                );
            }
        }
    }

    private void validateLimitConfig(RateLimitConfigDto config) {
        String identifier = getConfigIdentifier(config);
        
        // validate if the replenish rate and burst capacity are positive integers
        // validate the replenish rate and burst capacity are positive integers
        if (config.getReplenishRate() <= 0 || config.getBurstCapacity() <= 0) {
            LOGGER.error("Invalid replenish rate or burst capacity in the config: {}, {}," 
                + "should be positive integers", config,
                "ReplenishRate: " + config.getReplenishRate()
                + ", BurstCapacity: " + config.getBurstCapacity());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format("Replenish rate and burst capacity must be positive integers for %s.", identifier)
            );
        }

        // validate the max replenish rate and burst capacity from properties
        if (config.getReplenishRate() > rateLimitProperties.getMaxReplenishRate()
            || config.getBurstCapacity() > rateLimitProperties.getMaxBurstCapacity()) {
            LOGGER.error("Replenish rate or burst capacity exceeds maximum limit in the config: {}, {}," 
                + "maxReplenishRate: {}, maxBurstCapacity: {}", config,
                "ReplenishRate: " + config.getReplenishRate()
                + ", BurstCapacity: " + config.getBurstCapacity(),
                rateLimitProperties.getMaxReplenishRate(), rateLimitProperties.getMaxBurstCapacity());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format("Replenish rate or burst capacity exceeds maximum limit (max: %d/%d) for %s.",
                    rateLimitProperties.getMaxReplenishRate(),
                    rateLimitProperties.getMaxBurstCapacity(), identifier)
            );
        }
       
        // validate the burst capacity is greater than or equal to replenish rate
        if (config.getBurstCapacity() < config.getReplenishRate()) {
            LOGGER.error("Burst capacity is less than replenish rate in the config: {}, {}," 
                + "burst capacity should be greater than or equal to replenish rate", config,
                "ReplenishRate: " + config.getReplenishRate()
                + ", BurstCapacity: " + config.getBurstCapacity());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format("Burst capacity (%d) must be greater than or equal to replenish rate (%d) for %s.",
                    config.getBurstCapacity(), config.getReplenishRate(), identifier)
            );
        }

        // validate requested tokens is less than or equal to burst capacity
        if (config.getRequestedTokens() > config.getBurstCapacity()) {
            LOGGER.error("Requested tokens exceeds burst capacity in the config: {}, {}," 
                + "requested tokens should be less than or equal to burst capacity", config,
                "RequestedTokens: " + config.getRequestedTokens()
                + ", BurstCapacity: " + config.getBurstCapacity());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format("Requested tokens (%d) must be less than or equal to burst capacity (%d) for %s.",
                    config.getRequestedTokens(), config.getBurstCapacity(), identifier)
            );
        }
    }

    /**
     * Get configuration identifier (routeId or service) for error messages.
     *
     * @param config RateLimitConfigDto
     * @return routeId or service name formatted as "routeId xxx" or "service xxx"
     */
    private String getConfigIdentifier(RateLimitConfigDto config) {
        if (StringUtils.isNotBlank(config.getRouteId())) {
            return "routeId " + config.getRouteId();
        } else if (StringUtils.isNotBlank(config.getService())) {
            return "service " + config.getService();
        }
        return "unknown config";
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
