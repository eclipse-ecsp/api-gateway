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

package org.eclipse.ecsp.register.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.Map;

/**
 * Response.
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@JsonInclude(value = Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {

    /**
     * Response code.
     */
    protected Integer code;
    /**
     * Response message.
     */
    protected String message;
    /**
     * Response args.
     */
    protected Map<String, String> args;

    /**
     * Constructor to initialize {@link Response}.
     *
     * @param message response message
     */
    public Response(String message) {
        this.message = message;
    }

    /**
     * Constructor to initialize {@link Response}.
     *
     * @param code    response code
     * @param message response message
     */
    public Response(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
