package org.eclipse.ecsp.gateway.health;

import org.eclipse.ecsp.gateway.config.RouteRefreshProperties;
import org.eclipse.ecsp.gateway.events.RouteRefreshFallbackScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteRefreshHealthIndicatorTest {

    @Mock
    private RouteRefreshFallbackScheduler fallbackScheduler;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private RouteRefreshProperties properties;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private RouteRefreshProperties.RedisConfig redisConfig;

    @InjectMocks
    private RouteRefreshHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getRedis()).thenReturn(redisConfig);
        lenient().when(redisConfig.getChannel()).thenReturn("test-channel");
        when(properties.getStrategy()).thenReturn(RouteRefreshProperties.RefreshStrategy.EVENT_DRIVEN);
    }

    @Test
    void testHealth_Up_RedisConnectedAndFallbackInactive() {
        // Arrange
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        when(fallbackScheduler.isFallbackActive()).thenReturn(false);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("mode", "EVENT_DRIVEN")
                .containsEntry("redisConnected", true)
                .containsEntry("channel", "test-channel");
    }

    @Test
    void testHealth_Degraded_FallbackActive() {
        // Arrange
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        when(fallbackScheduler.isFallbackActive()).thenReturn(true);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));
        assertThat(health.getDetails()).containsEntry("mode", "POLLING_FALLBACK");
    }

    @Test
    void testHealth_Degraded_RedisConnectionFailed() {
        // Arrange
        when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Connection failed"));
        when(fallbackScheduler.isFallbackActive()).thenReturn(true);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));
        assertThat(health.getDetails()).containsEntry("redisConnected", false);
    }

    @Test
    void testHealth_Down_UnexpectedException() {
        // Arrange
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(fallbackScheduler.isFallbackActive()).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }
}
