/*
 * Copyright 2022 Google Inc.
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
package strings

import com.google.j2cl.integration.testing.Asserts.assertEquals

fun main(vararg unused: String) {
  assertEquals("null", (null as Any?).toString())
  assertEquals("1", 1.toString())
  assertEquals("1", intToStringHelper(1))
  assertEquals("1", nullableIntToStringHelper(1))
  assertEquals("null", nullableIntToStringHelper(null))
  assertEquals("bar", Foo().toString())
  assertEquals("bar", toStringHelper(Foo()))
  assertEquals("1,2", intArrayOf(1, 2).toString())
  assertEquals("1,2", toStringHelper(intArrayOf(1, 2)))
}

class Foo {
  override fun toString() = "bar"
}

fun intToStringHelper(i: Int) = i.toString()

fun nullableIntToStringHelper(i: Int?) = i.toString()

fun toStringHelper(o: Any?) = o.toString()
