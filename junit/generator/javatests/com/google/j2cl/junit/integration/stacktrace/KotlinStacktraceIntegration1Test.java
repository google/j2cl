/*
 * Copyright 2024 Google Inc.
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
public class KotlinStacktraceIntegration1Test extends IntegrationTestBase {

  @Before
  public void assumeNonJ2wasm() {
    // No kotlin to wasm tests
    assumeFalse(testMode.isJ2wasm());
  }

  @Test
  public void testKotlinAnonymousClasses() throws Exception {
    runStacktraceTest("KotlinAnonymousClassesStacktraceTest");
  }

  @Test
  public void testKotlinSimpleThrowingMethod() throws Exception {
    runStacktraceTest("SimpleKotlinThrowingStacktraceTest");
  }

  @Test
  public void testKotlinCustomException() throws Exception {
    runStacktraceTest("KotlinCustomExceptionStacktraceTest");
  }

  @Test
  public void testKotlinJsExceptionNonJsConstructor() throws Exception {
    runStacktraceTest("KotlinJsExceptionNonJsConstructorStacktraceTest");
  }

  @Test
  public void testKotlinFillInStackTrace() throws Exception {
    runStacktraceTest("KotlinFillInStacktraceTest");
  }

  @Test
  public void testKotlinJsException() throws Exception {
    runStacktraceTest("KotlinJsExceptionStacktraceTest");
  }

  @Test
  public void testKotlinLambda() throws Exception {
    runStacktraceTest("KotlinLambdaStacktraceTest");
  }

  @Test
  public void testKotlinRecursive() throws Exception {
    runStacktraceTest("KotlinRecursiveStacktraceTest");
  }

  @Test
  public void testKotlinThrowsInInstanceInitializer() throws Exception {
    runStacktraceTest("KotlinThrowsInInstanceInitializer");
  }

  @Test
  public void testKotlinThrowsInConstructor() throws Exception {
    runStacktraceTest("KotlinThrowsInConstructorTest");
  }

  @Test
  public void testKotlinThrowsInClassInitializer() throws Exception {
    runStacktraceTest("KotlinThrowsInClassInitializer");
  }

  @Test
  public void testKotlinThrowsInJsConstructor() throws Exception {
    runStacktraceTest("KotlinThrowsInJsConstructorTest");
  }

  @Test
  public void testKotlinThrowsInBridgeMethod() throws Exception {
    runStacktraceTest("KotlinThrowsInBridgeMethod");
  }
}
