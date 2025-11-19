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

package org.eclipse.ecsp.registry.entity;

import org.eclipse.ecsp.domain.Version;
import org.eclipse.ecsp.entities.IgniteEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RateLimitConfigEntity}.
 *
 * @author Abhishek Kumar
 */
class RateLimitConfigEntityTest {

    private static final String TEST_ID = "test-id";
    private static final String TEST_ROUTE_ID = "test-route";
    private static final String TEST_SERVICE = "test-service";
    private static final long REPLENISH_RATE = 100L;
    private static final long BURST_CAPACITY = 200L;
    private static final String TEST_KEY_RESOLVER = "CLIENT_IP";
    private static final long REQUESTED_TOKENS = 5L;
    private static final String EMPTY_KEY_STATUS = "429";
    private static final int TEST_YEAR = 2025;
    private static final int TEST_MONTH = 11;
    private static final int TEST_DAY = 19;
    private static final int TEST_HOUR = 10;
    private static final int TEST_MINUTE = 30;
    private static final int MIN_STRING_LENGTH = 2;
    private static final int EXPECTED_ARGS_SIZE = 2;

    @Test
    void constructor_DefaultValues_ShouldBeSetCorrectly() {
        // Act
        RateLimitConfigEntity entity = new RateLimitConfigEntity();

        // Assert
        assertNull(entity.getId());
        assertNull(entity.getRouteId());
        assertNull(entity.getService());
        assertEquals(0L, entity.getReplenishRate());
        assertEquals(0L, entity.getBurstCapacity());
        assertFalse(entity.isIncludeHeaders());
        assertNull(entity.getKeyResolver());
        assertNull(entity.getArgs());
        assertEquals(1L, entity.getRequestedTokens());
        assertTrue(entity.getDenyEmptyKey());
        assertEquals("400", entity.getEmptyKeyStatus());
        assertNull(entity.getLastUpdatedTime());
        assertNull(entity.getSchemaVersion());
    }

    @Test
    void settersAndGetters_AllFields_ShouldWorkCorrectly() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        final LocalDateTime now = LocalDateTime.now();
        Map<String, String> args = new HashMap<>();
        args.put("headerName", "X-API-Key");

        // Act
        entity.setId(TEST_ID);
        entity.setRouteId(TEST_ROUTE_ID);
        entity.setService(TEST_SERVICE);
        entity.setReplenishRate(REPLENISH_RATE);
        entity.setBurstCapacity(BURST_CAPACITY);
        entity.setIncludeHeaders(true);
        entity.setKeyResolver(TEST_KEY_RESOLVER);
        entity.setArgs(args);
        entity.setRequestedTokens(REQUESTED_TOKENS);
        entity.setDenyEmptyKey(false);
        entity.setEmptyKeyStatus(EMPTY_KEY_STATUS);
        entity.setLastUpdatedTime(now);
        entity.setSchemaVersion(Version.V1_0);

        // Assert
        assertEquals(TEST_ID, entity.getId());
        assertEquals(TEST_ROUTE_ID, entity.getRouteId());
        assertEquals(TEST_SERVICE, entity.getService());
        assertEquals(REPLENISH_RATE, entity.getReplenishRate());
        assertEquals(BURST_CAPACITY, entity.getBurstCapacity());
        assertTrue(entity.isIncludeHeaders());
        assertEquals(TEST_KEY_RESOLVER, entity.getKeyResolver());
        assertEquals(args, entity.getArgs());
        assertEquals(REQUESTED_TOKENS, entity.getRequestedTokens());
        assertFalse(entity.getDenyEmptyKey());
        assertEquals(EMPTY_KEY_STATUS, entity.getEmptyKeyStatus());
        assertEquals(now, entity.getLastUpdatedTime());
        assertEquals(Version.V1_0, entity.getSchemaVersion());
    }

    @Test
    void testToString() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        entity.setId(TEST_ID);
        entity.setRouteId(TEST_ROUTE_ID);

        // Act
        String result = entity.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(TEST_ID));
        assertTrue(result.contains(TEST_ROUTE_ID));
        assertTrue(result.length() > MIN_STRING_LENGTH);
    }

    @Test
    void setDenyEmptyKey_WithNull_ShouldAcceptNull() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();

        // Act
        entity.setDenyEmptyKey(null);

        // Assert
        assertNull(entity.getDenyEmptyKey());
    }

    @Test
    void setArgs_WithEmptyMap_ShouldWorkCorrectly() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        Map<String, String> emptyArgs = new HashMap<>();

        // Act
        entity.setArgs(emptyArgs);

        // Assert
        assertNotNull(entity.getArgs());
        assertTrue(entity.getArgs().isEmpty());
    }

    @Test
    void testArgsMap() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        Map<String, String> args = new HashMap<>();
        args.put("arg1", "value1");
        args.put("arg2", "value2");

        // Act
        entity.setArgs(args);

        // Assert
        assertEquals(args, entity.getArgs());
        assertEquals(EXPECTED_ARGS_SIZE, entity.getArgs().size());
    }

    @Test
    void schemaVersion_SetAndGet_ShouldWorkCorrectly() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();

        // Act
        entity.setSchemaVersion(Version.V1_0);

        // Assert
        assertEquals(Version.V1_0, entity.getSchemaVersion());
        assertNotNull(entity.getSchemaVersion());
    }

    @Test
    void testLastUpdatedTime() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        LocalDateTime timestamp = LocalDateTime.of(
            TEST_YEAR, TEST_MONTH, TEST_DAY, TEST_HOUR, TEST_MINUTE);

        // Act
        entity.setLastUpdatedTime(timestamp);

        // Assert
        assertEquals(timestamp, entity.getLastUpdatedTime());
    }

    @Test
    void auditableIgniteEntity_Implementation_ShouldWorkCorrectly() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        LocalDateTime now = LocalDateTime.now();

        // Act
        entity.setLastUpdatedTime(now);

        // Assert
        assertEquals(now, entity.getLastUpdatedTime());
    }

    @Test
    void igniteEntity_Implementation_ShouldWorkCorrectly() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();

        // Act
        entity.setSchemaVersion(Version.V1_0);

        // Assert
        assertEquals(Version.V1_0, entity.getSchemaVersion());
        assertNotNull(entity.getSchemaVersion());
        assertTrue(entity instanceof IgniteEntity);
    }

    @Test
    void setId_WithNull_ShouldAcceptNull() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();

        // Act
        entity.setId(null);

        // Assert
        assertNull(entity.getId());
    }

    @Test
    void setKeyResolver_WithDifferentValues_ShouldUpdate() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();

        // Act & Assert
        entity.setKeyResolver("clientIp");
        assertEquals("clientIp", entity.getKeyResolver());

        entity.setKeyResolver("apiKey");
        assertEquals("apiKey", entity.getKeyResolver());
    }

    @Test
    void includeHeaders_DefaultFalse_CanToggle() {
        // Arrange
        RateLimitConfigEntity entity = new RateLimitConfigEntity();

        // Assert default
        assertFalse(entity.isIncludeHeaders());

        // Act & Assert toggle
        entity.setIncludeHeaders(true);
        assertTrue(entity.isIncludeHeaders());

        entity.setIncludeHeaders(false);
        assertFalse(entity.isIncludeHeaders());
    }
}
