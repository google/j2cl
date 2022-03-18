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

import static org.junit.Assume.assumeFalse;

import com.google.j2cl.junit.integration.IntegrationTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration test for stack trace deobfuscation */
@RunWith(Parameterized.class)
public class StacktraceIntegration1Test extends IntegrationTestBase {

  @Before
  public void assumeNonJ2wasm() {
    // TODO(b/223396796): J2wasm does not support stacktrace test.
    assumeFalse(testMode.isJ2wasm());
  }

  @Test
  public void testSimpleThrowingMethod() throws Exception {
    runStacktraceTest("SimpleThrowingStacktraceTest");
  }

  @Test
  public void testCustomException() throws Exception {
    runStacktraceTest("CustomExceptionStacktraceTest");
  }

  @Test
  public void testJsException() throws Exception {
    runStacktraceTest("JsExceptionStacktraceTest");
  }

  @Test
  public void testJsExceptionNonJsConstructor() throws Exception {
    runStacktraceTest("JsExceptionNonJsConstructorStacktraceTest");
  }

  @Test
  public void testFillInStackTrace() throws Exception {
    runStacktraceTest("FillInStacktraceTest");
  }

  @Test
  public void testRecursive() throws Exception {
    runStacktraceTest("RecursiveStacktraceTest");
  }

  @Test
  public void testLambda() throws Exception {
    runStacktraceTest("LambdaStacktraceTest");
  }

  @Test
  public void testAnonymousClasses() throws Exception {
    runStacktraceTest("AnonymousClassesStacktraceTest");
  }

  @Test
  public void testThrowsInConstructor() throws Exception {
    runStacktraceTest("ThrowsInConstructorTest");
  }

  @Test
  public void testThrowsInJsConstructor() throws Exception {
    runStacktraceTest("ThrowsInJsConstructorTest");
  }

  @Test
  public void testThrowsInInstanceInitializer() throws Exception {
    runStacktraceTest("ThrowsInInstanceInitializer");
  }

  @Test
  public void testThrowsInClassInitializer() throws Exception {
    runStacktraceTest("ThrowsInClassInitializer");
  }

  @Test
  public void testThrowsInBridgeMethod() throws Exception {
    runStacktraceTest("ThrowsInBridgeMethod");
  }
}
