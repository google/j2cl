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
package com.google.j2cl.junit.integration.parameterizedtest.data;

import com.google.j2cl.junit.integration.testing.testlogger.TestCaseLogger;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** TestCase used for integration testing for j2cl JUnit support. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Parameterized.class)
public class SimpleParameterizedTest {

  @Parameters
  public static Object[][] data() {
    return new Object[][] {{"1", 0}, {"2", 1}, {"2", 3}};
  }

  @Parameter(1)
  public int expected;

  @Parameter public String input;

  @Test
  public void test() {
    TestCaseLogger.log(input);
  }

  @Test
  public void test2() {
    TestCaseLogger.log(expected + "");
  }

  @SuppressWarnings("JUnit4TestNotRun")
  public void testMethodWithoutAnnotation() {
    TestCaseLogger.log("should_not_be_in_log");
  }

  public void testMethodWithoutAnnotationWithParameter(@SuppressWarnings("unused") Object foo) {
    TestCaseLogger.log("should_not_be_in_log");
  }

  public Object testMethodWithoutAnnotationWithReturnType() {
    TestCaseLogger.log("should_not_be_in_log");
    return null;
  }
}
