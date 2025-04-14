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

package org.eclipse.ecsp.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * ObjectMapperUtil.
 */
public class ObjectMapperUtil {

    private static final ObjectMapper INSTANCE;

    static {
        INSTANCE = JsonMapper.builder().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS).build();
        INSTANCE.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        INSTANCE.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        INSTANCE.configure(Feature.INCLUDE_SOURCE_IN_LOCATION, true);
        INSTANCE.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        INSTANCE.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    /**
     * Default constructor.
     */
    private ObjectMapperUtil() {
    }

    /**
     * getObjectMapper method returns ObjectMapper instance.
     *
     * @return ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return INSTANCE;
    }

}
