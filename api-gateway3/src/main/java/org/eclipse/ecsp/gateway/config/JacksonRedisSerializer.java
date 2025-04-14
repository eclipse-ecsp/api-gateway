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

package org.eclipse.ecsp.gateway.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * JacksonRedisSerializer helps to serialize the objects.
 *
 * @param <T> Can be {@literal null}
 */
@EnableAutoConfiguration
public class JacksonRedisSerializer<T> implements RedisSerializer<T> {
    /**
     * Creating LOGGER instacne.
     */
    private static final Logger LOGGER
            = LoggerFactory.getLogger(JacksonRedisSerializer.class);

    /**
     * Created an ObjectMapper instance to read data from object.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * serialize the object.
     *
     * @param t object to serialize. Can be {@literal null}.
     * @return byte Array
     * @throws SerializationException throws exception if any during serialization
     */
    @Override
    public byte[] serialize(T t) throws SerializationException {
        try {
            return objectMapper.writeValueAsBytes(t);
        } catch (Exception e) {
            LOGGER.error("Error serializing to JSON {} / Error Cause :{}", t, e);
            throw new SerializationException("Error serializing object" + t, e);
        }
    }

    /**
     * returns a deserialized object.
     *
     * @param bytes object binary representation. Can be {@literal null}.
     * @return returns deserialized object
     * @throws SerializationException throws exception if any during serialization
     */

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, new TypeReference<T>() {
            });
        } catch (Exception e) {
            LOGGER.error("Error deserializing JSON {}/ Error Cause :{}",
                    new String(bytes), e);
            throw new SerializationException("Error deserializing JSON: "
                    + new String(bytes), e);
        }
    }
}