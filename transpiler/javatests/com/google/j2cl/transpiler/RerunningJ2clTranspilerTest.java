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
import java.util.Random;
import junit.framework.TestCase;

/** Test to run the transpiler twice and possibly uncover static state within the transpiler. */
public class RerunningJ2clTranspilerTest extends TestCase {

  public void testCompileJreTwice() throws Exception {
    compileJre().assertOutputFilesAreSame(compileJre());
  }

  private static final int JAVA_CONSERVATIVE_COMPILATION_TIME_MS = 1000;

  private static int getJavaCancelDelay() {
    // Random to introduce more variance in the timing instead of always targeting beginning.
    return new Random().nextInt(JAVA_CONSERVATIVE_COMPILATION_TIME_MS);
  }

  public void testCompileJreWithCancelation() throws Exception {
    createJreCompile().assertTranspileWithCancellation(getJavaCancelDelay());
    // Ensure compilation succeed after cancellation.
    compileJre().assertOutputFilesAreSame(compileJre());
    // Ensure compilation can be cancelled again.
    createJreCompile().assertTranspileWithCancellation(getJavaCancelDelay());
  }

  private static TranspileResult compileJre() throws Exception {
    return createJreCompile().assertTranspileSucceeds().assertNoWarnings();
  }

  private static TranspilerTester createJreCompile() {
    return newTesterWithDefaults()
        .setNativeSourcePathArg("transpiler/javatests/com/google/j2cl/transpiler/libjre_native.jar")
        .addSourcePathArg(
            "transpiler/javatests/com/google/j2cl/transpiler/jre_bundle_deploy-src.jar");
  }

  public void testCompileKotlinBox2DTwice() throws Exception {
    compileKotlinBox2D().assertOutputFilesAreSame(compileKotlinBox2D());
  }

  private static final int KOTLIN_CONSERVATIVE_COMPILATION_TIME_MS = 1000;

  private static int getKotlinCancelDelay() {
    // Random to introduce more variance in the timing instead of always targeting beginning.
    return new Random().nextInt(KOTLIN_CONSERVATIVE_COMPILATION_TIME_MS);
  }

  public void testCompileKotlinBox2DWithCancelation() throws Exception {
    createKotlinBox2DCompile().assertTranspileWithCancellation(getKotlinCancelDelay());
    // Ensure compilation succeed after cancellation.
    compileKotlinBox2D().assertOutputFilesAreSame(compileKotlinBox2D());
    // Ensure compilation can be cancelled again.
    createKotlinBox2DCompile().assertTranspileWithCancellation(getKotlinCancelDelay());
  }

  private static TranspileResult compileKotlinBox2D() throws Exception {
    return createKotlinBox2DCompile().assertTranspileSucceeds().assertNoWarnings();
  }

  private static TranspilerTester createKotlinBox2DCompile() {
    // Unlike for the java frontend test that use the JRE, we are not using Kotlin stdlib for
    // replicating the test for the Koltin frontend. Compiling stdlib is challenging due to the
    // complexity of stdlib compilation(special flags, builtins, etc.) and could require temporary
    // additional flag when we migrate  to new Kotlinc version.
    // We decided to use box2d instead that is complex enough for the purpose of this test.
    return newTesterWithKotlinDefaults()
        .addSourcePathArg("samples/box2d/src/main/kotlin/idiomatic/libbox2d_library-j2cl-src.jar");
  }
}
