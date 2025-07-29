/*
 * Copyright 2017 Google Inc.
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
package jsfunctionbridge

import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsFunction

fun main(vararg args: String) {
  testBridge()
  testBridge_parameterChecks()
}

private val FOO: ApplyFunction<String> =
  object : ApplyFunction<String> {
    val field: String = "a"

    override fun apply(s: String): String = s + field + `fun`()

    fun `fun`(): String = field + "d"
  }

private fun testBridge() {
  assertTrue("eaad" == callGeneric(FOO, "e"))
  assertTrue("eaad" == callParametric(FOO, "e"))
  assertTrue("eaad" == FOO.apply("e"))
  assertTrue("hello" == Identity().apply("hello"))
}

private fun testBridge_parameterChecks() {
  assertThrowsClassCastException { callGeneric(FOO, Any()) }
}

@Suppress("UNCHECKED_CAST")
private fun callGeneric(af: ApplyFunction<*>, o: Any?): Any? = (af as ApplyFunction<Any?>).apply(o)

private fun callParametric(af: ApplyFunction<String>, s: String): String = af.apply(s)

@JsFunction
fun interface ApplyFunction<T> {
  fun apply(element: T): T
}

private class Identity : ApplyFunction<Any?> {
  override fun apply(element: Any?): Any? = element
}
