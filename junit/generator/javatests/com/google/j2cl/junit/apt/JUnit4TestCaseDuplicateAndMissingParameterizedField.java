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
package com.google.j2cl.junit.apt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** A simple Unit test to test processing in {@link J2clTestingProcessingStepTest}. */
@RunWith(Parameterized.class)
public class JUnit4TestCaseDuplicateAndMissingParameterizedField {

  @Parameters
  public static Object[][] data() {
    return new Object[][] {{"0", 0}, {"1", 1}};
  }

  @Parameter public int field;

  @Parameter(1)
  public int field1;

  @Parameter(1)
  public int field2;

  @Test
  public void test() {}
}
