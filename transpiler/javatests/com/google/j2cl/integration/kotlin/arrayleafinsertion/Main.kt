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
package arrayleafinsertion

import com.google.j2cl.integration.testing.Asserts.assertThrowsArrayStoreException
import com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException

fun main(vararg unused: String) {
  testFullArray()
  testPartialArray()
}

private fun testFullArray() {
  val array: Array<Any?> = arrayOfNulls<HasName>(2) as Array<Any?>

  // You can insert a leaf value of a different but conforming type.
  array[0] = Person()

  // When inserting a leaf value the type must conform.
  assertThrowsArrayStoreException { array[0] = Any() }

  // You can always insert null.
  array[0] = null
}

private fun testPartialArray() {
  // You can create a partially initialized array.
  val partialArray: Array<Array<Any?>?> = arrayOfNulls(1)

  // When trying to insert into the uninitialized section you'll get an NPE.
  assertThrowsNullPointerException { partialArray[0]!![0] = Person() }

  // You can replace it with a fully initialized array.

  val fullyInitializedArray = arrayOf(arrayOfNulls<Any>(1))

  // And then insert a leaf value that conforms to the strictest leaf type
  fullyInitializedArray[0][0] = Person()
  fullyInitializedArray[0][0] = null
}
