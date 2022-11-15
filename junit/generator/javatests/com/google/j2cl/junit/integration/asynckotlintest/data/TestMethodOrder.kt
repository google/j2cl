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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.FixMethodOrder
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

private fun asyncLog(logMsg: String): Thenable {
  return Thenable { onFulfilled, _ ->
    Timer.schedule(
      {
        TestCaseLogger.log(logMsg)
        onFulfilled.execute(null)
      },
      10
    )
  }
}

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AsyncTestRunner::class)
class TestMethodOrder {
  @Timeout(200L)
  @BeforeTest
  fun before1(): Thenable {
    return asyncLog("before1")
  }

  @BeforeTest
  fun before2() {
    TestCaseLogger.log("before2")
  }

  @Timeout(200L)
  @BeforeTest
  fun before3(): Thenable {
    return asyncLog("before3")
  }

  @Timeout(200L)
  @AfterTest
  fun after1(): Thenable {
    return asyncLog("after1")
  }

  @AfterTest
  fun after2() {
    TestCaseLogger.log("after2")
  }

  @Timeout(200L)
  @AfterTest
  fun after3(): Thenable {
    return asyncLog("after3")
  }

  @Test(timeout = 200L)
  fun test(): Thenable {
    return asyncLog("test")
  }
}
