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
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

/**
 * PredicateDefinition.
 *
 * @author Sbala2
 */
@Validated
@Entity
public class PredicateDefinition {

    @NotNull
    private String name;

    private Map<String, String> args = new LinkedHashMap<>();

    /**
     * Default constructor.
     */
    public PredicateDefinition() {
    }

    /**
     * Parameterized constructor.
     *
     * @param text used  to set the predicate
     */
    public PredicateDefinition(String text) {
        int eqIdx = text.indexOf('=');
        if (eqIdx <= 0) {
            throw new ValidationException(
                    "Unable to parse PredicateDefinition text '" + text + "'" + ", must be of the form name=value");
        }
        setName(text.substring(0, eqIdx));

        String[] tokenized = tokenizeToStringArray(text.substring(eqIdx + 1), ",");

        for (int i = 0; i < tokenized.length; i++) {
            this.args.put(NameUtils.generateName(i), tokenized[i]);
        }
    }

    /**
     * Get the name of the predicate.
     *
     * @return name of the predicate
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the predicate.
     *
     * @param name name of the predicate
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * get the predicate args.
     *
     * @return args
     */
    public Map<String, String> getArgs() {
        return args;
    }

    /**
     * get the predicate args.
     *
     * @param args predicate args
     */
    public void setArgs(Map<String, String> args) {
        this.args = args;
    }

    /**
     * add arg to the predicate args.
     *
     * @param key   key predicate key
     * @param value predicate value.
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
        PredicateDefinition that = (PredicateDefinition) o;
        return Objects.equals(name, that.name) && Objects.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, args);
    }

    @Override
    public String toString() {
        return "PredicateDefinition{" + "name='" + name + '\'' + ", args=" + args + '}';
    }

}
