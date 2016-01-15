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

import com.google.j2cl.generator.NativeJavaScriptFile;

import java.io.IOException;

/**
 * Tests for the -nativesources flag error messages.
 */
public class NativeSourcesErrorTest extends IntegrationTestCase {
  private static final String PACKAGE_NAME =
      "com/google/j2cl/transpiler/integration/nativesourceserror/";
  private static final String NATIVE_SOURCES_ERROR_PATH =
      "third_party/java_src/j2cl/transpiler/javatests/" + PACKAGE_NAME;

  /**
   * Tries to load nonexistent.zip which doesn't exist.
   */
  public void testMissingZip() throws IOException, InterruptedException {
    TranspileResult transpileResult =
        transpileDirectory(
            "nativesourceserror",
            OutputType.DIR,
            "-source",
            "1.8",
            "-encoding",
            "UTF-8",
            "-cp",
            "third_party/java_src/j2cl/jre/java/libJavaJre_java_library.jar",
            "-nativesourcezip",
            NATIVE_SOURCES_ERROR_PATH + "nonexistent.zip");
    assertLogContainsSnippet(
        transpileResult.errorLines,
        "file not found: " + NATIVE_SOURCES_ERROR_PATH + "nonexistent.zip");
  }

  /**
   * bad_name_native_sources.zip contains:
   * com/google/j2cl/transpiler/integration/nativesourceserror/BadNameNativeClass.native.js
   *
   * <p>The compilation fails because NativeClass.java has no corresponding NativeClass.native.js
   */
  public void testNoMatchingSource() throws IOException, InterruptedException {
    TranspileResult transpileResult =
        transpileDirectory(
            "nativesourceserror",
            OutputType.DIR,
            "-source",
            "1.8",
            "-encoding",
            "UTF-8",
            "-cp",
            "third_party/java_src/j2cl/jre/java/libJavaJre_java_library.jar",
            "-nativesourcezip",
            NATIVE_SOURCES_ERROR_PATH + "bad_name_native_sources.zip");
    assertLogContainsSnippet(
        transpileResult.errorLines,
        "cannot find matching native file: "
            + PACKAGE_NAME + "NativeClass"
            + NativeJavaScriptFile.NATIVE_EXTENSION);
  }

  /**
   * too_many_native_sources.zip contains:
   * com/google/j2cl/transpiler/integration/nativesourceserror/NativeClass.native.js
   * com/google/j2cl/transpiler/integration/nativesourceserror/ExtraClass.native.js
   *
   * <p>The compilation fails because there is no ExtraClass.java so not all the native sources
   * are used.
   */
  public void testUnUsedSource() throws IOException, InterruptedException {
    TranspileResult transpileResult =
        transpileDirectory(
            "nativesourceserror",
            OutputType.DIR,
            "-source",
            "1.8",
            "-encoding",
            "UTF-8",
            "-cp",
            "third_party/java_src/j2cl/jre/java/libJavaJre_java_library.jar",
            "-nativesourcezip",
            NATIVE_SOURCES_ERROR_PATH + "too_many_native_sources.zip");
    assertLogContainsSnippet(
        transpileResult.errorLines,
        "native JavaScript file not used: "
            + NATIVE_SOURCES_ERROR_PATH
            + "too_many_native_sources.zip!/"
            + PACKAGE_NAME
            + "ExtraClass");
  }
}
