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

package org.eclipse.ecsp.gateway.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to load the jars from external path and classes uring {@link URLClassLoader}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JarUtils {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(JarUtils.class);

    /**
     * Load the class from the external jar path.
     *
     * @param path              the path to the external jar
     * @param clazzName         the name of the class to load
     * @param parentClassLoader the parent class loader
     * @return the loaded class
     */
    @SuppressWarnings("java:S2095")
    public static Class<?> loadClass(String path, String clazzName, ClassLoader parentClassLoader) {
        LOGGER.info("Loading external jars from: {}", path);
        File externalJarPath = new File(path);
        if (externalJarPath.exists() && externalJarPath.isDirectory()) {
            List<URL> urls = new ArrayList<>();
            for (File jarFile : externalJarPath.listFiles(((dir, name) -> name.endsWith(".jar")))) {
                try {
                    LOGGER.info("Jar {} found at directory: {}", jarFile.getName(), path);
                    urls.add(jarFile.toURI().toURL());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error while loading external jars from path:"
                            + externalJarPath, e);
                }
            }
            if (!urls.isEmpty()) {
                try {
                    URLClassLoader urlClassLoader = new URLClassLoader("urlClassLoader",
                            urls.toArray(new URL[urls.size()]), parentClassLoader);
                    Class<?> clazz = urlClassLoader.loadClass(clazzName.trim());
                    LOGGER.info("Successfully loaded class: {}", clazz);
                    return clazz;
                } catch (Exception e) {
                    LOGGER.error("Error occurred while loading class: " + clazzName, e);
                }
            }
        } else {
            LOGGER.warn("Invalid external library path : {}, doesn't exists", path);
        }
        return null;
    }
}
