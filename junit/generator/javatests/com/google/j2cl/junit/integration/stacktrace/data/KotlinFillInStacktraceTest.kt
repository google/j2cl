/*
 * Copyright 2024 Google Inc.
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
package com.google.j2cl.junit.integration.stacktrace.data

import kotlin.test.Test

/** Simple Throwable#fillInStackTrace stack test case */
class KotlinFillInStacktraceTest : StacktraceTestBase() {

  @Test
  fun test() {
    method1()
  }

  fun method1() {
    method2()
  }

  fun method2() {
    method3()
  }

  fun method3() {
    if (true) {
      val e = method4()
      e.fillInStackTrace()
      throw e
    }
  }

  private fun method4(): RuntimeException {
    return RuntimeException("__the_message__!\n And second line!")
  }
}
