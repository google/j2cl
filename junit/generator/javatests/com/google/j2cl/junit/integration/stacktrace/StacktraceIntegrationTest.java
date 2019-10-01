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

import com.google.j2cl.junit.integration.IntegrationTestBase;
import com.google.j2cl.junit.integration.Stacktrace;
import com.google.j2cl.junit.integration.TestResult;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration test for stack trace deobfuscation */
@RunWith(Parameterized.class)
public class StacktraceIntegrationTest extends IntegrationTestBase {

  @Test
  public void testSimpleThrowingMethod() throws Exception {
    runStacktraceTest("SimpleThrowingStacktraceTest");
  }

  @Test
  public void testRecursive() throws Exception {
    runStacktraceTest("RecursiveStacktraceTest");
  }

  @Test
  public void testNative() throws Exception {
    if (!testMode.isJ2cl()) {
      // test contains native js code and can't be run in pure Java
      return;
    }
    runStacktraceTest("NativeStacktraceTest");
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
    if (testMode == TestMode.JAVA) {
      // In Java we get a different stack trace which contains a ExceptionInInitializerError
      // Java synthesizes a try catch around field initializations and keeps the
      // original exception as the cause of the ExceptionInInitializerError exception.
      return;
    }

    runStacktraceTest("ThrowsInClassInitializer");
  }

  @Test
  public void testThrowsInBridgeMethod() throws Exception {
    runStacktraceTest("ThrowsInBridgeMethod");
  }

  @Test
  public void testThrowsInNativeJs() throws Exception {
    if (testMode == TestMode.JAVA) {
      // uses native methods which wont work in Java
      return;
    }
    runStacktraceTest("ThrowsInNativeJs");
  }

  @Test
  public void testThrowsInJsFunction() throws Exception {
    runStacktraceTest("ThrowsInJsFunction");
  }

  @Test
  public void testFillInStackTrace() throws Exception {
    runStacktraceTest("FillInStacktraceTest");
  }

  private void runStacktraceTest(String testName) throws Exception {
    TestResult testResult =
        newTestResultBuilder().testClassName(testName).addTestFailure("test").build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);

    Stacktrace stacktrace = loadStackTrace(testName);
    assertThat(logLines).matches(stacktrace);
  }
}
