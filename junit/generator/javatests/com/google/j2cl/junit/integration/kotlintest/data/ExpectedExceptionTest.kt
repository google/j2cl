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
import kotlin.test.Test

class ExpectedExceptionTest {
  // provoke a name collision
  class RuntimeException : java.lang.RuntimeException()

  @Test(expected = java.lang.RuntimeException::class)
  fun testShouldFail() {
    // test should fail since no exception is thrown
    TestCaseLogger.log("testShouldFail")
  }

  @Test(expected = ArrayStoreException::class)
  fun testShouldFail1() {
    TestCaseLogger.log("testShouldFail1")
    // test should fail since ArrayStoreException extends from RuntimeException
    if (true) {
      throw RuntimeException()
    }
  }

  @Test(expected = RuntimeException::class)
  fun testShouldSucceed() {
    TestCaseLogger.log("testShouldSucceed")
    if (true) {
      throw RuntimeException()
    }
  }
}
