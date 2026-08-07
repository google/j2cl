/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.junit.integration.stacktrace;

import static org.junit.Assume.assumeTrue;

import com.google.j2cl.junit.integration.IntegrationTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration test for stack trace deobfuscation */
@RunWith(Parameterized.class)
public class StacktraceIntegration1Test extends IntegrationTestBase {

  @Test
  public void testAnonymousClasses() throws Exception {
    runStacktraceTest("AnonymousClassesStacktraceTest");
  }

  @Test
  public void testCustomException() throws Exception {
    runStacktraceTest("CustomExceptionStacktraceTest");
  }

  @Test
  public void testDuplicateFileName() throws Exception {
    runStacktraceTest("DuplicateFileNameStacktraceTest");
  }

  @Test
  public void testExceptionWithCause() throws Exception {
    runStacktraceTest("ExceptionWithCauseStacktraceTest");
  }

  @Test
  public void testFillInStackTrace() throws Exception {
    runStacktraceTest("FillInStacktraceTest");
  }

  @Test
  public void testJsExceptionNonJsConstructor() throws Exception {
    runStacktraceTest("JsExceptionNonJsConstructorStacktraceTest");
  }

  @Test
  public void testJsException() throws Exception {
    runStacktraceTest("JsExceptionStacktraceTest");
  }

  @Test
  public void testLambda() throws Exception {
    runStacktraceTest("LambdaStacktraceTest");
  }

  @Test
  public void testNative() throws Exception {
    // test contains native js code and can't be run in pure Java or Wasm,
    // this condition overlaps init but it is acceptable
    assumeTrue(testMode.isJ2cl());

    runStacktraceTest("NativeStacktraceTest");
  }
}
