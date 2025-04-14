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

package org.eclipse.ecsp.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.io.Serializable;
import java.util.Map;

/**
 * POJO Defines the format of Response Object.
 *
 * @author SBala2
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@JsonInclude(value = Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response implements Serializable {
    private static final long serialVersionUID = -221442880038344851L;

    /**
     * defines the response status code.
     */
    private Integer code;
    /**
     * defines the response message.
     */
    private String message;
    /**
     * defines the arguments used in the response.
     */
    private Map<String, String> args;

    /**
     * Parameterized constructor with message parameter.
     *
     * @param message signifies the response message
     */
    public Response(String message) {
        this.message = message;
    }

    /**
     * Parameterized constructor.
     *
     * @param code    Response code
     * @param message Response message
     */
    public Response(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
