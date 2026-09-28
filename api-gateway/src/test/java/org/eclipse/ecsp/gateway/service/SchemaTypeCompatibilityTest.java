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

package org.eclipse.ecsp.gateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.eclipse.ecsp.gateway.utils.ObjectMapperUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

class SchemaTypeCompatibilityTest {

    @Test
    void normalizeSchemaTypesHandlesStandardArrayComponentsAndScalars() throws Exception {
        JsonNode schemaNode = ObjectMapperUtil.getObjectMapper().readTree("""
                {
                  "type": ["object", "null"],
                  "components": {
                    "schemas": {
                      "Count": { "types": "integer" }
                    }
                  },
                  "dependencies": ["not-a-schema-map"]
                }
                """);

        Assertions.assertDoesNotThrow(() -> IgniteRouteLocator.normalizeSchemaTypes(null));
        Assertions.assertDoesNotThrow(() -> IgniteRouteLocator.normalizeSchemaTypes(TextNode.valueOf("string")));
        IgniteRouteLocator.normalizeSchemaTypes(schemaNode);

        Assertions.assertEquals("object", schemaNode.path("type").asText());
        Assertions.assertTrue(schemaNode.path("nullable").asBoolean());
        JsonNode countSchema = schemaNode.path("components").path("schemas").path("Count");
        Assertions.assertEquals("integer", countSchema.path("type").asText());
        Assertions.assertFalse(countSchema.has("types"));
    }

    @Test
    void normalizeSchemaTypesPreservesExistingCompositionsAndType() throws Exception {
        JsonNode schemaNode = ObjectMapperUtil.getObjectMapper().readTree("""
                {
                  "types": ["string", "integer"],
                  "oneOf": [{ "type": "string" }],
                  "properties": {
                    "existing": { "type": "string", "types": ["integer", "null"] },
                    "nullOnly": { "types": [null] },
                    "duplicates": { "types": [42, "string", "string"] },
                    "composed": {
                      "types": ["string", "integer"],
                      "anyOf": [{ "type": "integer" }]
                    }
                  }
                }
                """);

        IgniteRouteLocator.normalizeSchemaTypes(schemaNode);

        Assertions.assertFalse(schemaNode.has("type"));
        Assertions.assertEquals(1, schemaNode.path("oneOf").size());
        JsonNode properties = schemaNode.path("properties");
        Assertions.assertEquals("string", properties.path("existing").path("type").asText());
        Assertions.assertTrue(properties.path("existing").path("nullable").asBoolean());
        Assertions.assertTrue(properties.path("nullOnly").path("nullable").asBoolean());
        Assertions.assertFalse(properties.path("nullOnly").has("type"));
        Assertions.assertEquals("string", properties.path("duplicates").path("type").asText());
        Assertions.assertFalse(properties.path("composed").has("oneOf"));
        Assertions.assertEquals(1, properties.path("composed").path("anyOf").size());
    }

    @Test
    void normalizedSchemaRejectsUnsupportedPayloadTypes() throws Exception {
        JsonNode schemaNode = ObjectMapperUtil.getObjectMapper().readTree("""
                {
                  "types": ["object"],
                  "required": ["value"],
                  "properties": {
                    "value": { "types": ["string", "integer", "null"] }
                  }
                }
                """);
        IgniteRouteLocator.normalizeSchemaTypes(schemaNode);
        SchemaValidator validator = new SchemaValidator(null, schemaNode);

        ValidationData<Void> validation = new ValidationData<>();
        validator.validate(ObjectMapperUtil.getObjectMapper().readTree("{\"value\":true}"), validation);

        Assertions.assertFalse(validation.isValid());
    }
}