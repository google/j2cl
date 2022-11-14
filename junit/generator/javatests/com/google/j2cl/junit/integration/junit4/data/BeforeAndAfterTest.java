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
package com.google.j2cl.junit.integration.junit4.data;

import static junit.framework.Assert.assertEquals;

import com.google.j2cl.junit.integration.testing.testlogger.TestCaseLogger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** TestCase used for integration testing for j2cl JUnit support. */
@RunWith(JUnit4.class)
public class BeforeAndAfterTest {

  private static int beforeClassRan = 0;

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

  public BeforeAndAfterTest() {
    TestCaseLogger.log("constructor");
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

  @Test
  public void test1() {
    TestCaseLogger.log("test");
  }

  @Test
  public void test2() {
    TestCaseLogger.log("test");
  }
}
