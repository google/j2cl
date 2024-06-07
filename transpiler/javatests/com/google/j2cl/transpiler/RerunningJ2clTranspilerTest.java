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
import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithKotlinDefaults;

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

  public void testCompileKotlinStdLibTwice() throws Exception {
    // Unlike for the java frontend test that use the JRE, we are not using Kotlin stdlib for
    // replicating the test for the Koltin frontend. Compiling stdlib is challenging due to the
    // complexity of stdlib compilation(special flags, builtins, etc.) and could require temporary
    // additional flag when we migrate  to new Kotlinc version.
    // We decided to use box2d instead that is complex enough for the purpose of this test.
    compileKotlinBox2D().assertOutputFilesAreSame(compileKotlinBox2D());
  }

  private static TranspileResult compileKotlinBox2D() throws Exception {
    return newTesterWithKotlinDefaults()
        .addSourcePathArg(
            "samples/box2d/src/main/kotlin/idiomatic/libbox2d_kt_library-j2cl-src.jar")
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }
}
