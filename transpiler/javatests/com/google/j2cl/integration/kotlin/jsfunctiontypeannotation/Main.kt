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
package jsfunctiontypeannotation

import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod

@JsFunction
fun interface ApplyFunction<T, S : Number?> {
  fun apply(t: T, s: S): T
}

class A : ApplyFunction<Double?, Double?> {
  override fun apply(d: Double?, i: Double?): Double? = d!! + i!!
}

class B<T> : ApplyFunction<T, Int?> {
  override fun apply(element: T, i: Int?): T = element
}

fun main(vararg args: String) {
  testParameterTypes()
  testCast()
  testNewInstance()
}

@Suppress("UNCHECKED_CAST")
fun callGenericInterface(af: ApplyFunction<*, *>, o: Any?, n: Number?): Any? =
  (af as ApplyFunction<Any?, Number?>).apply(o, n)

fun callParametricInterface(af: ApplyFunction<String, Int?>, s: String): String = af.apply(s, 1)

fun <U, V : Number?> callParametricWithTypeVariable(af: ApplyFunction<U, V>, u: U, v: V): U =
  af.apply(u, v)

@Suppress("UNCHECKED_CAST")
fun callImplementorGeneric(b: B<*>, o: Any?, n: Int?): Any? = (b as B<Any?>).apply(o, n)

fun callImplementorParametric(b: B<String>, s: String): String = b.apply(s, 1)

@Suppress("UNCHECKED_CAST")
fun testParameterTypes() {
  val foo: ApplyFunction<*, *> = B<String>()
  val bar: ApplyFunction<*, *> = A()
  assertTrue(callGenericInterface(foo, "a", 1) == "a")
  assertTrue(callGenericInterface(bar, 1.1, 1.1) == 2.2)
  assertTrue(callParametricInterface(foo as ApplyFunction<String, Int?>, "a") == "a")
  assertTrue(callParametricWithTypeVariable(foo, "a", 1) == "a")
  assertTrue(callParametricWithTypeVariable(bar as ApplyFunction<Any?, Double?>, 1.1, 1.1) == 2.2)
  assertTrue(callImplementorGeneric(B<Double?>() as B<Any?>, 1.1, 1) == 1.1)
  assertTrue(callImplementorParametric(B<String>(), "") == "")
  assertTrue(foo.apply("a", 1) == "a")
  assertTrue(bar.apply(1.1, 1.1) == 2.2)
  assertTrue(callOnFunction(A()) == 2.2)
}

@Suppress("UNCHECKED_CAST")
fun testCast() {
  val o: Any = B<String>()
  val b1 = o as B<*>
  val b2 = o as B<String>
  val af1 = o as ApplyFunction<*, *>
  val af2 = o as ApplyFunction<String, Int?>

  assertThrowsClassCastException {
    val a = o as A
  }
  assertTrue(b1 != null)
  assertTrue(b2 != null)
  assertTrue(af1 != null)
  assertTrue(af2 != null)
}

@Suppress("UNCHECKED_CAST")
fun testNewInstance() {
  val b1: B<*> = B<Any?>()
  val b2: B<String> = (B<String>() as B<*>) as B<String>
  val af1: ApplyFunction<*, *> = A()
  assertTrue(b1 != null)
  assertTrue(b2 != null)
  assertTrue(af1 != null)
}

@JsMethod external fun callOnFunction(f: ApplyFunction<Double?, Double?>): Double?
