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
package com.google.j2cl.junit.integration.parameterizedtest;

import static org.junit.Assume.assumeFalse;

import com.google.j2cl.junit.integration.IntegrationTestBase;
import com.google.j2cl.junit.integration.TestResult;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration test for j2cl JUnit4 parameterized test support. */
@RunWith(Parameterized.class)
public class J2clParameterizedIntegrationTest2 extends IntegrationTestBase {

  @Before
  public void assumeNonJ2wasm() {
    // TODO(b/233963223): J2wasm does not support parameterized test yet.
    assumeFalse(testMode.isJ2wasm());
  }

  @Before
  public void assumeNonJ2kt() {
    assumeFalse(testMode.isJ2kt());
  }

  @Test
  public void testParentTest() throws Exception {
    String testClassName = "ParentTest";
    String[][] data = {{"2", "3"}, {"3", "4"}};
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testClassName)
            .addTestSuccess("testGroup0_testParent[0]")
            .addTestSuccess("testGroup1_testParent[1]")
            .addJavaLogLineSequence(data[0][0])
            .addJavaLogLineSequence(data[1][0])
            .build();

    List<String> logLines = runTest(testClassName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testChildTest() throws Exception {
    String testClassName = "ChildTest";
    String[][] data = {{"abc", "bcd"}, {"cde", "def"}};
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testClassName)
            .addTestSuccess("testGroup0_testChild1[0]")
            .addTestSuccess("testGroup0_testChild2[0]")
            .addTestSuccess("testGroup0_testParent[0]")
            .addTestSuccess("testGroup1_testChild1[1]")
            .addTestSuccess("testGroup1_testChild2[1]")
            .addTestSuccess("testGroup1_testParent[1]")
            .addJavaLogLineSequence(data[0][0], data[0][1], data[0][0])
            .addJavaLogLineSequence(data[1][0], data[1][1], data[1][0])
            .build();

    List<String> logLines = runTest(testClassName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testBeforeAndAfterParamTest() throws Exception {
    String testClassName = "BeforeAndAfterParamTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testClassName)
            .addTestSuccess("testGroup0_test1[0]")
            .addTestSuccess("testGroup0_test2[0]")
            .addTestSuccess("testGroup1_test1[1]")
            .addTestSuccess("testGroup1_test2[1]")
            .addJavaLogLineSequence(
                "beforeParam1", "beforeParam2", "constructor", "setUp", "test1: 1", "tearDown")
            .addJavaLogLineSequence(
                "constructor", "setUp", "test2: 0", "tearDown", "afterParam1", "afterParam2")
            .addJavaLogLineSequence(
                "beforeParam1", "beforeParam2", "constructor", "setUp", "test1: 2", "tearDown")
            .addJavaLogLineSequence(
                "constructor",
                "setUp",
                "test2: 1",
                "tearDown",
                "afterParam1",
                "afterParam2",
                "afterClass")
            .build();

    List<String> logLines = runTest(testClassName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testBeforeAndAfterParamWithFailingTest() throws Exception {
    String testClassName = "BeforeAndAfterParamWithFailingTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testClassName)
            .addTestSuccess("testGroup0_test1[0]")
            .addTestFailure("testGroup0_test2[0]")
            .addTestSuccess("testGroup1_test1[1]")
            .addTestSuccess("testGroup1_test2[1]")
            .addJavaLogLineSequence("beforeParam", "constructor", "setUp", "test1: 1", "tearDown")
            .addJavaLogLineSequence("constructor", "afterParam")
            .addJavaLogLineSequence("beforeParam", "constructor", "setUp", "test1: 2", "tearDown")
            .addJavaLogLineSequence(
                "constructor", "setUp", "test2: 1", "tearDown", "afterParam", "afterClass")
            .build();

    List<String> logLines = runTest(testClassName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testConstructorWithIncorrectParameterCount() throws Exception {
    String testClassName = "ConstructorWithIncorrectParameterCount";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testClassName)
            .addTestFailure("testGroup0_test[0]")
            .addTestFailure("testGroup1_test[1]")
            .build();

    List<String> logLines = runTest(testClassName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testIncorrectInjectedFieldCount() throws Exception {
    String testClassName = "IncorrectInjectedFieldCount";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testClassName)
            .addTestFailure("testGroup0_test[0]")
            .addTestFailure("testGroup1_test[1]")
            .build();

    List<String> logLines = runTest(testClassName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testThrowsInFloatToIntegerTest() throws Exception {
    String testClassName = "ThrowsInFloatToIntegerTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testClassName)
            .addTestFailure("testGroup0_test[0]")
            .addTestFailure("testGroup1_test[1]")
            .build();

    List<String> logLines = runTest(testClassName);
    assertThat(logLines).matches(testResult);
  }

  @Test
  public void testIntegerToIntTest() throws Exception {
    String testClassName = "IntegerToIntTest";
    TestResult testResult =
        newTestResultBuilder()
            .testClassName(testClassName)
            .addTestSuccess("testGroup0_test[0]")
            .build();

    List<String> logLines = runTest(testClassName);
    assertThat(logLines).matches(testResult);
  }
}
