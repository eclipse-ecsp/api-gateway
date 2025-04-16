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

package org.eclipse.ecsp.gateway.plugins;

import org.eclipse.ecsp.gateway.utils.JarUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Load external jar plugin classes and register to spring context.
 */
@Component
public class PluginLoader {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PluginLoader.class);
    private final GenericApplicationContext applicationContext;

    @Value("${plugin.path:}")
    private String pluginJarPath;

    @Value("#{'${plugin.classes:}'.split(',')}")
    private List<String> pluginJarClasses;

    /**
     * Constructor to initialize the PluginLoader with the application context.
     *
     * @param applicationContext the application context
     */
    public PluginLoader(ApplicationContext applicationContext) {
        this.applicationContext = (GenericApplicationContext) applicationContext;
    }

    /**
     * Load plugin classes from external jars and return the object.
     *
     * @return list of plugin class objects
     */
    public List<Object> loadPlugins() {
        List<Object> plugins = new ArrayList<>(pluginJarClasses.size());
        if (!CollectionUtils.isEmpty(pluginJarClasses)) {
            for (String pluginClassName : pluginJarClasses) {
                plugins.add(loadPlugin(pluginClassName));
            }
        }
        return plugins;
    }

    /**
     * Loading plugin class from the external jar.
     *
     * @param pluginClassName name of the class
     * @return instance of the pluginClassName
     */
    public Object loadPlugin(String pluginClassName) {
        Object pluginObj;
        try {
            Class<?> pluginClazz = JarUtils.loadClass(pluginJarPath,
                    pluginClassName,
                    PluginLoader.class.getClassLoader());
            if (pluginClazz != null) {
                LOGGER.info("Found plugin {}, registering to spring container..", pluginClassName);
                // Register the plugin class as a Spring bean
                applicationContext.registerBean(pluginClazz);
                // Return the registered bean from the application context
                pluginObj = applicationContext.getBean(pluginClazz);
                LOGGER.info("plugin {}, registered to spring container...", pluginClassName);
            } else {
                LOGGER.error("Unable to locate plugin class:{}", pluginClassName);
                throw new IllegalArgumentException("Unable to locate plugin class: " + pluginClassName);
            }

        } catch (Exception e) {
            LOGGER.error("Error loading plugin: {}", e);
            throw new IllegalArgumentException("Error Loading plugin", e);
        }
        return pluginObj;
    }
}

