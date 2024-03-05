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

import jsinterop.annotations.JsFunction
import kotlin.test.Test

/** Integration test for throwing in a JsFunction */
class KotlinThrowsInJsFunction : StacktraceTestBase() {
  @JsFunction
  fun interface MyFunction {
    fun run()
  }

  @Test
  fun test() {
    executesFunction(this::methodRefJsFunction)
  }

  private fun methodRefJsFunction() {
    executesFunction {
      // Lambda JsFunction
      executesFunction(
        // Anonymous JsFunction
        object : MyFunction {
          override fun run() {
            // Concrete JsFunction
            executesFunction(MyFunctionImpl())
          }
        }
      )
    }
  }

  private class MyFunctionImpl : MyFunction {
    override fun run() {
      throw RuntimeException("__the_message__!")
    }
  }

  fun executesFunction(myFunction: MyFunction) {
    myFunction.run()
  }
}
