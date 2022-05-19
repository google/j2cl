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

import com.google.j2cl.junit.integration.IntegrationTestBase;
import com.google.j2cl.junit.integration.TestResult;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration test for j2cl JUnit4 support. */
@RunWith(Parameterized.class)
public class JUnit4IntegrationTest2 extends IntegrationTestBase {

  @Test
  public void testPassingTest() throws Exception {
    String testName = "SimplePassingTest";
    TestResult testResult = createTestResultForSimplePassingTest(testName);

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testSuite() throws Exception {
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
        .addJavaLogLineSequence("test", "testOther", "validTestMethodWithoutPrefix")
        .addBlackListedWord("should_not_be_in_log")
        .build();
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
  public void testThrowsOnConstruction() throws Exception {
    String testName = "ThrowsOnConstructionTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("b")
            .addTestSuccess("a")
            .addTestSuccess("c")
            .addJavaLogLineSequence(
                "constructor", "a", "after", "constructor", "constructor", "c", "after")
            .addBlackListedWord("should_not_be_in_log")
            .build();

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
            .addTestSuccess("testOther")
            .addJavaLogLineSequence("before", "after", "before", "testOther", "after")
            .addBlackListedWord("should_not_be_in_log")
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
            .addTestFailure("test")
            .addTestSuccess("testOther")
            .addJavaLogLineSequence("test", "after", "testOther", "after")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }
}
