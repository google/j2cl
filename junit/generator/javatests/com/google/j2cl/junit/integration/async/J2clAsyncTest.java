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

import static org.junit.Assume.assumeFalse;

import com.google.j2cl.junit.async.AsyncTestRunner;
import com.google.j2cl.junit.async.AsyncTestRunner.ErrorMessage;
import com.google.j2cl.junit.integration.IntegrationTestBase;
import com.google.j2cl.junit.integration.TestResult;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test for {@link com.google.j2cl.junit.async.AsyncTestRunner} and its j2cl counter part.
 *
 * <p>The test runs tests located in data/ and parses their command line output.
 *
 * <p>Test can be run with test_arg=--output_command_line to dumb the console output of tests that
 * ran to stdout.
 */
@RunWith(Parameterized.class)
public class J2clAsyncTest extends IntegrationTestBase {

  @Before
  public void assumeNonJ2wasm() {
    // TODO(b/223396796): J2wasm does not support async test.
    assumeFalse(testMode.isJ2wasm());
  }

  @Before
  public void assumeNonJ2kt() {
    assumeFalse(testMode.isJ2kt());
  }

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
  public void testAsyncAfterWillTimeOut() throws Exception {
    String testName = "TestAsyncAfterWillTimeOut";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("test")
            .addTestSuccess("testOther")
            .addJavaLogLineSequence("test", "after", "testOther", "after")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testAsyncBeforeWillTimeOut() throws Exception {
    String testName = "TestAsyncBeforeWillTimeOut";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("test")
            .addTestSuccess("testOther")
            .addJavaLogLineSequence("before", "before", "testOther")
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
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("test")
            .addJavaLogLineSequence("Before method ran!", "test method ran!", "After method ran!")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testResolvesAfterDelay() throws Exception {
    String testName = "TestResolvesAfterDelay";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("testResolvesAfterDelay1")
            .addTestSuccess("testResolvesAfterDelay2")
            .addTestSuccess("testResolvesAfterDelay3")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testMethodOrder() throws Exception {
    String testName = "TestMethodOrder";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("test")
            .addJavaLogLineSequence(
                "before3", "before2", "before1", "test", "after1", "after2", "after3")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testListenableFuture() throws Exception {
    // No j2cl version of this test since guava-j2cl is not available for open-source.
    assumeFalse(testMode.isJ2cl());

    String testName = "TestListenableFuture";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("testListenableFuture")
            .addTestFailure("testListenableFuture_fail")
            .addLogLineContains("java.lang.Exception: Should see this message")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testStructuralThenable() throws Exception {
    String testName = "TestStructuralThenable";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("testStructuraThenable")
            .addTestSuccess("testStructuraThenable_subinterface")
            .addTestSuccess("testStructuraThenable_subclass")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testFailingAsyncAfter() throws Exception {
    String testName = "TestFailingAsyncAfter";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("test")
            .addTestSuccess("testOther")
            .addJavaLogLineSequence("test", "after", "testOther", "after")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testFailingAsyncBefore() throws Exception {
    String testName = "TestFailingAsyncBefore";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("test")
            .addTestSuccess("testOther")
            .addJavaLogLineSequence("before", "before", "testOther")
            .addBlackListedWord("should_not_be_in_log")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testReturnTypeNotStructuralThenable() throws Exception {
    // No j2cl version of this test since these would be a compile error and thus are handled
    // in our APT unit tests
    assumeFalse(testMode.isJ2cl());

    String testName = "TestReturnTypeNotStructuralPromise";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("initializationError")
            // There should be exactly three of these errors.
            .addLogLineContains("1. " + ErrorMessage.NO_THEN_METHOD.format("java.lang.Object"))
            .addLogLineContains("2. " + ErrorMessage.NO_THEN_METHOD.format("java.lang.Object"))
            .addLogLineContains("3. " + ErrorMessage.NO_THEN_METHOD.format("java.lang.Object"))
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testReturnTypeNotStructuralThenable_thenParameterCount() throws Exception {
    // No j2cl version of this test since these would be a compile error and thus are handled
    // in our APT unit tests
    assumeFalse(testMode.isJ2cl());

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
  public void testReturnTypeNotStructuralThenable_thenParameterNotJsType() throws Exception {
    // No j2cl version of this test since these would be a compile error and thus are handled
    // in our APT unit tests
    assumeFalse(testMode.isJ2cl());

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
  public void testReturnsNullForAsyncAfter() throws Exception {
    String testName = "TestReturnsNullForAsyncAfter";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addJavaLogLineSequence("test method ran!")
            .addTestFailure("testMethod")
            .addLogLineContains(
                "java.lang.IllegalStateException: Test returned null as its promise")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testReturnsNullForAsyncBefore() throws Exception {
    String testName = "TestReturnsNullForAsyncBefore";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("testMethod")
            .addLogLineContains(
                "java.lang.IllegalStateException: Test returned null as its promise")
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
                    ? "Timed out while waiting for a promise returned from test_willTimeout"
                    // The exception that is thrown to indicate a timeout differs from JUnit 4.11 to
                    // JUnit 4.12. In JUnit 4.11 a java.lang.Exception is used, in JUnit 4.12 an
                    // org.junit.runners.model.TestTimedOutException is used. This works with both.
                    : "Exception: test timed out")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testWithExpectedException() throws Exception {
    // No j2cl version of this test since these would be a compile error and thus are handled
    // in our APT unit tests
    assumeFalse(testMode.isJ2cl());

    String testName = "TestWithExpectedException";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure(
                "initializationError",
                AsyncTestRunner.ErrorMessage.ASYNC_HAS_EXPECTED_EXCEPTION.format("test"))
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testTimeOutNotProvided() throws Exception {
    // No j2cl version of this test since these would be a compile error and thus are handled
    // in our APT unit tests
    assumeFalse(testMode.isJ2cl());

    String testName = "TestTimeOutNotProvided";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("initializationError")
            .addLogLineContains(
                AsyncTestRunner.ErrorMessage.ASYNC_HAS_NO_TIMEOUT.format("beforeThenable"))
            .addLogLineContains(
                AsyncTestRunner.ErrorMessage.ASYNC_HAS_NO_TIMEOUT.format("beforeFuture"))
            .addLogLineContains(
                AsyncTestRunner.ErrorMessage.ASYNC_HAS_NO_TIMEOUT.format("afterThenable"))
            .addLogLineContains(
                AsyncTestRunner.ErrorMessage.ASYNC_HAS_NO_TIMEOUT.format("afterFuture"))
            .addLogLineContains(
                AsyncTestRunner.ErrorMessage.ASYNC_HAS_NO_TIMEOUT.format(
                    "thenableDoesNotHaveTimeout"))
            .addLogLineContains(
                AsyncTestRunner.ErrorMessage.ASYNC_HAS_NO_TIMEOUT.format(
                    "futureDoesNotHaveTimeout"))
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }
}
