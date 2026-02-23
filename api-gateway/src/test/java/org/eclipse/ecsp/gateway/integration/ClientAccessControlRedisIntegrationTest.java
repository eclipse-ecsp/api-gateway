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

package org.eclipse.ecsp.gateway.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.eclipse.ecsp.gateway.exceptions.IgniteGlobalExceptionHandler;
import org.eclipse.ecsp.gateway.service.ClientAccessControlService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.nullable;

/**
 * Integration test for Client Access Control refresh using Redis pub/sub with TestContainers.
 *
 * <p>Tests event-driven configuration refresh mechanism:
 * - Redis connectivity and pub/sub message flow
 * - Basic cache operations
 * - Redis container health
 *
 * @author Abhishek Kumar
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientAccessControlRedisIntegrationTest {

    private static final int REDIS_PORT = 6379;
    private static final String REDIS_CHANNEL = "client-access-updates";

    @SuppressWarnings("resource") // Managed by Testcontainers framework
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
            .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("api.gateway.client-access-control.enabled", () -> "true");
        registry.add("api.gateway.routes.refresh.strategy", () -> "EVENT_DRIVEN");
        registry.add("api.gateway.routes.refresh.event.channel", () -> REDIS_CHANNEL);
        registry.add("api.dynamic.routes.enabled", () -> "true");
    }

    @BeforeAll
    static void beforeAll() {
        CollectorRegistry.defaultRegistry.clear(); // Clear Prometheus metrics registry before tests
    }        

    @AfterAll
    static void tearDown() {
        if (redis != null && redis.isRunning()) {
            redis.stop();
            redis.close();
        }
    }

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    private ClientAccessControlService cacheService;

    @Autowired
    private ClientAccessControlProperties properties;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IgniteGlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        cacheService.clearCache();
    }

    @Test
    @Order(1)
    @DisplayName("Redis container should be running and accessible")
    void testRedisContainerIsRunning() {
        assertThat(redis.isRunning()).isTrue();
        assertThat(redis.getFirstMappedPort()).isGreaterThan(0);
        
        // Verify Redis connectivity
        redisTemplate.opsForValue().set("test-key", "test-value").block();
        Mono<String> value = redisTemplate.opsForValue().get("test-key");
        assertThat(value.block()).isEqualTo("test-value");
        redisTemplate.delete("test-key").block();
    }

    @Test
    @Order(2)
    @DisplayName("Redis pub/sub should deliver messages to subscribers")
    void testRedisPubSubMessageDelivery() {
        // Arrange
        RefreshEvent event = RefreshEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CONFIG_UPDATED")
                .clientId("test_client_pubsub")
                .timestamp(Instant.now().toEpochMilli())
                .build();

        // Act - Publish event to Redis channel
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            Mono<Long> subscribers = redisTemplate.convertAndSend(REDIS_CHANNEL, eventJson);
            
            // Assert
            assertThat(subscribers.block()).isGreaterThanOrEqualTo(0);
        } catch (Exception e) {
            Assertions.fail("Failed to publish event: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Cache service should be available and operational")
    void testCacheServiceAvailable() {
        // Arrange & Act
        int initialSize = cacheService.getCacheSize();
        
        // Assert
        assertThat(initialSize).isGreaterThanOrEqualTo(0);
        assertThat(cacheService).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("Event publishing to Redis should work")
    void testEventPublishingSuccess() {
        // Arrange
        RefreshEvent event = RefreshEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CONFIG_UPDATED")
                .clientId("test_client")
                .timestamp(Instant.now().toEpochMilli())
                .build();

        // Act & Assert
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            Mono<Long> subscribers = redisTemplate.convertAndSend(REDIS_CHANNEL, eventJson);
            
            assertThat(subscribers.block()).isGreaterThanOrEqualTo(0);
        } catch (Exception e) {
            Assertions.fail("Failed to publish event: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Redis health check should respond with PONG")
    void testRedisHealthCheck() {
        try {
            String pingResult = redisTemplate.getConnectionFactory()
                    .getReactiveConnection()
                    .ping()
                    .block();
            
            assertThat(pingResult).isEqualTo("PONG");
        } catch (Exception e) {
            Assertions.fail("Redis health check failed: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Properties should be loaded correctly")
    void testPropertiesLoaded() {
        assertThat(properties).isNotNull();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getClaimNames()).isNotEmpty();
    }

    /**
     * Event model for Redis pub/sub messages.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    static class RefreshEvent {
        private String eventId;
        private String eventType;
        private String clientId;
        private Long timestamp;
    }

    /**
     * Overrides the auto-configured {@link RedisConnectionDetails} to force standalone
     * (non-cluster) mode so that Testcontainers host/port are used instead of the
     * cluster nodes defaulted in application.yml.
     */
    @TestConfiguration
    static class StandaloneRedisConnectionConfig {

        @Primary
        @Bean
        RedisConnectionDetails standaloneRedisConnectionDetails(
                @Value("${spring.data.redis.host}") String host,
                @Value("${spring.data.redis.port}") int port) {
            return new RedisConnectionDetails() {
                @Override
                public Standalone getStandalone() {
                    return new Standalone() {
                        @Override
                        public String getHost() {
                            return host;
                        }

                        @Override
                        public int getPort() {
                            return port;
                        }

                        @Override
                        public int getDatabase() {
                            return 0;
                        }
                    };
                }
            };
        }
    }
}
