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

import dev.morphia.annotations.Entity;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

/**
 * FilterDefinition.
 *
 * @author Sbala2
 */
@Validated
@Entity
public class FilterDefinition {

    @NotNull
    private String name;

    private Map<String, String> args = new LinkedHashMap<>();

    /**
     * FilterDefinition default constructor.
     */
    public FilterDefinition() {
    }

    /**
     * FilterDefinition constructor.
     *
     * @param text parameters to constructor.
     */
    public FilterDefinition(String text) {
        int eqIdx = text.indexOf('=');
        if (eqIdx <= 0) {
            setName(text);
            return;
        }
        setName(text.substring(0, eqIdx));

        String[] args = tokenizeToStringArray(text.substring(eqIdx + 1), ",");

        for (int i = 0; i < args.length; i++) {
            this.args.put(NameUtils.generateName(i), args[i]);
        }
    }

    /**
     * getName returns the name of the filter.
     *
     * @return filter name.
     */
    public String getName() {
        return name;
    }

    /**
     * setName sets the name of the filter.
     *
     * @param name filter name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * getArgs returns the arguments of the filter.
     *
     * @return filter arguments.
     */
    public Map<String, String> getArgs() {
        return args;
    }

    /**
     * setArgs sets the arguments of the filter.
     *
     * @param args filter arguments.
     */
    public void setArgs(Map<String, String> args) {
        this.args = args;
    }

    /**
     * addArg adds an argument to the filter.
     *
     * @param key   argument key.
     * @param value argument value.
     */
    public void addArg(String key, String value) {
        this.args.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilterDefinition that = (FilterDefinition) o;
        return Objects.equals(name, that.name) && Objects.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, args);
    }

    @Override
    public String toString() {
        return "FilterDefinition{" + "name='" + name + '\'' + ", args=" + args + '}';
    }

}
