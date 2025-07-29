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
package arrayoutofbounds

fun main(vararg unused: String) {
  testArrayLiteral()
  testArray()
}

private fun testArrayLiteral() {
  val empty = arrayOf<Any>()
  // We don't throw exception on out of bounds index.
  empty[100] = "ted"
  val oneD = intArrayOf(0, 1, 2)
  // We don't throw exception on out of bounds index.
  oneD[3] = 5
  val twoD = arrayOf(intArrayOf(0, 1), intArrayOf(2, 3))
  // We don't throw exception on out of bounds index.
  twoD[0][2] = 4
}

private fun testArray() {
  val empty = arrayOfNulls<Any>(0)
  // We don't throw exception on out of bounds index.
  empty[100] = "ted"
  val oneD = IntArray(3)
  // We don't throw exception on out of bounds index.
  oneD[3] = 5
  val twoD = arrayOf(IntArray(2), IntArray(2))
  // We don't throw exception on out of bounds index.
  twoD[0][2] = 4
}
