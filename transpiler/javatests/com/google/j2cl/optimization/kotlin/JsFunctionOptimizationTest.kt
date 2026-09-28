/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.optimization.kotlin

import com.google.j2cl.optimization.OptimizationTestUtil.assertFunctionMatches
import jsinterop.annotations.JsFunction
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class JsFunctionOptimizationTest {

  @JsFunction
  fun interface MyJsFunctionInterface {
    fun foo(a: Int): Int
  }

  @Test
  fun jsFunctionOptimizes() {
    val lambda = MyJsFunctionInterface { a -> a }
    assertFunctionMatches(lambda, "return <obf>;")

    // inner class optimizable to lambda
    val optimizableInner =
      object : MyJsFunctionInterface {
        override fun foo(a: Int): Int = a
      }
    assertEquals(MyJsFunctionInterface::class.java, optimizableInner.javaClass)
    assertFunctionMatches(optimizableInner, "return <obf>;")
  }
}
