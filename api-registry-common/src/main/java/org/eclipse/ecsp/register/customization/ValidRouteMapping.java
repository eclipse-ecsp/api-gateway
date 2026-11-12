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

package org.eclipse.ecsp.register.customization;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a {@link RouteCustomizerProperties.RouteMapping}: exactly one of {@code routeId} or
 * {@code path} must be set, {@code method} is only meaningful together with {@code path}, and
 * the mapping must configure at least one {@code exposeAs} target or {@code filter} (a mapping
 * with neither is a no-op configuration mistake, not a valid customization).
 */
@Documented
@Constraint(validatedBy = RouteMappingValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRouteMapping {

    /**
     * Validation failure message.
     *
     * @return the message template
     */
    String message() default "invalid route-customizer mapping";

    /**
     * Validation groups.
     *
     * @return group classes
     */
    Class<?>[] groups() default {};

    /**
     * Validation payload.
     *
     * @return payload classes
     */
    Class<? extends Payload>[] payload() default {};
}
