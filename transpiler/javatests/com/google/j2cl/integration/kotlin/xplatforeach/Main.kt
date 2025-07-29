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
package xplatforeach

import com.google.apps.docs.xplat.collections.JsArrayInteger
import com.google.apps.docs.xplat.collections.SerializedJsArray
import com.google.apps.docs.xplat.collections.SerializedJsMap
import com.google.apps.docs.xplat.collections.UnsafeJsMap
import com.google.apps.docs.xplat.collections.UnsafeJsMapInteger
import com.google.apps.docs.xplat.collections.UnsafeJsSet
import com.google.apps.docs.xplat.structs.SparseArray
import com.google.gwt.corp.collections.ImmutableJsArray
import com.google.gwt.corp.collections.JsArray
import com.google.gwt.corp.collections.UnmodifiableJsArray
import com.google.j2cl.integration.testing.Asserts.assertEquals
import jsinterop.annotations.JsMethod

fun main(vararg unused: String) {
  testJsArray()
  testImmutableJsArray()
  testUnmodifiableJsArray()
  testJsArrayInteger()
  testSerializedJsArray()
  testSerializedJsMap()
  testUnsafeJsMap()
  testUnsafeJsMapInteger()
  testUnsafeJsSet()
  testSparseArray()
}

fun testJsArray() {
  val array: JsArray<String> = createArrayLike("a", "b", "c")
  val actual = Array<String?>(3) { null }
  var i = 0
  for (element in array.getIterable()) {
    actual[i++] = element
  }
  assertEquals(arrayOf("a", "b", "c"), actual)
}

fun testImmutableJsArray() {
  val array: ImmutableJsArray<String> = createArrayLike("a", "b", "c")
  val actual = Array<String?>(3) { null }
  var i = 0
  for (element in array.getIterable()) {
    actual[i++] = element
  }
  assertEquals(arrayOf("a", "b", "c"), actual)
}

fun testUnmodifiableJsArray() {
  val array: UnmodifiableJsArray<String> = createArrayLike("a", "b", "c")
  val actual = Array<String?>(3) { null }
  var i = 0
  for (element in array.getIterable()) {
    actual[i++] = element
  }
  assertEquals(arrayOf("a", "b", "c"), actual)
}

fun testJsArrayInteger() {
  val array: JsArrayInteger = createArrayLike()
  setAt(array, 0, 1)
  setAt(array, 1, 2)
  setAt(array, 2, 3)

  val actualInts = IntArray(3)
  var i = 0
  for (element: Int in array.getIterable()) {
    actualInts[i++] = element
  }
  assertEquals(intArrayOf(1, 2, 3), actualInts)

  i = 0
  val actualIntegers = Array<Int?>(3) { null }
  for (element: Int? in array.getIterable()) {
    actualIntegers[i++] = element
  }
  assertEquals(arrayOf(1, 2, 3), actualIntegers)
}

fun testSerializedJsArray() {
  val array: SerializedJsArray = createArrayLike()
  setAt(array, 0, "a")
  setAt(array, 1, 2)
  setAt(array, 2, 3.3)

  val actual = Array<Any?>(3) { null }
  var i = 0
  for (element in array.getIterable()) {
    actual[i++] = element
  }
  assertEquals(arrayOf("a", 2.0, 3.3), actual)
}

fun testSerializedJsMap() {
  val map = SerializedJsMap()
  set(map, "a", 1)
  set(map, "b", 2)
  set(map, "c", 3)

  val actual = Array<String?>(3) { null }
  var i = 0
  for (key in map.getIterableKeys()) {
    actual[i++] = key
  }

  assertEquals(arrayOf("a", "b", "c"), actual)
}

fun testUnsafeJsMap() {
  val map = UnsafeJsMap<Any?>()
  set(map, "a", 1)
  set(map, "b", 2)
  set(map, "c", 3)

  val actual = Array<String?>(3) { null }
  var i = 0
  for (key in map.getIterableKeys()) {
    actual[i++] = key
  }

  assertEquals(arrayOf("a", "b", "c"), actual)
}

fun testUnsafeJsMapInteger() {
  val map = UnsafeJsMapInteger()
  set(map, "a", 1)
  set(map, "b", 2)
  set(map, "c", 3)

  val actual = Array<String?>(3) { null }
  var i = 0
  for (key in map.getIterableKeys()) {
    actual[i++] = key
  }

  assertEquals(arrayOf("a", "b", "c"), actual)
}

fun testUnsafeJsSet() {
  val map = UnsafeJsSet()
  set(map, "a", true)
  set(map, "b", false)
  set(map, "c", true)

  val actual = Array<String?>(3) { null }
  var i = 0
  for (key in map.getIterableKeys()) {
    actual[i++] = key
  }

  assertEquals(arrayOf("a", "b", "c"), actual)
}

fun testSparseArray() {
  val array: SparseArray<String> = createArrayLike()
  setAt(array, 2, "a")
  setAt(array, 4, "b")
  setAt(array, 8, "c")

  var i = 0
  val actualIntKeys = IntArray(3)
  for (key: Int in array.getIterableKeys()) {
    actualIntKeys[i++] = key
  }
  assertEquals(intArrayOf(2, 4, 8), actualIntKeys)

  i = 0
  val actualIntegerKeys = Array<Int?>(3) { null }
  for (key: Int? in array.getIterableKeys()) {
    actualIntegerKeys[i++] = key
  }
  assertEquals(arrayOf<Int?>(2, 4, 8), actualIntegerKeys)
}

@Suppress("UNCHECKED_CAST") private fun <T> createArrayLike(vararg values: Any?): T = values as T

@JsMethod(name = "set") private external fun setAt(arr: Any, index: Int, value: Any?)

@JsMethod(name = "set") private external fun setAt(arr: Any, index: Int, value: Int)

@JsMethod private external fun set(arr: Any, property: String, value: Any?)

@JsMethod private external fun set(arr: Any, property: String, value: Int)
