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

import java.io.IOException;

/**
 * Tests source map integration in j2cl
 */
public class SourceMapTest extends IntegrationTestCase {
  /**
   * With the -outputSourceInfo flag, the traspiler outputs
   * "statement lineNumber,columnNumber,length" for each statement in the j2clast. We use this to
   * verify that the source locations are being stored properly.
   */
  public void testInputLocation() throws IOException, InterruptedException {
    TranspileResult transpileResult =
        transpileDirectory(
            "sourcemap",
            OutputType.DIR,
            "-source",
            "1.8",
            "-encoding",
            "UTF-8",
            "-cp",
            "third_party/java_src/j2cl/jre/java/libJavaJre_java_library.jar",
            "-printInputSourceInfo");
    assertLogContainsSnippet(
        transpileResult.outputLines,
        "com.google.j2cl.ast.VariableDeclarationStatement line:17 col:4 -> line:17 col:14");
    assertLogContainsSnippet(
        transpileResult.outputLines,
        "com.google.j2cl.ast.IfStatement line:18 col:4 -> line:20 col:5");
    assertLogContainsSnippet(
        transpileResult.outputLines, "com.google.j2cl.ast.Block line:18 col:15 -> line:20 col:5");
    assertLogContainsSnippet(
        transpileResult.outputLines,
        "com.google.j2cl.ast.ExpressionStatement line:19 col:6 -> line:19 col:10");
  }

  /**
   * Transpiles to:
   *
   *  58   m_method()
   *  59 {
   *  60 let i = 1;
   *  61 if (i < 2){
   *  62 i++;
   *  63 }
   *  64 }
   */
  public void testOutputLocation() throws IOException, InterruptedException {
    TranspileResult transpileResult =
        transpileDirectory(
            "sourcemap",
            OutputType.DIR,
            "-source",
            "1.8",
            "-encoding",
            "UTF-8",
            "-cp",
            "third_party/java_src/j2cl/jre/java/libJavaJre_java_library.jar",
            "-printOutputSourceInfo");
    assertLogContainsSnippet(
        transpileResult.outputLines,
        "com.google.j2cl.ast.VariableDeclarationStatement line:64 col:0 -> line:64 col:1");
    assertLogContainsSnippet(
        transpileResult.outputLines,
        "com.google.j2cl.ast.IfStatement line:65 col:0 -> line:65 col:10");
    assertLogContainsSnippet(
        transpileResult.outputLines,
        "com.google.j2cl.ast.ExpressionStatement line:66 col:0 -> line:66 col:4");
  }
}
