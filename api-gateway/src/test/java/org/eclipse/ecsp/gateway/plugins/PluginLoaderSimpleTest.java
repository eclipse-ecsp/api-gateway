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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Simple test class for {@link PluginLoader}.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class PluginLoaderSimpleTest {

    @TempDir
    Path tempDir;

    private GenericApplicationContext applicationContext;
    private PluginLoader pluginLoader;

    @BeforeEach
    void setUp() {
        applicationContext = new GenericApplicationContext();
        applicationContext.refresh();
        pluginLoader = new PluginLoader();
        pluginLoader.setApplicationContext(applicationContext);
    }

    @Test
    void constructorWithValidContextInitializesSuccessfully() {
        assertNotNull(pluginLoader);
    }

    @Test
    void initializeWithPluginDisabledDoesNotThrowException() {
        TestPropertyValues.of("plugin.enabled=false", "plugin.path=/some/path").applyTo(applicationContext);

        try {
            pluginLoader.postProcessBeanFactory(null);
        } catch (Exception e) {
            fail("Initialize should not throw exception when plugins are disabled", e);
        }
    }

    @Test
    void initializeWithPluginEnabledButNoPathDoesNotThrowException() {
        TestPropertyValues.of("plugin.enabled=true", "plugin.path=").applyTo(applicationContext);

        try {
            pluginLoader.postProcessBeanFactory(null);
        } catch (Exception e) {
            fail("Initialize should not throw exception when plugins are disabled", e);
        }
    }

    @Test
    void initializeWithPluginEnabledAndValidPathInitializesSuccessfully() throws Exception {
        final File jarFile = createTestJar();
        
        TestPropertyValues.of("plugin.enabled=true", "plugin.path=" + jarFile.getParent()).applyTo(applicationContext);

        try {
            pluginLoader.postProcessBeanFactory(null);
        } catch (Exception e) {
            fail("Initialize should not throw exception when plugins are disabled", e);
        }

        assertNotNull(applicationContext.getClassLoader());
    }

    @Test
    void loadPluginsWithNoClassesReturnsEmptyList() {
        ReflectionTestUtils.setField(pluginLoader, "pluginJarClasses", Collections.emptyList());

        final List<Object> plugins = pluginLoader.loadPlugins();

        assertNotNull(plugins);
        assertTrue(plugins.isEmpty());
    }

    @Test
    void loadPluginsWithNullClassesReturnsEmptyList() {
        ReflectionTestUtils.setField(pluginLoader, "pluginJarClasses", null);

        final List<Object> plugins = pluginLoader.loadPlugins();

        assertNotNull(plugins);
        assertTrue(plugins.isEmpty());
    }

    @Test
    void loadPluginWithBlankClassNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> pluginLoader.loadPlugin(""));
        assertThrows(IllegalArgumentException.class, () -> pluginLoader.loadPlugin("   "));
    }

    @Test
    void loadPluginWithNullClassNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> pluginLoader.loadPlugin(null));
    }

    @Test
    void loadPluginWithNoClassLoaderInitializedThrowsException() {
        TestPropertyValues.of("plugin.path=").applyTo(applicationContext);
        ReflectionTestUtils.setField(pluginLoader, "initializationAttempted", false);
        ReflectionTestUtils.setField(pluginLoader, "pluginJarPath", "");

        assertThrows(IllegalStateException.class, 
            () -> pluginLoader.loadPlugin("java.lang.String"));
    }

    @Test
    void loadPluginWithValidClassLoadsSuccessfully() throws Exception {
        final File jarFile = createTestJar();
        
        TestPropertyValues.of("plugin.enabled=true", "plugin.path=" + jarFile.getParent()).applyTo(applicationContext);

        pluginLoader.postProcessBeanFactory(null);

        final Object result = pluginLoader.loadPlugin("java.lang.String");

        assertNotNull(result);
        assertTrue(result instanceof String);
    }

    @Test
    void loadPluginWithClassNotFoundThrowsException() throws Exception {
        final File jarFile = createTestJar();
        
        TestPropertyValues.of("plugin.enabled=true", "plugin.path=" + jarFile.getParent()).applyTo(applicationContext);

        pluginLoader.postProcessBeanFactory(null);

        assertThrows(IllegalArgumentException.class, 
            () -> pluginLoader.loadPlugin("com.nonexistent.Plugin"));
    }

    @Test
    void loadPluginWithClassNameHavingWhitespaceTrimsAndLoads() throws Exception {
        final File jarFile = createTestJar();
        
        TestPropertyValues.of("plugin.enabled=true", "plugin.path=" + jarFile.getParent()).applyTo(applicationContext);

        pluginLoader.postProcessBeanFactory(null);

        final Object result = pluginLoader.loadPlugin("  java.lang.String  ");

        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sanitizeWithNullListReturnsEmptyList() {
        final List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(
            pluginLoader, "sanitize", (List<String>) null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sanitizeWithEmptyListReturnsEmptyList() {
        final List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(
            pluginLoader, "sanitize", Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sanitizeWithValidStringsReturnsCleanedList() {
        final List<String> input = Arrays.asList("  test1  ", "test2", "  test1  ", null, "", "  ");
        final List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(
            pluginLoader, "sanitize", input);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("test1"));
        assertTrue(result.contains("test2"));
        assertFalse(result.contains(null));
        assertFalse(result.contains(""));
    }

    @Test
    void destroyWithoutClassLoaderHandlesGracefully() {
        try {
            pluginLoader.destroy();
        } catch (final Exception e) {
            Assertions.fail("Destroy should not throw exception", e);
        }
    }

    @Test
    void destroyWithClassLoaderClosesSuccessfully() throws Exception {
        final File jarFile = createTestJar();
        
        TestPropertyValues.of("plugin.enabled=true", "plugin.path=" + jarFile.getParent()).applyTo(applicationContext);

        try {
            pluginLoader.postProcessBeanFactory(null);
            pluginLoader.destroy();
        } catch (final Exception e) {
            Assertions.fail("Destroy should not throw exception", e);
        }
        
        // Should complete without exception
    }

    @Test
    void ensurePluginInfrastructureCalledMultipleTimesInitializesOnce() throws Exception {
        final File jarFile = createTestJar();
        
        TestPropertyValues.of("plugin.enabled=true", "plugin.path=" + jarFile.getParent()).applyTo(applicationContext);

        // Call multiple times
        try {
            pluginLoader.postProcessBeanFactory(null);
            pluginLoader.postProcessBeanFactory(null);
            pluginLoader.postProcessBeanFactory(null);
            // Should not throw exception
        } catch (final Exception e) {
            Assertions.fail("Destroy should not throw exception", e);
        }
        
    }

    @Test
    void initializeWithInvalidPathHandlesGracefully() {
        TestPropertyValues.of("plugin.enabled=true", "plugin.path=/nonexistent/path/to/jars", 
                "plugin.classes=com.example.Plugin").applyTo(applicationContext);

        try {
            pluginLoader.postProcessBeanFactory(null);
        } catch (final Exception e) {
            Assertions.fail("Initialize should not throw exception", e);
        }
    }

    @Test
    void initializeWithWhitespaceInClassListSanitizesCorrectly() {
        // Set environment properties before calling postProcessBeanFactory
        TestPropertyValues.of("plugin.enabled=true", "plugin.path=/some/path", 
                "plugin.classes=  com.example.Plugin1  ,,com.example.Plugin2,  ,", 
                "plugin.packages=").applyTo(applicationContext);
        
        // Refresh context to pick up system properties
        applicationContext.getEnvironment().getSystemProperties();

        pluginLoader.postProcessBeanFactory(null);

        @SuppressWarnings("unchecked")
        final List<String> sanitizedClasses = (List<String>) ReflectionTestUtils.getField(
            pluginLoader, "pluginJarClasses");
        
        assertNotNull(sanitizedClasses);
        assertEquals(2, sanitizedClasses.size());
        assertTrue(sanitizedClasses.contains("com.example.Plugin1"));
        assertTrue(sanitizedClasses.contains("com.example.Plugin2"));
    }

    /**
     * Helper method to create a test JAR file.
     *
     * @return the created JAR file
     * @throws Exception if creation fails
     */
    private File createTestJar() throws Exception {
        final File jarFile = new File(tempDir.toFile(), "test-plugin.jar");
        
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            final JarEntry entry = new JarEntry("META-INF/MANIFEST.MF");
            jos.putNextEntry(entry);
            jos.write("Manifest-Version: 1.0\n".getBytes());
            jos.closeEntry();
        }
        
        return jarFile;
    }
}
