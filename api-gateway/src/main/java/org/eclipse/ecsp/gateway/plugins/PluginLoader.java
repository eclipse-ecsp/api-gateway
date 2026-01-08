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

import jakarta.annotation.PreDestroy;
import org.eclipse.ecsp.gateway.utils.JarUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
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

/**
 * Load external jar plugin classes and register to spring context.
 * Implements BeanFactoryPostProcessor to register beans before instantiation.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PluginLoader implements ApplicationContextAware, BeanFactoryPostProcessor {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PluginLoader.class);
    
    private GenericApplicationContext applicationContext;
    private final Object initializationMonitor = new Object();

    private volatile boolean initializationAttempted;
    private URLClassLoader pluginClassLoader;

    private String pluginJarPath;
    private List<String> pluginJarClasses;
    private List<String> pluginPackages;

    /**
     * Post-process the bean factory to load and register plugins before beans are instantiated.
     * This allows plugin @Configuration classes and @Bean methods to be processed by Spring.
     *
     * @param beanFactory the bean factory
     * @throws BeansException if an error occurs
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // Read configuration properties from environment
        String enabled = applicationContext.getEnvironment().getProperty("plugin.enabled", "false");
        boolean pluginEnabled = Boolean.parseBoolean(enabled);
        
        if (!pluginEnabled) {
            LOGGER.info("Plugin loading is disabled");
            return;
        }
        
        pluginJarPath = applicationContext.getEnvironment().getProperty("plugin.path", "");
        String classes = applicationContext.getEnvironment().getProperty("plugin.classes", "");
        String packages = applicationContext.getEnvironment().getProperty("plugin.packages", "");
        
        pluginJarClasses = sanitize(classes.isEmpty() ? Collections.emptyList() : 
            List.of(classes.split(",")));
        pluginPackages = sanitize(packages.isEmpty() ? Collections.emptyList() : 
            List.of(packages.split(",")));
        
        LOGGER.info("Plugin loading enabled. Path: {}, Classes: {}, Packages: {}", 
            pluginJarPath, pluginJarClasses.size(), pluginPackages.size());
        ensurePluginInfrastructure();
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
        if (!StringUtils.hasText(pluginClassName)) {
            throw new IllegalArgumentException("Plugin class name must not be blank");
        }
        ensurePluginInfrastructure();
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
            if (!StringUtils.hasText(pluginJarPath)) {
                LOGGER.info("No plugin.path configured; skipping external plugin initialization");
                initializationAttempted = true;
                return;
            }
            pluginClassLoader = JarUtils.createClassLoader(pluginJarPath, PluginLoader.class.getClassLoader());
            if (pluginClassLoader == null) {
                LOGGER.warn("Plugin class loader could not be created for path: {}", pluginJarPath);
                initializationAttempted = true;
                return;
            }
            applicationContext.setClassLoader(pluginClassLoader);
            applicationContext.getDefaultListableBeanFactory().setBeanClassLoader(pluginClassLoader);
            registerPackages();
            initializationAttempted = true;
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
        scanner.addIncludeFilter(new AnnotationTypeFilter(Configuration.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Bean.class));
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
        
        // Generate bean name following Spring's naming convention (camelCase)
        String beanName = generateBeanName(pluginClazz);
        
        // Check if this is a @Configuration class
        boolean isConfigurationClass = pluginClazz.isAnnotationPresent(Configuration.class);
        
        // Register bean definition - Spring will instantiate it later with full support for @Value, @Autowired, etc.
        applicationContext.registerBean(beanName, pluginClazz, bd -> {
            bd.setAutowireCandidate(true);
            bd.setLazyInit(false);
            bd.setScope(BeanDefinition.SCOPE_SINGLETON);
            if (isConfigurationClass) {
                bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            } else {
                bd.setRole(BeanDefinition.ROLE_APPLICATION);
            }
        });
        
        LOGGER.info("Plugin bean {} registered successfully with bean name '{}'. " 
            + "Will be instantiated by Spring with full @Value, @Autowired support", 
            pluginClazz.getName(), beanName);
    }

    /**
     * Generates a bean name following Spring's naming convention.
     * First checks for explicit bean name in @Component or @Configuration annotations.
     * If not found, converts simple class name to camelCase (first letter lowercase).
     *
     * <p>Examples:
     * - @Component("classa") -> classa
     * - @Configuration("myCustomConfig") -> myCustomConfig
     * - ClassA (no annotation value) -> classA
     * - VehicleIdKeyResolver (no annotation value) -> vehicleIdKeyResolver
     *
     * @param clazz the class to generate bean name for
     * @return the bean name in camelCase format or custom name from annotation
     */
    private String generateBeanName(Class<?> clazz) {
        // Check for @Component annotation with custom bean name
        Component componentAnnotation = clazz.getAnnotation(Component.class);
        if (componentAnnotation != null && StringUtils.hasText(componentAnnotation.value())) {
            return componentAnnotation.value();
        }
        
        // Check for @Configuration annotation with custom bean name
        Configuration configAnnotation = clazz.getAnnotation(Configuration.class);
        if (configAnnotation != null && StringUtils.hasText(configAnnotation.value())) {
            return configAnnotation.value();
        }
        
        // Fall back to Spring's default naming convention: camelCase
        String simpleName = clazz.getSimpleName();
        // Convert first character to lowercase (Spring's bean naming convention)
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private List<String> sanitize(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(value -> value != null && StringUtils.hasText(value.trim()))
                .map(String::trim)
                .distinct()
                .toList();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (GenericApplicationContext) applicationContext;
    }

    /**
     * Clean up resources on shutdown.
     */
    @PreDestroy
    public void cleanup() {
        if (pluginClassLoader != null) {
            try {
                pluginClassLoader.close();
                LOGGER.info("Plugin class loader closed successfully");
            } catch (IOException e) {
                LOGGER.error("Error closing plugin class loader", e);
            }
        }
    }
}
