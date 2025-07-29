/*
 * Copyright 2023 Google Inc.
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
package arrayiterator

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testIteratorMethodCall()
  testSpecializedIterator()
}

fun testIteratorMethodCall() {
  val arr = arrayOf("foo", "bar", "baz")
  val iterator = arr.iterator()

  var i = 0
  while (i < arr.size) {
    assertTrue(iterator.hasNext())
    assertEquals(arr[i], iterator.next())
    i++
  }

  val primitiveArr = intArrayOf(1, 2, 3, 4)
  val primitiveIterator = primitiveArr.iterator()

  i = 0
  while (i < primitiveArr.size) {
    assertTrue(primitiveIterator.hasNext())
    assertTrue(primitiveIterator.next() == primitiveArr[i])
    i++
  }
}

fun testSpecializedIterator() {
  val intArr = intArrayOf(1, 2, 3, 4)
  val intIterator = intArr.iterator()

  var i = 0
  while (i < intArr.size) {
    assertTrue(intIterator.nextInt() == intArr[i])
    i++
  }

  val boolArr = booleanArrayOf(true, false, true, false)
  val boolIterator = boolArr.iterator()

  i = 0
  while (i < boolArr.size) {
    assertTrue(boolIterator.nextBoolean() == boolArr[i])
    i++
  }
}
