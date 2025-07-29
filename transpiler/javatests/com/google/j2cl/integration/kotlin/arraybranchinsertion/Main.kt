/*
 * Copyright 2022 Google Inc.
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
package arraybranchinsertion

import com.google.j2cl.integration.testing.Asserts.assertThrowsArrayStoreException
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testFullArray()
  testPartialArray()
}

private fun testFullArray() {
  val array2d: Array<Array<out Any?>> =
    (arrayOf<Array<HasName?>>(arrayOfNulls(2), arrayOfNulls(2))) as Array<Array<out Any?>>
  assertTrue(array2d[0].size == 2)
  assertTrue(array2d.size == 2)

  // You can swap out an entire array in an array slot.
  array2d[0] = arrayOfNulls<HasName>(2)
  assertTrue(array2d[0].size == 2)

  // When inserting an array into an array slot you *can* change the length.
  array2d[0] = arrayOfNulls<HasName>(4)
  assertTrue(array2d[0].size == 4)

  // When inserting an array into an array slot you can tighten the leaf type as a class or
  // interface.
  array2d[0] = arrayOfNulls<Person>(2)
  assertTrue(array2d[0].size == 2)
  array2d[0] = arrayOfNulls<HasFullName>(2)
  assertTrue(array2d[0].size == 2)

  // When inserting an array into an array slot you can NOT broaden the leaf type.
  assertThrowsArrayStoreException { array2d[0] = arrayOfNulls<Any>(2) }

  // You can NOT put an object in a slot that expects an array.
  // Hide the cast of the array in a method call to avoid KotlinC to try to optimize item insertion
  // operation that leads to an inconsistent semantic.
  // See https://youtrack.jetbrains.com/issue/KT-55005
  assertThrowsArrayStoreException { castToArrayOfAny(array2d)[0] = Any() }

  // When inserting an array into an array slot you can not change the number of dimensions.
  assertThrowsArrayStoreException {
    array2d[0] = arrayOf(arrayOfNulls<HasName>(2), arrayOfNulls<HasName>(2))
  }
}

private fun castToArrayOfAny(arr: Any): Array<Any> = arr as Array<Any>

private fun testPartialArray() {
  // You can create a partially initialized array.
  val partialArray: Array<Array<out Any?>?> = arrayOfNulls(1)
  assertTrue(partialArray.size == 1)

  // You can fill the uninitialized dimensions with the same type.
  partialArray[0] = arrayOfNulls(100)
  assertTrue(partialArray[0]!!.size == 100)

  // Or with a subtype.
  partialArray[0] = arrayOfNulls<Person>(100)
  assertTrue(partialArray[0]!!.size == 100)
}
