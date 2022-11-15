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
package com.google.j2cl.junit.integration.asynckotlintest.data

import com.google.j2cl.junit.async.AsyncTestRunner
import com.google.j2cl.junit.async.Timeout
import com.google.j2cl.junit.integration.testing.async.Thenable
import com.google.j2cl.junit.integration.testing.async.Timer
import com.google.j2cl.junit.integration.testing.testlogger.TestCaseLogger
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.FixMethodOrder
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

private var first = true

/** Integration test used in J2clTestRunnerTest. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AsyncTestRunner::class)
class TestAsyncBeforeWillTimeOut {
  @Timeout(50)
  @BeforeTest
  fun willTimeoutOnFirstCall(): Thenable {
    TestCaseLogger.log("before")
    val delay = if (first) 300 else 0
    first = false
    return Thenable { onFulfilled, onRejected ->
      Timer.schedule({ onFulfilled.execute(null) }, delay)
    }
  }

  @Test
  fun test() {
    TestCaseLogger.log("test")
  }

  @Test
  fun testOther() {
    TestCaseLogger.log("testOther")
  }
}
