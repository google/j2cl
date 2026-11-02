/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.junit.apt;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class VelocityRendererTest {
  private static final String PACKAGE_PATH = "com/google/j2cl/junit/apt/";

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void resourceSurvivesClosingAnotherProcessorClassLoader() throws Exception {
    URL jar = createJar(Map.of("template.vm", "hello")).toUri().toURL();
    try (URLClassLoader first = new URLClassLoader(new URL[] {jar}, null);
        URLClassLoader second = new URLClassLoader(new URL[] {jar}, null)) {
      try (InputStream firstStream = first.getResourceAsStream(PACKAGE_PATH + "template.vm")) {
        assertNotNull(firstStream);
        VelocityRenderer renderer =
            new VelocityRenderer(second.loadClass(VelocityRenderer.class.getName()));
        // Hold the reader open to reproduce one compilation finishing while another parses.
        var openResource = VelocityRenderer.class.getDeclaredMethod("openResource", String.class);
        openResource.setAccessible(true);
        try (Reader reader = (Reader) openResource.invoke(renderer, "template.vm")) {
          first.close();
          StringWriter output = new StringWriter();
          reader.transferTo(output);
          assertEquals("hello", output.toString());
        }
      }
    }
  }

  @Test
  public void rendersTemplatesAndIncludedResources() throws Exception {
    URL jar =
        createJar(Map.of("template.vm", "$greeting #parse('included.vm')", "included.vm", "world"))
            .toUri()
            .toURL();
    try (URLClassLoader loader = new URLClassLoader(new URL[] {jar}, null)) {
      VelocityRenderer renderer =
          new VelocityRenderer(loader.loadClass(VelocityRenderer.class.getName()));
      assertEquals(
          "hello world", renderer.renderTemplate("template.vm", Map.of("greeting", "hello")));
    }
  }

  @Test
  public void reportsMissingResource() {
    FileNotFoundException error =
        assertThrows(
            FileNotFoundException.class,
            () -> new VelocityRenderer(getClass()).renderTemplate("missing.vm", Map.of()));
    assertEquals("Could not find file: missing.vm", error.getMessage());
  }

  private Path createJar(Map<String, String> templates) throws Exception {
    Path jar = temporaryFolder.newFile("templates.jar").toPath();
    try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
      String classResource = PACKAGE_PATH + "VelocityRenderer.class";
      output.putNextEntry(new JarEntry(classResource));
      try (InputStream input = VelocityRenderer.class.getResourceAsStream("/" + classResource)) {
        assertNotNull(input);
        input.transferTo(output);
      }
      output.closeEntry();
      for (Map.Entry<String, String> template : templates.entrySet()) {
        output.putNextEntry(new JarEntry(PACKAGE_PATH + template.getKey()));
        output.write(template.getValue().getBytes(UTF_8));
        output.closeEntry();
      }
    }
    return jar;
  }
}
