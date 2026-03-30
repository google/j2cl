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
package arrayofnativejstype

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object") open class MyNativeType

private fun testSingleDimension() {
  val arr = arrayOfNulls<MyNativeType>(10)
  assertTrue(arr.size == 10)

  val element = MyNativeType()
  arr[0] = element
  assertTrue(arr[0] === element)
}

private fun testMultiDimension() {
  val arr2d = Array(3) { arrayOfNulls<MyNativeType>(2) }
  assertTrue(arr2d.size == 3)
  assertTrue(arr2d[0].size == 2)

  val element = MyNativeType()
  arr2d[1][1] = element
  assertTrue(arr2d[1][1] === element)

  val arr2dVar = arrayOfNulls<Array<MyNativeType>>(3)
  assertTrue(arr2dVar.size == 3)
  assertTrue(arr2dVar[0] == null)
}

private fun testArrayLiteral() {
  val element1 = MyNativeType()
  val element2 = MyNativeType()
  val arr = arrayOf(element1, element2)

  assertTrue(arr.size == 2)
  assertTrue(arr[0] === element1 && arr[1] === element2)
}

private fun testEquality() {
  val arr1 = arrayOfNulls<MyNativeType>(10)
  val arr2 = arrayOfNulls<MyNativeType>(10)
  assertTrue(arr1 === arr1)
  assertTrue(arr1 !== arr2)
  assertTrue(arr1 != null)

  val undefinedArr = getUndefined()
  assertTrue(undefinedArr == null)
  assertTrue(undefinedArr !== arr1)
}

@JsProperty(namespace = JsPackage.GLOBAL, name = "undefined")
private external fun getUndefined(): Array<MyNativeType?>?

// Trick:
// Object(arr) call is effectively a pass-through without native.js.
// Also validates that passed instance is actually not a Wasm object.

@JsMethod(namespace = JsPackage.GLOBAL, name = "Object")
private external fun passThrough(arr: Array<MyNativeType?>): Array<MyNativeType?>

@JsMethod(namespace = JsPackage.GLOBAL, name = "Object")
private external fun passThrough2d(arr: Array<Array<MyNativeType?>>): Array<Array<MyNativeType?>>

@JsMethod(namespace = JsPackage.GLOBAL, name = "Array.isArray")
private external fun isArray(arr: Array<MyNativeType?>): Boolean

@JsMethod(namespace = JsPackage.GLOBAL, name = "Array.isArray")
private external fun isArray(arr: Array<Array<MyNativeType?>>): Boolean

private fun testJsBoundary() {
  val element = MyNativeType()
  val arr = arrayOf<MyNativeType?>(element)
  assertTrue(isArray(arr))

  val result = passThrough(arr)
  assertTrue(isArray(result))
  assertTrue(result.size == 1)
  assertTrue(result[0] === element)

  val arr2d = arrayOf(arrayOf<MyNativeType?>(element))
  assertTrue(isArray(arr2d))

  val result2d = passThrough2d(arr2d)
  assertTrue(isArray(result2d))
  assertTrue(result2d.size == 1 && result2d[0].size == 1)
  assertTrue(result2d[0][0] === element)
}

fun main() {
  testSingleDimension()
  testMultiDimension()
  testArrayLiteral()
  testEquality()
  testJsBoundary()
}
