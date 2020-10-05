/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler;

import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithDefaults;

import com.google.j2cl.transpiler.TranspilerTester.TranspileResult;
import junit.framework.TestCase;

/** Test to run the transpiler twice and possibly uncover static state within the transpiler. */
public class RerunningJ2clTranspilerTest extends TestCase {

  public void testCompileJreTwice() throws Exception {
    compileJre().assertOutputFilesAreSame(compileJre());
  }

  private static TranspileResult compileJre() throws Exception {
    return newTesterWithDefaults()
        .setNativeSourcePathArg("transpiler/javatests/com/google/j2cl/transpiler/libjre_native.jar")
        .addSourcePathArg(
            "transpiler/javatests/com/google/j2cl/transpiler/jre_bundle_deploy-src.jar")
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }
}
