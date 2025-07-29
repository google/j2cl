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
package nullsafetyoperators

class MyClass(val field: Int)

fun main() {
  var a: MyClass? = MyClass(1)
  val b = a?.field
  val c = a!!.field
  a = null
  val d = a ?: MyClass(2)
  val e: MyClass? = null
  val f = e?.field ?: d!!.field
  val g: java.lang.Number? = null
  val h = g?.intValue()
  val i = 1!!
  val j = 1 ?: 2
  val k = 1?.hashCode()
}
