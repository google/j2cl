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
package com.google.j2cl.junit.integration.kotlintest.data

import com.google.j2cl.junit.integration.testing.testlogger.TestCaseLogger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** TestCase used for integration testing for j2cl JUnit support. */
open class BeforeAndAfterTest {
  var ran = "beforeRan"

  init {
    ran = "init"
    TestCaseLogger.log("constructor")
  }

  @BeforeTest
  fun setUp() {
    assertEquals("init", ran, "The value for ran should be init")
    TestCaseLogger.log("setUp")
  }

  @AfterTest
  fun tearDown() {
    TestCaseLogger.log("tearDown")
  }

  @Test
  fun test1() {
    TestCaseLogger.log("test")
  }

  @Test
  fun test2() {
    TestCaseLogger.log("test")
  }
}
