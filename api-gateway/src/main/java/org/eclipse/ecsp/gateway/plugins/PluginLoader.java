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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.ecsp.gateway.utils.JarUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Load external jar plugin classes and register to spring context.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PluginLoader {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PluginLoader.class);
    
    private final GenericApplicationContext applicationContext;
    private final Object initializationMonitor = new Object();

    private volatile boolean initializationAttempted;
    private URLClassLoader pluginClassLoader;

    @Value("${plugin.enabled:false}")
    private boolean pluginEnabled;

    @Value("${plugin.path:}")
    private String pluginJarPath;

    @Value("#{'${plugin.classes:}'.split(',')}")
    private List<String> pluginJarClasses;

    @Value("#{'${plugin.packages:}'.split(',')}")
    private List<String> pluginPackages;

    /**
     * Constructor to initialize the PluginLoader with the application context.
     *
     * @param applicationContext the application context
     */
    public PluginLoader(ApplicationContext applicationContext) {
        this.applicationContext = (GenericApplicationContext) applicationContext;
    }

    @PostConstruct
    void initialize() {
        if (pluginEnabled) {
            pluginJarClasses = sanitize(pluginJarClasses);
            pluginPackages = sanitize(pluginPackages);
            ensurePluginInfrastructure();    
        }
        
    }

    @PreDestroy
    void destroy() {
        if (pluginClassLoader != null) {
            try {
                pluginClassLoader.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close plugin class loader cleanly", e);
            }
        }
    }

    /**
     * Load plugin classes from external jars and return the object.
     *
     * @return list of plugin class objects
     */
    public List<Object> loadPlugins() {
        ensurePluginInfrastructure();
        if (CollectionUtils.isEmpty(pluginJarClasses)) {
            LOGGER.info("No plugin classes configured, skipping explicit plugin loading");
            return Collections.emptyList();
        }
        List<Object> plugins = new ArrayList<>(pluginJarClasses.size());
        for (String pluginClassName : pluginJarClasses) {
            plugins.add(loadPlugin(pluginClassName));
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
        ensurePluginInfrastructure();
        if (!StringUtils.hasText(pluginClassName)) {
            throw new IllegalArgumentException("Plugin class name must not be blank");
        }
        if (pluginClassLoader == null) {
            throw new IllegalStateException("Plugin class loader is not initialized. Check plugin.path configuration.");
        }
        try {
            Class<?> pluginClazz = ClassUtils.forName(pluginClassName.trim(), pluginClassLoader);
            registerBeanIfAbsent(pluginClazz);
            Object pluginObj = applicationContext.getBean(pluginClazz);
            LOGGER.info("Plugin {} registered with the Spring container", pluginClassName);
            return pluginObj;
        } catch (BeansException | ClassNotFoundException e) {
            LOGGER.error("Error loading plugin class: {}", pluginClassName, e);
            throw new IllegalArgumentException("Error loading plugin " + pluginClassName, e);
        }
    }

    private void ensurePluginInfrastructure() {
        if (initializationAttempted) {
            return;
        }
        synchronized (initializationMonitor) {
            if (initializationAttempted) {
                return;
            }
            initializationAttempted = true;
            if (!StringUtils.hasText(pluginJarPath)) {
                LOGGER.info("No plugin.path configured; skipping external plugin initialization");
                return;
            }
            pluginClassLoader = JarUtils.createClassLoader(pluginJarPath, PluginLoader.class.getClassLoader());
            if (pluginClassLoader == null) {
                LOGGER.warn("Plugin class loader could not be created for path: {}", pluginJarPath);
                return;
            }
            applicationContext.setClassLoader(pluginClassLoader);
            applicationContext.getDefaultListableBeanFactory().setBeanClassLoader(pluginClassLoader);
            registerPackages();
        }
    }

    private void registerPackages() {
        if (CollectionUtils.isEmpty(pluginPackages)) {
            LOGGER.debug("No plugin packages configured for scanning");
            return;
        }
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.setEnvironment(applicationContext.getEnvironment());
        scanner.setResourceLoader(new PathMatchingResourcePatternResolver(pluginClassLoader));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        for (String basePackage : pluginPackages) {
            try {
                Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
                if (CollectionUtils.isEmpty(candidates)) {
                    LOGGER.info("No components discovered under plugin package: {}", basePackage);
                    continue;
                }
                for (BeanDefinition beanDefinition : candidates) {
                    registerCandidate(beanDefinition);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to scan plugin package: {}", basePackage, e);
            }
        }
    }

    private void registerCandidate(BeanDefinition beanDefinition) {
        String beanClassName = beanDefinition.getBeanClassName();
        if (!StringUtils.hasText(beanClassName)) {
            return;
        }
        try {
            Class<?> candidateClass = ClassUtils.forName(beanClassName, pluginClassLoader);
            registerBeanIfAbsent(candidateClass);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Unable to load plugin bean class: {}", beanClassName, e);
        }
    }

    private void registerBeanIfAbsent(Class<?> pluginClazz) {
        String[] existingBeans = applicationContext.getBeanNamesForType(pluginClazz, false, false);
        if (existingBeans.length > 0) {
            LOGGER.debug("Plugin bean {} already registered", pluginClazz.getName());
            return;
        }
        LOGGER.info("Registering plugin bean {}", pluginClazz.getName());
        applicationContext.registerBean(pluginClazz.getSimpleName(), pluginClazz);
        LOGGER.info("Plugin bean {} registered successfully with bean name {}", pluginClazz.getName(), 
            pluginClazz.getSimpleName());
    }

    private List<String> sanitize(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(value -> value != null && StringUtils.hasText(value.trim()))
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }
}

