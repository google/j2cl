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
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import kotlin.test.Test
import org.junit.runner.RunWith

/** Integration test used in J2clTestRunnerTest. */
@RunWith(AsyncTestRunner::class)
class TestStructuralThenable {
  fun interface StructuralThenable {
    @JsFunction
    fun interface FullFilledCallback {
      fun execute(o: Any?)
    }

    @JsFunction
    fun interface RejectedCallback {
      fun execute(o: Any?)
    }

    @JsMethod fun then(onFulfilled: FullFilledCallback?, onRejected: RejectedCallback?)
  }

  @Test(timeout = 200L)
  fun testStructuraThenable(): StructuralThenable {
    return StructuralThenable { onFulfilled, _ -> onFulfilled!!.execute(null) }
  }

  fun interface SubStructuralThenable : StructuralThenable

  @Test(timeout = 200L)
  fun testStructuraThenable_subinterface(): SubStructuralThenable {
    return SubStructuralThenable { onFulfilled, _ -> onFulfilled!!.execute(null) }
  }

  abstract class StructuralThenableImpl : SubStructuralThenable

  @Test(timeout = 200L)
  fun testStructuraThenable_subclass(): StructuralThenableImpl {
    return object : StructuralThenableImpl() {
      override fun then(
        onFulfilled: StructuralThenable.FullFilledCallback?,
        onRejected: StructuralThenable.RejectedCallback?,
      ) {
        onFulfilled!!.execute(null)
      }
    }
  }
}
