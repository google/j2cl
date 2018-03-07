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
package com.google.j2cl.junit.integration.junit3;

import com.google.j2cl.junit.integration.IntegrationTestBase;
import com.google.j2cl.junit.integration.TestResult;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration test for j2cl JUnit3 support. */
@RunWith(Parameterized.class)
public class JUnit3IntegrationTest extends IntegrationTestBase {

  @Test
  public void testMethodOrdering() throws Exception {
    String testName = "MethodOrderingTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("test_a")
            .addTestSuccess("test_b")
            .addTestSuccess("test_b1")
            .addTestSuccess("test_c")
            .addTestSuccess("test_parent_a")
            .addTestSuccess("test_parent_b")
            .addTestSuccess("test_parent_b1")
            .addTestSuccess("test_parent_c")
            .addJavaLogLineSequence(
                "a", "b", "b1", "c", "parent_a", "parent_b", "parent_b1", "parent_c")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testParentClass() throws Exception {
    String testName = "ParentMethodTest";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("test")
            .addTestSuccess("testOther")
            .addTestSuccess("testParent")
            .addTestSuccess("testParentOther")
            .addJavaLogLineSequence("test", "test", "parentTest", "parentTest")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testSetupAndTearDown() throws Exception {
    String testName = "SetupAndTearDownTest";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("test1")
            .addTestSuccess("test2")
            .addTestSuccess("test3")
            .addJavaLogLineSequence(
                "setup",
                "test",
                "tearDown",
                "setup",
                "test",
                "tearDown",
                "setup",
                "test",
                "tearDown")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testSimplePassingTest() throws Exception {
    String testName = "SimplePassingTest";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("test")
            .addTestSuccess("testOther")
            .addJavaLogLineSequence("test", "test")
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
            .addTestError("test", "java.lang.RuntimeException")
            .addTestSuccess("testOther")
            .addJavaLogLineSequence("test", "test")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testStaticMethod() throws Exception {
    String testName = "StaticMethodTest";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("test")
            .addJavaLogLineSequence("constructor", "test")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testThrowsInConstructor() throws Exception {
    if (!testMode.isJ2cl()) {
      // Junit3 generates a non-standard error in this case that is hard to assert. Like:
      // 1) warning(junit.framework.TestSuite$1)
      return;
    }

    String testName = "ThrowsInConstructorTest";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("testOther")
            .addTestFailure("test")
            .addJavaLogLineSequence("constructor", "constructor", "testOther")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testThrowsInSetup() throws Exception {
    String testName = "ThrowsInSetupTest";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("testOther")
            .addTestError("test", "java.lang.RuntimeException: first setup throws")
            .addJavaLogLineSequence("setup", "setup", "testOther")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testThrowsInTearDown() throws Exception {
    String testName = "ThrowsInTearDownTest";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestSuccess("testOther")
            .addTestError("test", "java.lang.RuntimeException: first tearDown throws")
            .addJavaLogLineSequence("test", "tearDown", "testOther", "tearDown")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }
}
