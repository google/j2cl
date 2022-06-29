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
import static junit.framework.Assert.assertTrue;

import com.google.j2cl.junit.integration.testlogger.TestCaseLogger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** TestCase used for integration testing for j2cl JUnit support. */
@RunWith(Parameterized.class)
public final class PrimitiveAndObjectCastTest {
  private static final int OFFSET = 5;

  @Parameters
  public static Object[][] data() {
    return new Object[][] {
      {1, 1, new Integer(1)}, {2, 2, new Integer(2)}, {999, 999, new Integer(999)}
    };
  }

  // input does not need any cast and serves as a comparison to input2 and input3
  @Parameter public int input;

  @Parameter(1)
  public Integer input2;

  @Parameter(2)
  public int input3;

  @Test
  public void test() {
    assertEquals(input + OFFSET, input3 + OFFSET);
    assertEquals(input, input2.intValue());
    assertTrue(input == input3);
    TestCaseLogger.log(input + ", " + input2 + ", " + input3);
  }
}
