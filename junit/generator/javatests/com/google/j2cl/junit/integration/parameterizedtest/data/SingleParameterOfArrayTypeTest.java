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
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * TestCase used for integration testing for j2cl JUnit support when the single parameter is an
 * array of object .
 */
@RunWith(Parameterized.class)
public class SingleParameterOfArrayTypeTest {

  @Parameters
  public static List<Object[][]> data() {
    return Arrays.asList(new Object[][] {{"3", "2", "5"}}, new Object[][] {{"4", "3", "6"}});
  }

  @Parameter public Object[] input;

  @Test
  public void test() {
    TestCaseLogger.log("[" + input[0] + ", " + input[1] + ", " + input[2] + "]");
  }
}
