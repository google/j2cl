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
import jsinterop.annotations.JsEnum
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsProperty
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests that unreferenced enum values are effectively removed. */
@RunWith(JUnit4::class)
class EnumOptimizationTest {
  internal enum class Foo {
    UNREFERENCED_VALUE_1,
    UNREFERENCED_VALUE_2,
    UNREFERENCED_SUBCLASS_VALUE_1,
    UNREFERENCED_SUBCLASS_VALUE_2,
  }

  @JsMethod
  private fun unusedEnumValues() {
    val v1 = Foo.UNREFERENCED_VALUE_1
    val v2 = Foo.UNREFERENCED_VALUE_2
    val v3 = Foo.UNREFERENCED_SUBCLASS_VALUE_1
    val v4 = Foo.UNREFERENCED_SUBCLASS_VALUE_2
  }

  @JsProperty private external fun getUnusedEnumValues(): Any

  @Test
  fun unreferencedEnumValuesAreRemoved() {
    assertFunctionMatches(getUnusedEnumValues(), "")
  }

  @JsEnum
  internal enum class MyJsEnum {
    ONE,
    TWO,
  }

  @JsMethod
  private fun unusedJsEnumBoxes() {
    val o1: Any = MyJsEnum.ONE
    val o2: Any = MyJsEnum.TWO
  }

  @JsProperty private external fun getUnusedJsEnumBoxes(): Any

  @Test
  fun unreferencedBoxedJsEnumsAreRemoved() {
    assertFunctionMatches(getUnusedJsEnumBoxes(), "")
  }

  @JsMethod
  private fun jsEnumReferenceEquality(): Boolean {
    val e = MyJsEnum.TWO
    return e == MyJsEnum.TWO
  }

  @JsProperty private external fun getJsEnumReferenceEquality(): Any?

  @Test
  fun comparisonByReferenceDoesNotBox() {
    assertFunctionMatches(getJsEnumReferenceEquality(), "return !0;")
  }

  @JsMethod
  private fun jsEnumEquals(): Boolean {
    val e = MyJsEnum.TWO
    return e == MyJsEnum.TWO
  }

  @JsProperty private external fun getJsEnumEquals(): Any

  // TODO(b/117514489): Enable and complete the test when unnecessary boxing is not longer emitted.
  @Ignore
  fun equalsDoesNotBox() {
    assertFunctionMatches(getJsEnumEquals(), "")
  }
}
