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
import java.util.Random
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests cast checks are optimized out when cast checking is disabled. */
@RunWith(JUnit4::class)
class CastDisableOptimizationTest {

  class TestObject

  private interface TestInterface

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private class NativeObject

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Function")
  private class NativeFunction

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array") private class NativeArray

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Number")
  private class NativeNumber

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  private class NativeString

  fun castOp(): TestObject {
    return field as TestObject
  }

  private fun castOpInterface(): TestInterface {
    return field as TestInterface
  }

  private fun castOpString(): String {
    return field as String
  }

  private fun castNativeFunction(): NativeFunction {
    return field as NativeFunction
  }

  private fun castNativeObject(): NativeObject {
    return field as NativeObject
  }

  private fun castNativeArray(): NativeArray {
    return field as NativeArray
  }

  private fun castNativeNumber(): NativeNumber {
    return field as NativeNumber
  }

  @JsMethod
  private fun failingCasts() {
    // All casts here are chosen to be failing casts to be extra sure that the compiler doesn't
    // optimize them out by somehow proving the cast is valid.
    castOp()
    castOpInterface()
    castOpString()
    castNativeFunction()
    castNativeNumber()
    castNativeArray()
    castNativeObject()
  }

  @JsProperty private external fun getFailingCasts(): Any

  @Test
  fun castsAreRemoved() {
    assertFunctionMatches(getFailingCasts(), "")
  }

  companion object {
    private val field = createField()
    private val randomNumber = Random().nextInt(42)

    private fun createField(): Any {
      // Makes sure that field type is not upgradable even the compiler becomes really smart and
      // also no types are pruned otherwise casts can be statically evaluated.
      return when (randomNumber) {
        0 -> TestObject()
        2 -> object : TestInterface {}
        3 -> "Some string"
        else ->
          // We always hit this case but compiler is not smart enough to know that.
          Any()
      }
    }
  }
}
