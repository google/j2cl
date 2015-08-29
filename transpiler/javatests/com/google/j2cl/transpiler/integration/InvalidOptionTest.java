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

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

/**
 * Tests that invalid flag options are reported.
 */
public class InvalidOptionTest extends IntegrationTestCase {
  public void testInvalidOption() throws IOException, InterruptedException {
    String[] args =
        new String[] {IntegrationTestCase.TRANSPILER_BINARY, "-source", "2.0", "-encoding", "abc"};
    TranspileResult transpileResult = transpile(args);
    assertLogContainsSnippet(transpileResult.errorLines, "invalid source version: 2.0");
    assertLogContainsSnippet(transpileResult.errorLines, "unsupported encoding: abc");
  }

  public void testInvalidOutputOption() throws IOException, InterruptedException {
    String[] extraArgs = {"-source", "1.8", "-encoding", "UTF-8"};

    // Output to a location that is not a directory and is not a zip file.
    File outputLocation = Files.createTempDir();
    outputLocation = new File(outputLocation, "Foo.txt");
    outputLocation.createNewFile();

    // Run the transpile
    String[] transpileArgs =
        IntegrationTestCase.getTranspileArgs(
            outputLocation, this.getClass(), "invalidoutputoption", extraArgs);
    TranspileResult transpileResult = transpile(transpileArgs, outputLocation);

    // Verify that the output location was rejected.
    assertLogContainsSnippet(
        transpileResult.errorLines, "-output location must be a directory or .zip file");
  }
}
