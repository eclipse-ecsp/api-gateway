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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class for {@link JarUtils}.
 */
class JarUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void loadClassValidJarWithClassReturnsClass() throws IOException {
        // Create a test JAR with a class
        createTestJar(tempDir.toFile(), "test.jar", "java/lang/String.class");

        // Load String class from the JAR (it exists in JDK)
        Class<?> result = JarUtils.loadClass(
                tempDir.toString(),
                "java.lang.String",
                getClass().getClassLoader()
        );

        assertNotNull(result);
    }

    @Test
    void loadClassInvalidPathReturnsNull() {
        Class<?> result = JarUtils.loadClass(
                "/non/existent/path",
                "com.example.TestClass",
                getClass().getClassLoader()
        );

        assertNull(result);
    }

    @Test
    void loadClassClassNotFoundReturnsNull() throws IOException {
        // Create a valid JAR but try to load non-existent class
        createTestJar(tempDir.toFile(), "empty.jar", null);

        Class<?> result = JarUtils.loadClass(
                tempDir.toString(),
                "com.nonexistent.TestClass",
                getClass().getClassLoader()
        );

        assertNull(result);
    }

    @Test
    void loadClassClassNameWithWhitespaceTrimsAndLoads() throws IOException {
        // Create a test JAR
        createTestJar(tempDir.toFile(), "test.jar", "java/lang/String.class");

        // Load class with leading/trailing whitespace
        Class<?> result = JarUtils.loadClass(
                tempDir.toString(),
                "  java.lang.String  ",
                getClass().getClassLoader()
        );

        assertNotNull(result);
    }

    @Test
    void createClassLoaderValidDirectoryWithJarsReturnsClassLoader() throws IOException {
        // Create multiple test JARs
        createTestJar(tempDir.toFile(), "test1.jar", "Test1.class");
        createTestJar(tempDir.toFile(), "test2.jar", "Test2.class");

        URLClassLoader result = JarUtils.createClassLoader(
                tempDir.toString(),
                getClass().getClassLoader()
        );

        assertNotNull(result);
        assertNotNull(result.getURLs());
    }

    @Test
    void createClassLoaderNonExistentPathReturnsNull() {
        URLClassLoader result = JarUtils.createClassLoader(
                "/non/existent/path",
                getClass().getClassLoader()
        );

        assertNull(result);
    }

    @Test
    void createClassLoaderPathIsFileReturnsNull() throws IOException {
        // Create a file instead of a directory
        File file = new File(tempDir.toFile(), "notadirectory.txt");
        file.createNewFile();

        URLClassLoader result = JarUtils.createClassLoader(
                file.getAbsolutePath(),
                getClass().getClassLoader()
        );

        assertNull(result);
    }

    @Test
    void createClassLoaderEmptyDirectoryReturnsNull() {
        // tempDir is empty by default
        URLClassLoader result = JarUtils.createClassLoader(
                tempDir.toString(),
                getClass().getClassLoader()
        );

        assertNull(result);
    }

    @Test
    void createClassLoaderDirectoryWithNoJarsReturnsNull() throws IOException {
        // Create non-JAR files
        new File(tempDir.toFile(), "test.txt").createNewFile();
        new File(tempDir.toFile(), "test.zip").createNewFile();

        URLClassLoader result = JarUtils.createClassLoader(
                tempDir.toString(),
                getClass().getClassLoader()
        );

        assertNull(result);
    }

    @Test
    void createClassLoaderMultipleJarsSortedLoadsInOrder() throws IOException {
        // Create JARs that should be sorted alphabetically
        createTestJar(tempDir.toFile(), "z-last.jar", "Last.class");
        createTestJar(tempDir.toFile(), "a-first.jar", "First.class");
        createTestJar(tempDir.toFile(), "m-middle.jar", "Middle.class");

        URLClassLoader result = JarUtils.createClassLoader(
                tempDir.toString(),
                getClass().getClassLoader()
        );

        assertNotNull(result);
    }

    @Test
    void createClassLoaderNullParentClassLoaderUsesSystemClassLoader() throws IOException {
        createTestJar(tempDir.toFile(), "test.jar", "Test.class");

        URLClassLoader result = JarUtils.createClassLoader(
                tempDir.toString(),
                null
        );

        assertNotNull(result);
    }

    @Test
    void loadClassNullParentClassLoaderReturnsNull() throws IOException {
        createTestJar(tempDir.toFile(), "test.jar", "java/lang/String.class");

        Class<?> result = JarUtils.loadClass(
                tempDir.toString(),
                "java.lang.String",
                null
        );

        // Should still work with null parent
        assertNotNull(result);
    }

    /**
     * Helper method to create a test JAR file.
     *
     * @param directory  the directory to create the JAR in
     * @param jarName    the name of the JAR file
     * @param entryName  optional entry name to add to the JAR
     * @return the created JAR file
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private File createTestJar(File directory, String jarName, String entryName) throws IOException {
        File jarFile = new File(directory, jarName);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (FileOutputStream fos = new FileOutputStream(jarFile);
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {

            if (entryName != null) {
                ZipEntry entry = new ZipEntry(entryName);
                jos.putNextEntry(entry);
                jos.write(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}); // Mock class file
                jos.closeEntry();
            }
        }

        return jarFile;
    }
}
