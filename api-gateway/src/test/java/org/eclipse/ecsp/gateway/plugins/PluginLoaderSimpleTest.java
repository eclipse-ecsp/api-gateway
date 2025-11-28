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
    void constructor_WithValidContext_InitializesSuccessfully() {
        assertNotNull(pluginLoader);
    }

    @Test
    void initialize_WithPluginDisabled_DoesNotThrowException() {
        ReflectionTestUtils.setField(pluginLoader, "pluginEnabled", false);
        ReflectionTestUtils.setField(pluginLoader, "pluginJarPath", "/some/path");

        try {
            pluginLoader.postProcessBeanFactory(null);
        } catch (Exception e) {
            fail("Initialize should not throw exception when plugins are disabled", e);
        }
    }

    @Test
    void initialize_WithPluginEnabledButNoPath_DoesNotThrowException() {
        ReflectionTestUtils.setField(pluginLoader, "pluginEnabled", true);
        ReflectionTestUtils.setField(pluginLoader, "pluginJarPath", "");

        try {
            pluginLoader.postProcessBeanFactory(null);
        } catch (Exception e) {
            fail("Initialize should not throw exception when plugins are disabled", e);
        }
    }

    @Test
    void initialize_WithPluginEnabledAndValidPath_InitializesSuccessfully() throws Exception {
        final File jarFile = createTestJar();
        
        ReflectionTestUtils.setField(pluginLoader, "pluginEnabled", true);
        ReflectionTestUtils.setField(pluginLoader, "pluginJarPath", jarFile.getParent());
        ReflectionTestUtils.setField(pluginLoader, "pluginJarClasses", Collections.emptyList());
        ReflectionTestUtils.setField(pluginLoader, "pluginPackages", Collections.emptyList());

        try {
            pluginLoader.postProcessBeanFactory(null);
        } catch (Exception e) {
            fail("Initialize should not throw exception when plugins are disabled", e);
        }

        assertNotNull(applicationContext.getClassLoader());
    }

    @Test
    void loadPlugins_WithNoClasses_ReturnsEmptyList() {
        ReflectionTestUtils.setField(pluginLoader, "pluginJarClasses", Collections.emptyList());

        final List<Object> plugins = pluginLoader.loadPlugins();

        assertNotNull(plugins);
        assertTrue(plugins.isEmpty());
    }

    @Test
    void loadPlugins_WithNullClasses_ReturnsEmptyList() {
        ReflectionTestUtils.setField(pluginLoader, "pluginJarClasses", null);

        final List<Object> plugins = pluginLoader.loadPlugins();

        assertNotNull(plugins);
        assertTrue(plugins.isEmpty());
    }

    @Test
    void loadPlugin_WithBlankClassName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> pluginLoader.loadPlugin(""));
        assertThrows(IllegalArgumentException.class, () -> pluginLoader.loadPlugin("   "));
    }

    @Test
    void loadPlugin_WithNullClassName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> pluginLoader.loadPlugin(null));
    }

    @Test
    void loadPlugin_WithNoClassLoaderInitialized_ThrowsException() {
        ReflectionTestUtils.setField(pluginLoader, "pluginJarPath", "");
        ReflectionTestUtils.setField(pluginLoader, "initializationAttempted", false);

        assertThrows(IllegalStateException.class, 
            () -> pluginLoader.loadPlugin("java.lang.String"));
    }

    @Test
    void loadPlugin_WithValidClass_LoadsSuccessfully() throws Exception {
        final File jarFile = createTestJar();
        
        ReflectionTestUtils.setField(pluginLoader, "pluginEnabled", true);
        ReflectionTestUtils.setField(pluginLoader, "pluginJarPath", jarFile.getParent());
        ReflectionTestUtils.setField(pluginLoader, "pluginJarClasses", Collections.emptyList());
        ReflectionTestUtils.setField(pluginLoader, "pluginPackages", Collections.emptyList());

        pluginLoader.postProcessBeanFactory(null);

        final Object result = pluginLoader.loadPlugin("java.lang.String");

        assertNotNull(result);
        assertTrue(result instanceof String);
    }

    @Test
    void loadPlugin_WithClassNotFound_ThrowsException() throws Exception {
        final File jarFile = createTestJar();
        
        ReflectionTestUtils.setField(pluginLoader, "pluginEnabled", true);
        ReflectionTestUtils.setField(pluginLoader, "pluginJarPath", jarFile.getParent());
        ReflectionTestUtils.setField(pluginLoader, "pluginJarClasses", Collections.emptyList());
        ReflectionTestUtils.setField(pluginLoader, "pluginPackages", Collections.emptyList());

        pluginLoader.postProcessBeanFactory(null);

        assertThrows(IllegalArgumentException.class, 
            () -> pluginLoader.loadPlugin("com.nonexistent.Plugin"));
    }

    @Test
    void loadPlugin_WithClassNameHavingWhitespace_TrimsAndLoads() throws Exception {
        final File jarFile = createTestJar();
        
        ReflectionTestUtils.setField(pluginLoader, "pluginEnabled", true);
        ReflectionTestUtils.setField(pluginLoader, "pluginJarPath", jarFile.getParent());
        ReflectionTestUtils.setField(pluginLoader, "pluginJarClasses", Collections.emptyList());
        ReflectionTestUtils.setField(pluginLoader, "pluginPackages", Collections.emptyList());

        pluginLoader.postProcessBeanFactory(null);

        final Object result = pluginLoader.loadPlugin("  java.lang.String  ");

        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sanitize_WithNullList_ReturnsEmptyList() {
        final List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(
            pluginLoader, "sanitize", (List<String>) null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sanitize_WithEmptyList_ReturnsEmptyList() {
        final List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(
            pluginLoader, "sanitize", Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sanitize_WithValidStrings_ReturnsCleanedList() {
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
    void destroy_WithoutClassLoader_HandlesGracefully() {
        try {
            pluginLoader.destroy();
        } catch (final Exception e) {
            Assertions.fail("Destroy should not throw exception", e);
        }
    }

    @Test
    void destroy_WithClassLoader_ClosesSuccessfully() throws Exception {
        final File jarFile = createTestJar();
        
        ReflectionTestUtils.setField(pluginLoader, "pluginEnabled", true);
        ReflectionTestUtils.setField(pluginLoader, "pluginJarPath", jarFile.getParent());
        ReflectionTestUtils.setField(pluginLoader, "pluginJarClasses", Collections.emptyList());
        ReflectionTestUtils.setField(pluginLoader, "pluginPackages", Collections.emptyList());

        try {
            pluginLoader.postProcessBeanFactory(null);
            pluginLoader.destroy();
        } catch (final Exception e) {
            Assertions.fail("Destroy should not throw exception", e);
        }
        
        // Should complete without exception
    }

    @Test
    void ensurePluginInfrastructure_CalledMultipleTimes_InitializesOnce() throws Exception {
        final File jarFile = createTestJar();
        
        ReflectionTestUtils.setField(pluginLoader, "pluginEnabled", true);
        ReflectionTestUtils.setField(pluginLoader, "pluginJarPath", jarFile.getParent());
        ReflectionTestUtils.setField(pluginLoader, "pluginJarClasses", Collections.emptyList());
        ReflectionTestUtils.setField(pluginLoader, "pluginPackages", Collections.emptyList());

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
    void initialize_WithInvalidPath_HandlesGracefully() {
        ReflectionTestUtils.setField(pluginLoader, "pluginEnabled", true);
        ReflectionTestUtils.setField(pluginLoader, "pluginJarPath", "/nonexistent/path/to/jars");
        ReflectionTestUtils.setField(pluginLoader, "pluginJarClasses", Arrays.asList("com.example.Plugin"));
        ReflectionTestUtils.setField(pluginLoader, "pluginPackages", Collections.emptyList());

        try {
            pluginLoader.postProcessBeanFactory(null);
        } catch (final Exception e) {
            Assertions.fail("Initialize should not throw exception", e);
        }
    }

    @Test
    void initialize_WithWhitespaceInClassList_SanitizesCorrectly() {
        // Set environment properties before calling postProcessBeanFactory
        System.setProperty("plugin.enabled", "true");
        System.setProperty("plugin.path", "/some/path");
        System.setProperty("plugin.classes", "  com.example.Plugin1  ,,com.example.Plugin2,  ,");
        System.setProperty("plugin.packages", "");
        
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
        
        // Clean up system properties
        System.clearProperty("plugin.enabled");
        System.clearProperty("plugin.path");
        System.clearProperty("plugin.classes");
        System.clearProperty("plugin.packages");
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
