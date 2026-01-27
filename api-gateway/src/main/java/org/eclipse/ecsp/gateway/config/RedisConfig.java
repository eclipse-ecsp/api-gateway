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

package org.eclipse.ecsp.gateway.config;

import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.NoArgsConstructor;
import org.eclipse.ecsp.gateway.annotations.ConditionOnRedisEnabled;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import java.time.Duration;

/**
 * Configuration class for Redis caching.
 *
 * @author Abhishek Kumar
 */
@Configuration
@ConditionOnRedisEnabled
@NoArgsConstructor
@Import({RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class})
public class RedisConfig {

    /**
     * Customizes the Lettuce client configuration for Redis cluster.
     *
     * @return a LettuceClientConfigurationBuilderCustomizer that configures cluster topology refresh options
     */
    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer() {
        return clientConfigurationBuilder -> {
            ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                    .enablePeriodicRefresh(Duration.ofMinutes(1))
                    .enableAllAdaptiveRefreshTriggers()
                    .build();

            ClusterClientOptions clientOptions = ClusterClientOptions.builder()
                    .topologyRefreshOptions(topologyRefreshOptions)
                    .validateClusterNodeMembership(false)
                    .build();

            clientConfigurationBuilder.clientOptions(clientOptions);
        };
    }
}
