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
package com.google.j2cl.junit.integration.junit3.data;

import com.google.j2cl.junit.integration.testing.testlogger.TestCaseLogger;
import junit.framework.TestCase;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/** TestCase used for integration testing for j2cl JUnit support. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MethodOrderingParentTest extends TestCase {

  // Methods in this class are not in alphabetical order, but we expect them to be executed
  // in that order

  public void test_parent_b1() {
    TestCaseLogger.log("parent_b1");
  }

  public void test_parent_c() {
    TestCaseLogger.log("parent_c");
  }

  public void test_parent_b() {
    TestCaseLogger.log("parent_b");
  }

  public void test_parent_a() {
    TestCaseLogger.log("parent_a");
  }
}
