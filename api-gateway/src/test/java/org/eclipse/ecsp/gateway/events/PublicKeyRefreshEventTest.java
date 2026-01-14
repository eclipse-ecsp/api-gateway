package org.eclipse.ecsp.gateway.events;

import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent.RefreshType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PublicKeyRefreshEvent}.
 */
public class PublicKeyRefreshEventTest {

    @Test
    public void testPublicKeyRefreshEvent() {
        PublicKeyRefreshEvent event = new PublicKeyRefreshEvent(RefreshType.PUBLIC_KEY, "test-source");

        Assertions.assertEquals(RefreshType.PUBLIC_KEY, event.getRefreshType());
        Assertions.assertEquals("test-source", event.getSourceId());

        event.setRefreshType(RefreshType.ALL_KEYS);
        event.setSourceId("new-source");

        Assertions.assertEquals(RefreshType.ALL_KEYS, event.getRefreshType());
        Assertions.assertEquals("new-source", event.getSourceId());
    }
}
