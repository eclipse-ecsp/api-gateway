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

package org.eclipse.ecsp.register;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.eclipse.ecsp.utils.ObjectMapperUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.LinkedList;
import java.util.List;

class SchemaReferenceCollectorTest {

    @Test
    void collectSchemaReferencesHandlesArraysBooleansMapsAndDiscriminators() throws Exception {
        JsonNode schemaNode = ObjectMapperUtil.getObjectMapper().readTree("""
                [
                  {
                    "allOf": [{ "$ref": "#/components/schemas/Alpha" }],
                    "additionalProperties": true,
                    "properties": {
                      "gamma": { "$ref": "#/components/schemas/Gamma" }
                    },
                    "dependencies": ["not-a-schema-map"],
                    "discriminator": {
                      "mapping": {
                        "beta": "#/components/schemas/Beta",
                        "external": "https://example.com/schema.json"
                      }
                    }
                  },
                  "literal"
                ]
                """);
        LinkedList<String> references = new LinkedList<>();

        ReflectionTestUtils.invokeMethod(
                ApiRoutesLoader.class, "collectSchemaReferences", schemaNode, references);

        Assertions.assertEquals(List.of("Alpha", "Gamma", "Beta"), references);
    }

    @Test
    void addComponentSchemaReferenceIgnoresMalformedAndDecodesValidReferences() {
        LinkedList<String> references = new LinkedList<>();

        invokeAddReference(null, references);
        invokeAddReference(IntNode.valueOf(7), references);
        invokeAddReference(TextNode.valueOf("https://example.com/schema.json"), references);
        invokeAddReference(TextNode.valueOf("#/components/schemas/"), references);
        invokeAddReference(TextNode.valueOf("#/components/schemas/Vehicle/properties/id"), references);
        invokeAddReference(TextNode.valueOf("#/components/schemas/A~1B~0C"), references);

        Assertions.assertEquals(List.of("Vehicle", "A/B~C"), references);
    }

    private void invokeAddReference(JsonNode referenceNode, LinkedList<String> references) {
        ReflectionTestUtils.invokeMethod(
                ApiRoutesLoader.class, "addComponentSchemaReference", referenceNode, references);
    }
}