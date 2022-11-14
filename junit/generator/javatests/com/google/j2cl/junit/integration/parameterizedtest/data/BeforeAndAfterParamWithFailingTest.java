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

import static junit.framework.Assert.assertEquals;

import com.google.j2cl.junit.integration.testing.testlogger.TestCaseLogger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.AfterParam;
import org.junit.runners.Parameterized.BeforeParam;
import org.junit.runners.Parameterized.Parameters;

/** TestCase used for integration testing for j2cl JUnit support. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Parameterized.class)
public class BeforeAndAfterParamWithFailingTest {

  private static int beforeClassRan;
  private static int counter;

  @Parameters(name = "{index}")
  public static Object[][] data() {
    return new Object[][] {{"1", "0"}, {"2", "1"}};
  }

  @BeforeClass
  public static void beforeClass() {
    // logger does not work in before class since there is no active test case yet :/
    // instead we use a variable and check this in setup.
    beforeClassRan++;
  }

  @AfterClass
  public static void afterClass() {
    TestCaseLogger.log("afterClass");
  }

  @BeforeParam
  public static void beforeParam() {
    TestCaseLogger.log("beforeParam");
  }

  @AfterParam
  public static void afterParam() {
    TestCaseLogger.log("afterParam");
  }

  @Before
  public void setUp() {
    assertEquals(1, beforeClassRan);
    TestCaseLogger.log("setUp");
  }

  @After
  public void tearDown() {
    TestCaseLogger.log("tearDown");
  }

  private final String input1;

  private final String input2;

  public BeforeAndAfterParamWithFailingTest(String input1, String input2) {
    this.input1 = input1;
    this.input2 = input2;
    TestCaseLogger.log("constructor");
    counter++;
    if (counter == 2) {
      throw new RuntimeException("throwing in the constructor");
    }
  }

  @Test
  public void test1() {
    TestCaseLogger.log("test1: " + input1);
  }

  @Test
  public void test2() {
    TestCaseLogger.log("test2: " + input2);
  }
}
