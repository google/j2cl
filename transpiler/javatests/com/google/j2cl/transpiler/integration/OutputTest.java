/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.integration;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

/**
 * Tests that expected output is created.
 */
public class OutputTest extends IntegrationTestCase {
  public void testOutputDir() throws IOException, InterruptedException {
    TranspileResult transpileResult =
        transpileDirectory(
            "output",
            OutputType.DIR,
            "-source",
            "1.8",
            "-encoding",
            "UTF-8",
            "-cp",
            "third_party/java_src/j2cl/jre/java/libJavaJre.jar");

    assertEquals(0, transpileResult.exitCode);
    assertTrue(transpileResult.errorLines.isEmpty());
    assertTrue(transpileResult.outputLocation.exists());

    assertTrue(
        new File(
                transpileResult.outputLocation,
                "com/google/j2cl/transpiler/integration/output/Foo.js")
            .exists());
    assertTrue(
        new File(
                transpileResult.outputLocation,
                "com/google/j2cl/transpiler/integration/output/Foo.impl.js")
            .exists());
    assertTrue(
        new File(
                transpileResult.outputLocation,
                "com/google/j2cl/transpiler/integration/output/Bar.js")
            .exists());
    assertTrue(
        new File(
                transpileResult.outputLocation,
                "com/google/j2cl/transpiler/integration/output/Bar.impl.js")
            .exists());
    assertFalse(new File(transpileResult.outputLocation, "some/thing/Bogus.js").exists());
  }

  public void testOutputZip() throws IOException, InterruptedException {
    TranspileResult transpileResult =
        transpileDirectory(
            "output",
            OutputType.ZIP,
            "-source",
            "1.8",
            "-encoding",
            "UTF-8",
            "-cp",
            "third_party/java_src/j2cl/jre/java/libJavaJre.jar");

    assertEquals(0, transpileResult.exitCode);
    assertTrue(transpileResult.errorLines.isEmpty());
    assertTrue(transpileResult.outputLocation.exists());

    try (ZipFile zipFile = new ZipFile(transpileResult.outputLocation)) {
      assertNotNull(zipFile.getEntry("com/google/j2cl/transpiler/integration/output/Foo.js"));
      assertNotNull(zipFile.getEntry("com/google/j2cl/transpiler/integration/output/Foo.impl.js"));
      assertNotNull(zipFile.getEntry("com/google/j2cl/transpiler/integration/output/Bar.js"));
      assertNotNull(zipFile.getEntry("com/google/j2cl/transpiler/integration/output/Bar.impl.js"));
      assertNull(zipFile.getEntry("some/thing/Bogus.js"));
    }
  }
}
