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
public class JUnit4IntegrationTest3 extends IntegrationTestBase {
  @Before
  public void assumeNonJ2kt() {
    assumeFalse(testMode.isJ2kt());
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
