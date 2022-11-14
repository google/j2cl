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
import kotlin.test.Ignore
import kotlin.test.Test

@Test
fun testAtFileLevel() {
  TestCaseLogger.log("should_not_be_in_log")
}

/** TestCase used for integration testing for j2cl JUnit support. */
class SimplePassingTest {
  @Test
  fun test() {
    TestCaseLogger.log("test")
  }

  @Test
  fun testOther() {
    TestCaseLogger.log("testOther")
  }

  @Test
  fun validTestMethodWithoutPrefix() {
    TestCaseLogger.log("validTestMethodWithoutPrefix")
  }

  @Ignore
  @Test
  fun testIgnore() {
    TestCaseLogger.log("should_not_be_in_log")
  }

  fun testMethodWithoutAnnotation() {
    TestCaseLogger.log("should_not_be_in_log")
  }

  fun testMethodWithoutAnnotationWithParameter(@Suppress("UNUSED_PARAMETER") foo: Any?) {
    TestCaseLogger.log("should_not_be_in_log")
  }

  fun testMethodWithoutAnnotationWithReturnType(): Any? {
    TestCaseLogger.log("should_not_be_in_log")
    return null
  }
}
