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

/** Tests output flag. */
public class OutputTest extends IntegrationTestCase {

  public void testOutputDir() throws IOException, InterruptedException {
    TranspileResult transpileResult = transpileDirectory("output", "-cp", JRE_PATH);

    assertEquals(0, transpileResult.getExitCode());
    assertFalse(transpileResult.getProblems().hasProblems());
    File outputLocation = transpileResult.getOutputLocation();

    assertTrue(outputLocation.exists());

    assertFileExists(outputLocation, "com/google/j2cl/transpiler/integration/output/Foo.java.js");
    assertFileExists(
        outputLocation, "com/google/j2cl/transpiler/integration/output/Foo.impl.java.js");
    assertFileExists(outputLocation, "com/google/j2cl/transpiler/integration/output/Bar.java.js");
    assertFileExists(
        outputLocation, "com/google/j2cl/transpiler/integration/output/Bar.impl.java.js");
    assertFileDoesNotExist(outputLocation, "some/thing/Bogus.js");
  }

  public void testOutputZip() throws IOException, InterruptedException {
    File outputLocation = File.createTempFile("output", ".zip");
    TranspileResult transpileResult = transpileDirectory("output", outputLocation, "-cp", JRE_PATH);

    assertEquals(0, transpileResult.getExitCode());
    assertFalse(transpileResult.getProblems().hasErrors());
    assertTrue(transpileResult.getOutputLocation().exists());

    try (ZipFile zipFile = new ZipFile(outputLocation)) {
      assertNotNull(zipFile.getEntry("com/google/j2cl/transpiler/integration/output/Foo.java.js"));
      assertNotNull(
          zipFile.getEntry("com/google/j2cl/transpiler/integration/output/Foo.impl.java.js"));
      assertNotNull(zipFile.getEntry("com/google/j2cl/transpiler/integration/output/Bar.java.js"));
      assertNotNull(
          zipFile.getEntry("com/google/j2cl/transpiler/integration/output/Bar.impl.java.js"));
      assertNull(zipFile.getEntry("some/thing/Bogus.js"));
    }
  }

  public void testOutputInvalidFile() throws IOException {
    // Output to a location that is not a directory and is not a zip file.
    File outputLocation = File.createTempFile("foo", ".txt");

    // Run the transpile
    TranspileResult transpileResult = transpileDirectory("output", outputLocation);

    // Verify that the output location was rejected.
    assertErrorsContainsSnippet(
        transpileResult.getProblems(),
        "Output location '" + outputLocation + "' must be a directory or .zip file");
  }

  private static void assertFileExists(File outputPath, String fileName) {
    assertTrue(new File(outputPath, fileName).exists());
  }

  private static void assertFileDoesNotExist(File outputPath, String fileName) {
    assertFalse(new File(outputPath, fileName).exists());
  }
}
