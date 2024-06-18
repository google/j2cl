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
package com.google.j2cl.optimization.kotlin

import com.google.j2cl.optimization.OptimizationTestUtil.assertFunctionMatches
import java.util.ArrayList
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ArrayListOptimizationTest {

  private val arrayListField: ArrayList<String> = ArrayList<String>()

  @JsType(isNative = true, name = "ArrayList", namespace = "java.util")
  interface HasArrayListMethods {
    @JsProperty(name = "getAtIndex") fun getMethod(): Any

    @JsProperty(name = "setAtIndex") fun setMethod(): Any
  }

  @Test
  fun arrayListGetChecksAreRemoved() {
    val getMethod: Any = (arrayListField as HasArrayListMethods).getMethod()
    assertFunctionMatches(getMethod, "return this.<obf>[<obf>];")
  }

  @Test
  fun arrayListSetChecksAreRemoved() {
    val instance: HasArrayListMethods = arrayListField as HasArrayListMethods
    assertFunctionMatches(
      instance.setMethod(),
      "var <obf>=this.<obf>(<obf>); this.<obf>[<obf>]=<obf>; return <obf>;",
    )
  }
}
