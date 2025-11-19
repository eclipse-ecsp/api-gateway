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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MapToJsonConverter}.
 *
 * @author Abhishek Kumar
 */
class MapToJsonConverterTest {

    private static final int EXPECTED_MAP_SIZE_TWO = 2;

    private MapToJsonConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MapToJsonConverter();
    }

    @Test
    void convertToDatabaseColumn_WithValidMap_ShouldReturnJson() {
        // Arrange
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        // Act
        String result = converter.convertToDatabaseColumn(map);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("key1"));
        assertTrue(result.contains("value1"));
        assertTrue(result.contains("key2"));
        assertTrue(result.contains("value2"));
    }

    @Test
    void convertToDatabaseColumn_WithEmptyMap_ShouldReturnNull() {
        // Arrange
        Map<String, String> map = new HashMap<>();

        // Act
        String result = converter.convertToDatabaseColumn(map);

        // Assert
        assertNull(result);
    }

    @Test
    void convertToDatabaseColumn_WithNullMap_ShouldReturnNull() {
        // Act
        String result = converter.convertToDatabaseColumn(null);

        // Assert
        assertNull(result);
    }

    @Test
    void convertToDatabaseColumn_WithSingleEntry_ShouldReturnValidJson() {
        // Arrange
        Map<String, String> map = new HashMap<>();
        map.put("headerName", "X-API-Key");

        // Act
        String result = converter.convertToDatabaseColumn(map);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("headerName"));
        assertTrue(result.contains("X-API-Key"));
    }

    @Test
    void convertToDatabaseColumn_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Arrange
        Map<String, String> map = new HashMap<>();
        map.put("key", "value with spaces");
        map.put("special", "value:with:colons");

        // Act
        String result = converter.convertToDatabaseColumn(map);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("value with spaces"));
        assertTrue(result.contains("value:with:colons"));
    }

    @Test
    void convertToEntityAttribute_WithValidJson_ShouldReturnMap() {
        // Arrange
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        // Act
        Map<String, String> result = converter.convertToEntityAttribute(json);

        // Assert
        assertNotNull(result);
        assertEquals(EXPECTED_MAP_SIZE_TWO, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    void convertToEntityAttribute_WithEmptyString_ShouldReturnEmptyMap() {
        // Act
        Map<String, String> result = converter.convertToEntityAttribute("");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToEntityAttribute_WithNullString_ShouldReturnEmptyMap() {
        // Act
        Map<String, String> result = converter.convertToEntityAttribute(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToEntityAttribute_WithInvalidJson_ShouldReturnEmptyMap() {
        // Arrange
        String invalidJson = "not a valid json";

        // Act
        Map<String, String> result = converter.convertToEntityAttribute(invalidJson);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToEntityAttribute_WithMalformedJson_ShouldReturnEmptyMap() {
        // Arrange
        String malformedJson = "{\"key\":\"value\"";

        // Act
        Map<String, String> result = converter.convertToEntityAttribute(malformedJson);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToEntityAttribute_WithSingleEntry_ShouldReturnMapWithOneEntry() {
        // Arrange
        String json = "{\"headerName\":\"X-API-Key\"}";

        // Act
        Map<String, String> result = converter.convertToEntityAttribute(json);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("X-API-Key", result.get("headerName"));
    }

    @Test
    void roundTrip_ConvertToDbAndBack_ShouldMaintainData() {
        // Arrange
        Map<String, String> original = new HashMap<>();
        original.put("key1", "value1");
        original.put("key2", "value2");

        // Act
        String json = converter.convertToDatabaseColumn(original);
        Map<String, String> result = converter.convertToEntityAttribute(json);

        // Assert
        assertNotNull(result);
        assertEquals(EXPECTED_MAP_SIZE_TWO, result.size());
        assertEquals(original.get("key1"), result.get("key1"));
        assertEquals(original.get("key2"), result.get("key2"));
    }

    @Test
    void convertToEntityAttribute_WithWhitespaceJson_ShouldReturnEmptyMap() {
        // Arrange
        String whitespaceJson = "   ";

        // Act
        Map<String, String> result = converter.convertToEntityAttribute(whitespaceJson);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToDatabaseColumn_WithComplexValues_ShouldHandleCorrectly() {
        // Arrange
        Map<String, String> map = new HashMap<>();
        map.put("url", "https://example.com/api?param=value");
        map.put("regex", "^[a-zA-Z0-9]+$");

        // Act
        String result = converter.convertToDatabaseColumn(map);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("https://example.com/api?param=value"));
        assertTrue(result.contains("^[a-zA-Z0-9]+$"));
    }
}
