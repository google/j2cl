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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BooleanOptimizationTest {
  @JsMethod
  fun simpleComp(): Boolean {
    return true == true
  }

  @JsProperty private external fun getSimpleComp(): Any

  @Test
  fun simpleCompOptimizes() {
    assertFunctionMatches(getSimpleComp(), "return !0;")
  }

  @JsMethod
  fun boxedComp(): Boolean {
    return java.lang.Boolean.valueOf(true) === java.lang.Boolean.valueOf(true)
  }

  @JsProperty private external fun getBoxedComp(): Any

  @Test
  fun boxedCompOptimizes() {
    assertFunctionMatches(getBoxedComp(), "return !0;")
  }

  @JsMethod
  fun booleanFieldComp(): Boolean {
    return java.lang.Boolean.TRUE === java.lang.Boolean.TRUE
  }

  @JsProperty private external fun getBooleanFieldComp(): Any

  @Test
  fun booleanFieldCompOptimizes() {
    assertFunctionMatches(getBooleanFieldComp(), "return !0;")
  }
}
