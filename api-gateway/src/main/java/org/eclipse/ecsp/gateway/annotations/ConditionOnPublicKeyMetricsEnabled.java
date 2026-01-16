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

package org.eclipse.ecsp.gateway.annotations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Conditional annotation that indicates a component should only be registered.
 * when the 'api.gateway.metrics.public-key.enabled' property is set to 'true'.
 *
 * <p>This annotation is used to conditionally enable components related to public key metrics
 * in the API Gateway. When the property is set to 'true' or not specified (default is 'true'),
 * the annotated component will be registered in the application context.
 * 
 * <p>Can be applied to types (classes) and methods that should only be active when public key metrics
 * are enabled in the API Gateway configuration.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(name = "api.gateway.metrics.public-key-cache.enabled",
        havingValue = "true", matchIfMissing = true)
public @interface ConditionOnPublicKeyMetricsEnabled {
}
