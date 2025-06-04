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

package org.eclipse.ecsp.annotations;

import org.springframework.core.annotation.AliasFor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation to apply multiple {@link CustomGatewayFilter} annotations to a method.
 *
 * <p>This can be used to specify an array of request filters for a single method.
 * </p>
 *
 * <pre>
 * &#64;CustomGatewayFilters({
 *     &#64;CustomGatewayFilter(...),
 *     &#64;CustomGatewayFilter(...)
 * })
 * </pre>
 *
 * @see CustomGatewayFilter
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustomGatewayFilters {
    /**
     * Alias for {@link #filters()}.
     *
     * @return an array of {@link CustomGatewayFilter} annotations
     */
    CustomGatewayFilter[] value() default {};

    /**
     * Alias for {@link #value()}.
     *
     * @return an array of {@link CustomGatewayFilter} annotations
     */
    @AliasFor("value")
    CustomGatewayFilter[] filters() default {};
}
