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

/** Integration test for j2cl JUnit4 support. */
@RunWith(Parameterized.class)
public class JUnit4IntegrationTest1 extends IntegrationTestBase {

  private interface TestSequencer {
    String[] forTest(String name);
  }

  @Before
  public void assumeNonJ2kt() {
    assumeFalse(testMode.isJ2kt());
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
  public void testChild() throws Exception {
    String testName = "ChildTest";
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
  public void testChildWithNewMethods() throws Exception {
    String testName = "ChildWithNewMethodsTest";
    TestSequencer testSequence =
        t -> new String[] {"constructor", "setUp", "setUpChild", t, "tearDownChild", "tearDown"};
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("test1")
            .addTestSuccess("test2")
            .addTestSuccess("testChild1")
            .addTestSuccess("testChild2")
            .addJavaLogLineSequence(testSequence.forTest("testChild"))
            .addJavaLogLineSequence(testSequence.forTest("testChild"))
            .addJavaLogLineSequence(testSequence.forTest("test"))
            .addJavaLogLineSequence(testSequence.forTest("test"))
            .addJavaLogLineSequence("afterClass")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testGrandChildWithNewMethods() throws Exception {
    String testName = "GrandChildWithNewMethodsTest";
    TestSequencer testSequence =
        t ->
            new String[] {
              "constructor",
              "setUp",
              "grandChildSetUp",
              "setUpChildOverridden",
              t,
              "grandChildTearDown",
              "tearDownChildOverridden",
              "tearDown"
            };
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("test1")
            .addTestSuccess("test2")
            .addTestSuccess("testChild1")
            .addTestSuccess("testChild2")
            .addTestSuccess("testGrandChild1")
            .addTestSuccess("testGrandChild2")
            .addJavaLogLineSequence(testSequence.forTest("testGrandChild"))
            .addJavaLogLineSequence(testSequence.forTest("testGrandChild"))
            .addJavaLogLineSequence(testSequence.forTest("testGrandChild"))
            .addJavaLogLineSequence(testSequence.forTest("testChild"))
            .addJavaLogLineSequence(testSequence.forTest("test"))
            .addJavaLogLineSequence(testSequence.forTest("test"))
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
            .addJavaLogLineSequence("testShouldFail", "testShouldFail1", "testShouldSucceed")
            .build();

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
  public void testMethodOrdering() throws Exception {
    String testName = "MethodOrderingTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("a")
            .addTestSuccess("b")
            .addTestSuccess("b1")
            .addTestSuccess("c")
            .addTestSuccess("parent_a")
            .addTestSuccess("parent_b")
            .addTestSuccess("parent_b1")
            .addTestSuccess("parent_c")
            .addJavaLogLineSequence(
                "a", "b", "b1", "c", "parent_a", "parent_b", "parent_b1", "parent_c")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }
}
