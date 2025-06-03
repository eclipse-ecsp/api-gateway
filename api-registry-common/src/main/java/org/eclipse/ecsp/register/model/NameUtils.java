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

import org.eclipse.ecsp.utils.RegistryCommonConstants;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NameUtils.
 *
 * @author Sbala2
 */
public final class NameUtils {
    private static final Pattern NAME_PATTERN = Pattern.compile("([A-Z][a-z0-9]+)");

    /**
     * Private constructor to prevent instantiation.
     */
    private NameUtils() {
        throw new AssertionError("Must not instantiate utility class.");
    }

    /**
     * method generateName generates a name based on the given index.
     *
     * @param i index to generate name
     * @return generated name
     */
    public static String generateName(int i) {
        return RegistryCommonConstants.GENERATED_NAME_PREFIX + i;
    }

    /**
     * method normalizeToCanonicalPropertyFormat converts the name to canonical format.
     *
     * @param name name to convert
     * @return converted name
     */
    public static String normalizeToCanonicalPropertyFormat(String name) {
        Matcher matcher = NAME_PATTERN.matcher(name);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            if (!builder.isEmpty()) {
                matcher.appendReplacement(builder, "-" + matcher.group(1));
            } else {
                matcher.appendReplacement(builder, matcher.group(1));
            }
        }
        return builder.toString().toLowerCase();
    }

}