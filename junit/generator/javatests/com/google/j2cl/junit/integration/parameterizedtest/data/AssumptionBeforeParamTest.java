/*
 * Copyright 2024 Google Inc.
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

import static org.junit.Assume.assumeTrue;

import com.google.j2cl.junit.integration.testing.testlogger.TestCaseLogger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.AfterParam;
import org.junit.runners.Parameterized.BeforeParam;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AssumptionBeforeParamTest {
  @BeforeParam
  public static void before() {
    TestCaseLogger.log("beforeParam");
    assumeTrue(false);
  }

  @AfterParam
  public static void after() {
    TestCaseLogger.log("afterParam");
  }

  @Parameters(name = "{index}")
  public static Object[][] data() {
    return new Object[][] {{"1"}, {"2"}};
  }

  String input;

  @Test
  public void testShouldNotRun() {
    TestCaseLogger.log("should_not_be_in_log");
  }
}
