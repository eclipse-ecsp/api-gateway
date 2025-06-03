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
import org.springframework.stereotype.Indexed;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to define filters that are applied to HTTP requests
 * before they reach the target method. It supports specifying a filter name, arguments,
 * and key-value argument pairs.
 *
 * <ul>
 *   <li><b>value</b>: The name of the filter (alias for name).</li>
 *   <li><b>name</b>: The name of the filter (alias for value).</li>
 *   <li><b>args</b>: An array of arguments to pass arguments to the filter.</li>
 * </ul>
 *
 * <p>
 * This annotation is repeatable and can be used multiple times on the same method.
 * </p>
 *
 * @author Abhishek Kumar
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
@Repeatable(CustomGatewayFilters.class)
public @interface CustomGatewayFilter {
    /**
     * The name of the filter.
     * Alias for {@link #name()}.
     *
     * @return the filter name
     */
    String value() default "";

    /**
     * The name of the filter.
     * Alias for {@link #value()}.
     *
     * @return the filter name
     */
    @AliasFor("value")
    String name() default "";

    /**
     * Arguments to be passed to the filter.
     *
     * <p>formatted as key=value pairs.
     *
     * @return array of filter arguments
     */
    String[] args() default {};
}
