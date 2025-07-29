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
package implicitnullcheck

import com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException
import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Tests places where implicit null checks are performed or not performed. */
fun main(vararg unused: String) {
  testNpe_onNonNullCast()
  testNpe_onNonNullableParameter()
  testNpe_onGenericParameter()
  testNpe_onUninitializedField()
  // The following tests show that kotlin is not null-safe at runtime.
  // (see https://youtrack.jetbrains.com/issue/KT-8135)
  testNoNpe_onGenericReturn()
  testNoNpe_onGenericPropertyAccess()
}

fun testNpe_onNonNullCast() {
  var o: Any? = null
  assertThrowsNullPointerException { o as Any }
}

private fun testNpe_onNonNullableParameter() {
  var h = Holder<String?>(null)

  // There is no check here, this is the case of a specialized return,
  // but the variable is explicitly typed as non-nullable.
  val o: String = (h as Holder<String>).get()

  acceptsNullAsNonNullable(o)
}

private fun testNpe_onGenericParameter() {
  var h = Holder<String?>(null)
  val o = (h as Holder<String>).get()
  acceptsNullAsNonNullableGeneric<Any>(o)
}

fun testNpe_onUninitializedField() {
  open class Super {
    val s: String

    constructor() {
      s = m()
    }

    open fun m(): String {
      return "S"
    }
  }

  class Sub(val initialValue: String) : Super() {
    // When m() is called from the constructor of the superclass the field has not been set yet,
    // so it returns a null value.
    override fun m() = initialValue
  }

  // There is no check here as well and the value is passed below as nullable.
  val nonNullString: String = Sub("").s
  acceptsNullAsNonNullable(nonNullString)
}

private fun testNoNpe_onGenericReturn() {
  var h = Holder<String?>(null)
  var g = h as Holder<String>
  var s: String = g.get()
  assertTrue(s == null)
}

private fun testNoNpe_onGenericPropertyAccess() {
  var h = Holder<String?>(null)
  var g = h as Holder<String>
  var s: String = g.content
  assertTrue(s == null)
}

private class Holder<T : Any?>(val content: T) {
  fun get(): T = content
}

private fun acceptsNullAsNonNullable(s: String) {
  assertTrue(s == null)
}

private fun <T : Any?> acceptsNullAsNonNullableGeneric(t: T) {
  assertTrue(t == null)
}
