/*
 * Copyright 2026 Google LLC
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
package example;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.Test;

public class JavaOutputTest {
  @Test
  public void javaInfoPreservesProcessorResourcesForTranspiledBindings() throws Exception {
    try (var jar = new ZipFile(System.getProperty("java.output"))) {
      assertNotNull(jar.getEntry("example/GeneratedBinding.class"));
      assertEquals("body { color: red; }\n", read(jar, "example/style.css"));
      assertEquals(
          "{\"binding\":\"example.GeneratedBinding\"}", read(jar, "example/metadata.json"));
      assertEquals("// resource JavaScript\n", read(jar, "assets/bootstrap.js"));
      assertEquals("// resource native JavaScript\n", read(jar, "assets/bootstrap.native.js"));
    }
    Path javascript = Path.of(System.getProperty("javascript.output"));
    assertTrue(
        Files.readString(javascript.resolve("example/GeneratedBinding.impl.java.js"))
            .contains("example/style.css"));
    assertEquals(
        "// generated JavaScript\n", Files.readString(javascript.resolve("example/generated.js")));
    assertFalse(Files.exists(javascript.resolve("assets/bootstrap.js")));
    assertFalse(Files.exists(javascript.resolve("assets/bootstrap.native.js")));
  }

  private static String read(ZipFile jar, String name) throws Exception {
    assertNotNull(jar.getEntry(name));
    try (var stream = jar.getInputStream(jar.getEntry(name))) {
      return new String(stream.readAllBytes(), UTF_8);
    }
  }
}
