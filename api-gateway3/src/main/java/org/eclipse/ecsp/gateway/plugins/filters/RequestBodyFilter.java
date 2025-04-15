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


package org.eclipse.ecsp.gateway.plugins.filters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.ObjectMapperUtil;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.openapi4j.core.validation.ValidationResults.ValidationItem;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.stream.Collectors;

/**
 * Request body filter to validate request body with route schema.
 */
public class RequestBodyFilter implements GatewayFilter, Ordered {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RequestBodyFilter.class);
    @Value("${requestBody.validation}")
    public boolean requestBodyValidation;

    /**
     * Constructor to initialize RequestBodyValidator.
     *
     * @param config RequestBodyValidator.Config
     */
    public RequestBodyFilter(Config config) {
    }

    /**
     * filter method to validate the request body.
     *
     * @param exchange ServerWebExchange
     * @param chain    GatewayFilterChain
     * @return {@link Mono} of {@link Void}.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        validate(exchange);
        return chain.filter(exchange);
    }

    private void validate(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            throw new ApiGatewayException(HttpStatus.NOT_FOUND, "api.gateway.error", "Request not found");
        }
        // get schema validator
        // no validation required if there is no schema defined
        if (route.getMetadata() == null) {
            return;
        }
        // Validate if schema validator defined
        Object validatorObj = route.getMetadata().get(GatewayConstants.SCHEMA_VALIDATOR);
        if (validatorObj != null) {
            String body = exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
            // Check if the body is null or empty
            if (StringUtils.isBlank(body)) {
                LOGGER.error("Invalid request body missing from request: {}", exchange.getRequest().getPath());
                throw new ApiGatewayException(HttpStatus.BAD_REQUEST,
                        "api.gateway.error.request",
                        "Invalid request payload");
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Request Body : {}", body);
            }
            if (requestBodyValidation) {
                JsonNode contentNode;
                try {
                    contentNode = ObjectMapperUtil.getObjectMapper().readTree(body);
                } catch (Exception e) {
                    LOGGER.error("Invalid request body validation failed with error {} for request: {}",
                            e,
                            exchange.getRequest().getPath());
                    throw new ApiGatewayException(HttpStatus.BAD_REQUEST,
                            "api.gateway.error.request",
                            "Invalid request payload");
                }
                SchemaValidator schemaValidator = (SchemaValidator) validatorObj;
                ValidationData<Void> validation = new ValidationData<>();
                schemaValidator.validate(contentNode, validation);
                if (!validation.isValid()) {
                    LOGGER.warn("Request body validation failed: {}", validation.results());
                    throw new ApiGatewayException(HttpStatus.BAD_REQUEST,
                            "api.gateway.error.request.validation",
                            "Validation failed : " + validation.results()
                                    .items().stream()
                                    .map(ValidationItem::message)
                                    .collect(Collectors.joining(", ")));
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return GatewayConstants.REQUEST_BODY_VALIDATOR_FILTER_ORDER;
    }

    /**
     * Confing class to pass configurations to filter.
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class Config {
    }
}
