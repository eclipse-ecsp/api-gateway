package org.eclipse.ecsp.registry.events;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RouteChangeEventTest {

    @Test
    void testConstructorsAndGetters() {
        List<String> services = List.of("s1");
        List<String> routes = List.of("r1");

        // Constructor 1
        RouteChangeEvent event1 = new RouteChangeEvent(services, routes);
        assertThat(event1.getEventId()).isNotNull();
        assertThat(event1.getTimestamp()).isNotNull();
        assertThat(event1.getEventType()).isEqualTo(RouteEventType.ROUTE_CHANGE);
        assertThat(event1.getServices()).isEqualTo(services);
        assertThat(event1.getRoutes()).isEqualTo(routes);

        // Constructor 2
        RouteChangeEvent event2 = new RouteChangeEvent(RouteEventType.RATE_LIMIT_CONFIG_CHANGE, services, routes);
        assertThat(event2.getEventType()).isEqualTo(RouteEventType.RATE_LIMIT_CONFIG_CHANGE);
    }

    @Test
    void testJsonCreatorConstructor() {
        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        List<String> services = List.of("s1");
        List<String> routes = List.of("r1");
        
        RouteChangeEvent event = new RouteChangeEvent(eventId, now, 
                RouteEventType.SERVICE_HEALTH_CHANGE, services, routes);
        
        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getTimestamp()).isEqualTo(now);
        assertThat(event.getEventType()).isEqualTo(RouteEventType.SERVICE_HEALTH_CHANGE);
        assertThat(event.getServices()).isEqualTo(services);
        assertThat(event.getRoutes()).isEqualTo(routes);
    }

    @Test
    void testEqualsAndHashCode() {
        List<String> services = List.of("s1");
        List<String> routes = List.of("r1");
        String eventId = "id1";
        Instant now = Instant.now();
        
        RouteChangeEvent event1 = new RouteChangeEvent(eventId, now, RouteEventType.ROUTE_CHANGE, services, routes);
        RouteChangeEvent event2 = new RouteChangeEvent(eventId, now, RouteEventType.ROUTE_CHANGE, services, routes);
        
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).hasSameHashCodeAs(event2.hashCode());
        
        assertThat(event1.toString()).contains(eventId);
    }
}
