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

/** Tests for the -nativesources flag error messages. */
public class NativeSourcesErrorTest extends IntegrationTestCase {
  private static final String PACKAGE_NAME =
      "com/google/j2cl/transpiler/integration/nativesourceserror/";
  private static final String NATIVE_SOURCES_ERROR_PATH =
      "third_party/java_src/j2cl/transpiler/javatests/" + PACKAGE_NAME;

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
            "-cp",
            JRE_PATH,
            "-nativesourcepath",
            NATIVE_SOURCES_ERROR_PATH + "bad_name_native_sources.zip");
    assertErrorsContainsSnippet(
        transpileResult.getProblems(),
        "Cannot find matching native file '"
            + PACKAGE_NAME
            + "NativeClass"
            + NativeJavaScriptFile.NATIVE_EXTENSION
            + "'.");
  }

  /**
   * too_many_native_sources.zip contains:
   * com/google/j2cl/transpiler/integration/nativesourceserror/NativeClass.native.js
   * com/google/j2cl/transpiler/integration/nativesourceserror/ExtraClass.native.js
   *
   * <p>The compilation fails because there is no ExtraClass.java so not all the native sources are
   * used.
   */
  public void testUnusedSource() throws IOException, InterruptedException {
    TranspileResult transpileResult =
        transpileDirectory(
            "nativesourceserror",
            "-cp",
            JRE_PATH,
            "-nativesourcepath",
            NATIVE_SOURCES_ERROR_PATH + "too_many_native_sources.zip");
    assertErrorsContainsSnippet(
        transpileResult.getProblems(),
        "Native JavaScript file '"
            + NATIVE_SOURCES_ERROR_PATH
            + "too_many_native_sources.zip!/"
            + PACKAGE_NAME
            + "ExtraClass.native.js"
            + "' not used.");
  }
}
