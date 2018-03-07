/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.junit.integration.async;

import com.google.j2cl.junit.async.J2clAsyncTestRunner;
import com.google.j2cl.junit.async.J2clAsyncTestRunner.ErrorMessage;
import com.google.j2cl.junit.integration.IntegrationTestBase;
import com.google.j2cl.junit.integration.TestResult;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test for {@link com.google.j2cl.junit.async.J2clAsyncTestRunner} and its j2cl counter part.
 *
 * <p>The test runs tests located in data/ and parses their command line output.
 *
 * <p>Test can be run with test_arg=--output_command_line to dumb the console output of tests that
 * ran to stdout.
 */
@RunWith(Parameterized.class)
public class J2clAsyncTest extends IntegrationTestBase {

  @Test
  public void testAfterWithFailingAsyncTest() throws Exception {
    String testName = "TestAfterWithFailingAsyncTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("testAfterWithFailingAsyncTest")
            .addJavaLogLineSequence(
                "Before method ran!", "test method ran!", "promise rejected!", "After method ran!")
            .addLogLineContains("java.lang.Exception: Should see this message")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testChainingWithException() throws Exception {
    String testName = "TestChainingWithException";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("testChainingWithException")
            .addLogLineContains("java.lang.RuntimeException: Should fail with this message")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testNonAsyncTest() throws Exception {
    String testName = "TestNonAsyncTest";
    TestResult testResult =
        newTestResultBuilder().testClassName(testName).addTestSuccess("test").build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testResolvesAfterDelay() throws Exception {
    String testName = "TestResolvesAfterDelay";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("testResolvesAfterDelay")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testReturnTypeNotStructualPromise() throws Exception {
    if (testMode.isJ2cl()) {
      // No j2cl version of this test since these would be a compile error and thus are handled
      // in our APT unit tests
      return;
    }

    String testName = "TestReturnTypeNotStructuralPromise";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure(
                "initializationError", ErrorMessage.NO_THEN_METHOD.format("java.lang.Object"))
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testReturnTypeNotStructualPromise_thenParameterCount() throws Exception {
    if (testMode.isJ2cl()) {
      // No j2cl version of this test since these would be a compile error and thus are handled
      // in our APT unit tests
      return;
    }

    String testName = "TestReturnTypeNotStructuralPromiseThenParameterCount";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure(
                "initializationError",
                ErrorMessage.NO_THEN_METHOD.format(
                    "com.google.j2cl.junit.integration.async.data."
                        + "TestReturnTypeNotStructuralPromiseThenParameterCount.Thenable"))
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testReturnTypeNotStructualPromise_thenParameterNotJsType() throws Exception {
    if (testMode.isJ2cl()) {
      // No j2cl version of this test since these would be a compile error and thus are handled
      // in our APT unit tests
      return;
    }

    String testName = "TestReturnTypeNotStructuralPromiseThenParameterNotJsType";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure(
                "initializationError",
                ErrorMessage.INVALID_CALLBACK_PARAMETER.format(
                    "com.google.j2cl.junit.integration.async.data."
                        + "TestReturnTypeNotStructuralPromiseThenParameterNotJsType.Thenable",
                    "second"))
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testReturnsNullForPromise() throws Exception {
    String testName = "TestReturnsNullForPromise";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("returnsNullForPromise")
            .addLogLineContains(
                "java.lang.IllegalStateException: Test returned null as its promise")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testWillTimeOut() throws Exception {
    String testName = "TestWillTimeOut";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("willTimeout")
            .addLogLineContains(
                testMode.isJ2cl()
                    ? "Timed out while waiting for a promise returned "
                        + "from test_willTimeout to resolve. "
                        + "Set goog.testing.TestCase.getActiveTestCase().promiseTimeout "
                        + "to adjust the timeout."
                    // The exception that is thrown to indicate a timeout differs from JUnit 4.11 to
                    // JUnit 4.12. In JUnit 4.11 a java.lang.Exception is used, in JUnit 4.12 an
                    // org.junit.runners.model.TestTimedOutException is used. This works with both.
                    : "Exception: test timed out after 10 milliseconds")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testWithExpectedException() throws Exception {
    if (testMode.isJ2cl()) {
      // No j2cl version of this test since these would be a compile error and thus are handled
      // in our APT unit tests
      return;
    }

    String testName = "TestWithExpectedException";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure(
                "initializationError",
                J2clAsyncTestRunner.ErrorMessage.EXPECTED_EXCEPTION.format("test"))
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testTimeOutNotProvided() throws Exception {
    if (testMode.isJ2cl()) {
      // No j2cl version of this test since these would be a compile error and thus are handled
      // in our APT unit tests
      return;
    }

    String testName = "TestTimeOutNotProvided";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure(
                "initializationError",
                J2clAsyncTestRunner.ErrorMessage.TIME_OUT_SET.format("doesNotHaveTimeout"))
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testWithLifeCycleMethodBeingAsync() throws Exception {
    if (testMode.isJ2cl()) {
      // No j2cl version of this test since these would be a compile error and thus are handled
      // in our APT unit tests
      return;
    }

    String testName = "TestWithLifeCycleMethodBeingAsync";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure(
                "initializationError", "java.lang.Exception: Method before() should be void")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }
}
