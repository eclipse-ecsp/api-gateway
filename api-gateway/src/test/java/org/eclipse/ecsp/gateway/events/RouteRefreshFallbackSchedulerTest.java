package org.eclipse.ecsp.gateway.events;

import org.eclipse.ecsp.gateway.service.RouteRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteRefreshFallbackSchedulerTest {

    @Mock
    private RouteRefreshService routeRefreshService;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private RedisConnection redisConnection;

    @InjectMocks
    private RouteRefreshFallbackScheduler scheduler;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testCheckRedisAndRefreshRoutes_RedisUp_NoAction() {
        // Arrange
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        // Act
        scheduler.checkRedisAndRefreshRoutes();

        // Assert
        verify(routeRefreshService, never()).refreshRoutes();
    }

    @Test
    void testCheckRedisAndRefreshRoutes_RedisDown_EnablesFallbackAndRefreshes() {
        // Arrange
        when(redisConnectionFactory.getConnection())
                .thenThrow(new RedisConnectionFailureException("Connection failed"));

        // Act - First run when Redis goes down
        scheduler.checkRedisAndRefreshRoutes();

        // Assert
        verify(routeRefreshService, times(1)).refreshRoutes();
    }

    @Test
    void testCheckRedisAndRefreshRoutes_RedisStaysDown_RefreshesAgain() {
        // Arrange
        when(redisConnectionFactory.getConnection())
                .thenThrow(new RedisConnectionFailureException("Connection failed"));

        // Act - First run (Goes down)
        scheduler.checkRedisAndRefreshRoutes();
        // Act - Second run (Stays down)
        scheduler.checkRedisAndRefreshRoutes();

        final int expectedInvocations = 2;
        // Assert
        verify(routeRefreshService, times(expectedInvocations)).refreshRoutes();
    }

    @Test
    void testCheckRedisAndRefreshRoutes_RedisRecovers_DisablesFallback() {
        // Arrange - Sequence: Down -> Up
        when(redisConnectionFactory.getConnection())
                .thenThrow(new RedisConnectionFailureException("Connection failed")) // 1st call
                .thenReturn(redisConnection); // 2nd call
        when(redisConnection.ping()).thenReturn("PONG");

        // Act - Redis goes down
        scheduler.checkRedisAndRefreshRoutes();
        // Act - Redis comes back up
        scheduler.checkRedisAndRefreshRoutes();

        // Assert
        // Should have called refresh once (when down), and not again when it came back up
        verify(routeRefreshService, times(1)).refreshRoutes();
    }

    @Test
    void testCheckRedisAndRefreshRoutes_RefreshException_HandledGracefully() {
        // Arrange
        when(redisConnectionFactory.getConnection())
                .thenThrow(new RedisConnectionFailureException("Connection failed"));
        doThrow(new RuntimeException("Refresh failed")).when(routeRefreshService).refreshRoutes();

        // Act
        scheduler.checkRedisAndRefreshRoutes();

        // Assert
        verify(routeRefreshService, times(1)).refreshRoutes();
        // Exception should be caught and logged, test should success
    }

    @Test
    void testCheckRedisAndRefreshRoutes_PingFails_TriggersFallback() {
        // Arrange
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        doThrow(new RuntimeException("Ping failed")).when(redisConnection).ping();

        // Act
        scheduler.checkRedisAndRefreshRoutes();

        // Assert
        verify(routeRefreshService, times(1)).refreshRoutes();
    }

    @Test
    void testActivateFallback_ManuallyEnablesFallback() {
        scheduler.activateFallback();
        assertTrue(scheduler.isFallbackActive());
    }

    @Test
    void testIsFallbackActive_ReturnsCorrectState() {
        assertFalse(scheduler.isFallbackActive());
        scheduler.activateFallback();
        assertTrue(scheduler.isFallbackActive());
    }
}
