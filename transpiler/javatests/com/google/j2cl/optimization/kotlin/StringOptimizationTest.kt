/*
 * Copyright 2024 Google Inc.
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
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsProperty
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StringOptimizationTest {
  @Before
  fun setUp() {
    // Refer to static fields of String so that $clinit is not empty and does not get pruned.
    String.CASE_INSENSITIVE_ORDER.compare("aaa", "bbb")
  }

  @JsMethod
  fun stringEqualsString(): Boolean {
    return "".equals("")
  }

  @JsProperty private external fun getStringEqualsString(): Any

  @JsMethod
  fun stringNotEqualsString(): Boolean {
    return "".equals(Any())
  }

  @JsProperty private external fun getStringNotEqualsString(): Any

  @Test
  fun simpleEqualsOptimizes() {
    assertFunctionMatches(getStringEqualsString(), "return !0;")
    assertFunctionMatches(getStringNotEqualsString(), "return !1;")
  }

  @JsMethod
  fun stringSameString(): Boolean {
    return "" === ""
  }

  @JsProperty private external fun getStringSameString(): Any

  @Test
  fun simpleSameOptimizes() {
    assertFunctionMatches(getStringSameString(), "return !0;")
  }

  @JsMethod
  fun stringEqualsStringOnStatic(): Boolean {
    return staticField
  }

  @JsProperty private external fun getStringEqualsStringOnStatic(): Any

  @Test
  fun staticFieldEqualsOptimizes() {
    assertFunctionMatches(getStringEqualsStringOnStatic(), "return !0;")
  }

  companion object {
    private val staticField = "asd" == "asd"
  }
}
