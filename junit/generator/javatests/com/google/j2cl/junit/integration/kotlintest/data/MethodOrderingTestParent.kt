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
package com.google.j2cl.junit.integration.kotlintest.data

import com.google.j2cl.junit.integration.testing.testlogger.TestCaseLogger
import kotlin.test.Test
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
open class MethodOrderingTestParent {
  // Methods in this class are not in alphabetical order, but we expect them to be executed
  // in that order
  @Test
  fun parent_b1() {
    TestCaseLogger.log("parent_b1")
  }

  @Test
  fun parent_c() {
    TestCaseLogger.log("parent_c")
  }

  @Test
  fun parent_b() {
    TestCaseLogger.log("parent_b")
  }

  @Test
  fun parent_a() {
    TestCaseLogger.log("parent_a")
  }
}
