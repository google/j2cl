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

import com.google.j2cl.junit.integration.testing.testlogger.TestCaseLogger;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

/** TestCase used for integration testing for j2cl JUnit support. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4.class)
public class MethodOrderingTest extends MethodOrderingTestParent {

  // Methods in this class are not in alphabetical order, but we expect them to be executed
  // in that order

  @Test
  public void b1() {
    TestCaseLogger.log("b1");
  }

  @Test
  public void c() {
    TestCaseLogger.log("c");
  }

  @Test
  public void b() {
    TestCaseLogger.log("b");
  }

  @Test
  public void a() {
    TestCaseLogger.log("a");
  }
}
