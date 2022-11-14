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
public class MultipleIterableParametersTest {

  @Parameters(name = "Case{index}: values at {index} are param1 = {0}, param2 = {1}, param3 = {2}")
  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {{"3", "2", "5"}, {"4", "3", "6"}});
  }

  @Parameter public String param1;

  @Parameter(1)
  public String param2;

  @Parameter(2)
  public String param3;

  @Test
  public void test1() {
    TestCaseLogger.log(param1);
  }

  @Test
  public void test2() {
    TestCaseLogger.log(param2);
  }

  @Test
  public void test3() {
    TestCaseLogger.log(param3);
  }
}
