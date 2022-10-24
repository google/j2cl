/*
 * Copyright 2022 Google Inc.
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

import static org.junit.Assume.assumeFalse;

import com.google.j2cl.junit.integration.IntegrationTestBase;
import com.google.j2cl.junit.integration.TestResult;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration test for j2kt JUnit3 support. */
@RunWith(Parameterized.class)
public class J2ktJUnit3IntegrationTest extends IntegrationTestBase {

  @Before
  public void assumeNonWeb() {
    assumeFalse(testMode.isWeb());
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
            .addJavaLogLineSequence("test")
            .addJavaLogLineSequence("test")
            .addJavaLogLineSequence("parentTest")
            .addJavaLogLineSequence("parentTest")
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
    if (!testMode.isJ2kt()) {
      // Junit3 generates a non-standard error in this case that is hard to assert. Like:
      // 1) warning(junit.framework.TestSuite$1)
      return;
    }

    String testName = "ThrowsInConstructorTest";

    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testName)
            .addTestFailure("testOther")
            .addTestFailure("test")
            .addJavaLogLineSequence("constructor", "constructor")
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
            .addTestError("testOther", "java.lang.RuntimeException: throw in setup")
            .addTestError("test", "java.lang.RuntimeException: throw in setup")
            .addJavaLogLineSequence("setup", "setup")
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
            .addTestError("testOther", "java.lang.RuntimeException: throw in tearDown")
            .addTestError("test", "java.lang.RuntimeException: throw in tearDown")
            .addJavaLogLineSequence("test", "tearDown")
            .addJavaLogLineSequence("testOther", "tearDown")
            .build();

    List<String> logLines = runTest(testName);
    assertThat(logLines).matches(testResult);
  }
}
