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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.register.customization;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

/**
 * Cross-field validation for {@link RouteCustomizerProperties.RouteMapping}.
 */
public class RouteMappingValidator
        implements ConstraintValidator<ValidRouteMapping, RouteCustomizerProperties.RouteMapping> {

    @Override
    public boolean isValid(RouteCustomizerProperties.RouteMapping mapping, ConstraintValidatorContext context) {
        if (mapping == null) {
            return true;
        }
        boolean hasRouteId = StringUtils.hasText(mapping.getRouteId());
        boolean hasPath = StringUtils.hasText(mapping.getPath());
        if (hasRouteId == hasPath) {
            return violation(context, "exactly one of route-id or path must be set");
        }
        if (!hasPath && StringUtils.hasText(mapping.getMethod())) {
            return violation(context, "method is only meaningful together with path");
        }
        if (mapping.getFilters() == null || mapping.getFilters().isEmpty()) {
            return violation(context, "mapping must configure at least one filter");
        }
        return true;
    }

    private boolean violation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
