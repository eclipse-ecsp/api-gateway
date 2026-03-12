package org.eclipse.ecsp.gateway.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RouteRefreshServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RouteRefreshService routeRefreshService;

    @Test
    void testRefreshRoutes_PublishesEvent() {
        // Act
        routeRefreshService.refreshRoutes();

        // Assert
        verify(eventPublisher, times(1)).publishEvent(any(RefreshRoutesEvent.class));
    }
}
