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
package com.google.j2cl.junit.integration.junit4;

import static org.junit.Assume.assumeFalse;

import com.google.j2cl.junit.integration.IntegrationTestBase;
import com.google.j2cl.junit.integration.TestResult;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration test for j2kt JUnit4 support. */
@RunWith(Parameterized.class)
public class J2ktJUnit4IntegrationTest extends IntegrationTestBase {

  private interface TestSequencer {
    String[] forTest(String name);
  }

  @Before
  public void assumeNonWeb() {
    assumeFalse(testMode.isWeb());
  }

  @Test
  public void testPassingTest() throws Exception {
    String testName = "SimplePassingTest";
    TestResult testResult = createTestResultForSimplePassingTest(testName);

    List<String> logLines = runTest(testName);

    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testIgnore() throws Exception {
    String testName = "IgnoreTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("testOverriddenWithTest")
            .addTestSuccess("testOverriddenWithoutTest")
            .addTestSuccess("testOverriddenWithIgnoreButNoTest")
            .build();

    List<String> logLines = runTest(testName);

    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testFailingTest() throws Exception {
    String testName = "SimpleFailingTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("test")
            .addTestFailure("testOther")
            .addJavaLogLineSequence("test")
            .addJavaLogLineSequence("testOther")
            .build();

    List<String> logLines = runTest(testName);

    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testSimpleSuite() throws Exception {
    String testName = "SimpleSuite";
    TestResult testResult = createTestResultForSimplePassingTest(testName);

    List<String> logLines = runTest(testName);

    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testSuiteOfSuite() throws Exception {
    String testName = "SuiteOfSuite";
    TestResult testResult = createTestResultForSimplePassingTest(testName);

    List<String> logLines = runTest(testName);

    assertThat(logLines).matches(testResult);
  }

  private TestResult createTestResultForSimplePassingTest(String suiteName) {
    return newTestResultBuilder()
        .testClassName(suiteName)
        .addTestSuccess("test")
        .addTestSuccess("testOther")
        .addTestSuccess("validTestMethodWithoutPrefix")
        .addJavaLogLineSequence("test")
        .addJavaLogLineSequence("testOther")
        .addJavaLogLineSequence("validTestMethodWithoutPrefix")
        .addBlackListedWord("should_not_be_in_log")
        .build();
  }

  @Test
  public void testBeforeAndAfter() throws Exception {
    String testName = "BeforeAndAfterTest";
    TestSequencer testSequence = t -> new String[] {"constructor", "setUp", t, "tearDown"};
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("test1")
            .addTestSuccess("test2")
            .addJavaLogLineSequence(testSequence.forTest("test"))
            .addJavaLogLineSequence(testSequence.forTest("test"))
            .addJavaLogLineSequence("afterClass")
            .build();

    List<String> logLines = runTest(testName);

    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testThrowsOnConstruction() throws Exception {
    String testName = "ThrowsOnConstructionTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("b")
            .addTestSuccess("a")
            .addTestSuccess("c")
            .addJavaLogLineSequence("constructor", "a", "after")
            .addJavaLogLineSequence("constructor")
            .addJavaLogLineSequence("constructor", "c", "after")
            .addJavaLogLineSequence("afterClass")
            .addBlackListedWord("should_not_be_in_log")
            .build();

    List<String> logLines = runTest(testName);

    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testThrowsInBeforeClass() throws Exception {
    String testName = "ThrowsInBeforeClassTest";
    TestResult testResult = newTestResultBuilder().testClassName(testName).build();
    if (testMode.isJvm()) {
      // In JUnit 4, if there is exception in BeforeClass, the log will show 0 test run regardless
      // of failures number.
      testResult =
          newTestResultBuilder()
              .testClassName(testName)
              .failedToInstantiateTest(true)
              .addJavaLogLineSequence("afterClass")
              .addBlackListedWord("should_not_be_in_log")
              .build();
    } else {
      testResult =
          newTestResultBuilder()
              .testClassName(testName)
              .addTestFailure("test")
              .addJavaLogLineSequence("afterClass")
              .addBlackListedWord("should_not_be_in_log")
              .build();
    }

    List<String> logLines = runTest(testName);

    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testThrowsInBefore() throws Exception {
    String testName = "ThrowsInBeforeTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("test")
            .addTestFailure("testOther")
            .addJavaLogLineSequence("before", "after", "before", "after")
            .addBlackListedWord("should_not_be_in_log")
            .addJavaLogLineSequence("afterClass")
            .build();

    List<String> logLines = runTest(testName);

    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testThrowsInAfter() throws Exception {
    String testName = "ThrowsInAfterTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("testOther")
            .addTestFailure("test")
            .addJavaLogLineSequence("test", "after")
            .addJavaLogLineSequence("testOther", "after")
            .addJavaLogLineSequence("afterClass")
            .build();

    List<String> logLines = runTest(testName);

    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testExpectedException() throws Exception {
    String testName = "ExpectedExceptionTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("testShouldSucceed")
            .addTestFailure("testShouldFail")
            .addTestFailure("testShouldFail1")
            .addJavaLogLineSequence("testShouldFail")
            .addJavaLogLineSequence("testShouldFail1")
            .addJavaLogLineSequence("testShouldSucceed")
            .build();

    List<String> logLines = runTest(testName);

    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testAssumption() throws Exception {
    String testName = "AssumptionTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSkip("testAssumptionFails")
            .addJavaLogLineSequence("setUp", "before assumption fails", "tearDown")
            .addBlackListedWord("should_not_be_in_log")
            .addTestSuccess("testAssumptionPasses")
            .addJavaLogLineSequence("setUp", "after assumption passes", "tearDown")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testAssumptionBefore() throws Exception {
    String testName = "AssumptionBeforeTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSkip("testShouldNotRun")
            .addJavaLogLineSequence("setUp", "tearDown")
            .addBlackListedWord("should_not_be_in_log")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testAssumptionBeforeClass() throws Exception {
    String testName = "AssumptionBeforeClassTest";
    var testResultBuilder = newTestResultBuilder().testClassName(testName);
    // On the JVM, short-circuiting in @BeforeClass causes the entire test to pass, but it will list
    // zero run.
    if (!testMode.isJvm()) {
      testResultBuilder.addTestSkip("testShouldNotRun");
    }
    TestResult testResult =
        testResultBuilder
            .addBlackListedWord("should_not_be_in_log")
            .addJavaLogLineSequence("beforeClass ran", "afterClass")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }
}
