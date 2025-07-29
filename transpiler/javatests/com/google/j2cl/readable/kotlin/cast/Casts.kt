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
package cast

import java.io.Serializable

class Casts {
  fun testCastToClass() {
    val o = Any()
    val c = o as Casts?
  }

  fun testCasToInterface() {
    val o = Any()
    val s = o as Serializable?
  }

  fun testCastToBoxedType() {
    val o: Any? = null
    val b: Byte? = o as Byte?
    val d: Double? = o as Double?
    val f: Float? = o as Float?
    val i: Int? = o as Int?
    val l: Long? = o as Long?
    val s: Short? = o as Short?
    val n: Number? = o as Number?
    val c: Char? = o as Char?
    val bool: Boolean? = o as Boolean?
  }

  fun testCastToArray() {
    val foo: Array<Any?>? = null as Array<Any?>?
    val bar: Array<Array<Any?>?>? = null as Array<Array<Any?>?>?
  }

  private fun functionForInstanceofTest() {}

  fun testCastVarialeAfterInstanceOf() {
    var o: Any? = Any()
    if (o is Casts) {
      val c: Casts = o
    }
    if (o is Casts) {
      val cAvoidsCastsTo: Casts = o
      o = Any()
      val cNotAvoidCastsTo: Casts? = o as Casts?
    }
    if (o is Casts) {
      val cAvoidsCastsTo: Casts = o
      o = Foo()
      val cNotAvoidCastsTo: Casts? = o as Casts?
    }
    if (o is Casts) {
      val cAvoidsCastsTo: Casts = o
      o = Any()
      val cAvoidCastsTo: Casts? = o as Casts?
    }
    if (o is Casts) {
      val cAvoidsCastsTo: Casts = o
      functionForInstanceofTest()
      val cNotAvoidCastsTo: Casts = o
    }

    val c = if (o is Casts) o else null
  }

  /** Class for testing purposes */
  private inner class Foo {
    var field: Any = Any()

    fun method(): Any {
      return Any()
    }
  }

  fun testCastFieldAfterInstanceOf() {
    var foo: Foo = Foo()
    if (foo.field is Casts) {
      val c = foo.field as Casts?
    }
    if (foo.field is Casts) {
      val cAvoidsCastsTo = foo.field as Casts?
      foo.field = Any()
      val cNotAvoidCastsTo = foo.field as Casts?
    }
    if (foo.field is Casts) {
      val cAvoidsCastsTo = foo.field as Casts?
      foo = Foo()
      val cNotAvoidCastsTo = foo.field as Casts?
    }
    if (foo.field is Casts) {
      val cAvoidsCastsTo = foo.field as Casts?
      functionForInstanceofTest()
      val cNotAvoidCastsTo = foo.field as Casts?
    }

    val c = if (foo.field is Casts) foo.field as Casts else null
  }

  fun testCaseMethodAfterInstanceOf() {
    val foo: Foo = Foo()
    if (foo.method() is Casts) {
      val cNotAvoidCastsTo = foo.method() as Casts?
    }

    val c = if (foo.method() is Casts) foo.method() as Casts else null
  }

  fun testCastNullability() {
    val a: Any? = Foo()
    val aa = a as Foo

    val b: Any? = Foo()
    val bb = b as Foo?

    val c: Any? = Foo()
    val cc = c as? Foo

    val d: Any? = Foo()
    val dd = d as? Foo?
  }

  fun testPrecedence() {
    val foo: Any = "foo"
    val bar: Any = "bar"
    val notString = 123
    val s1 = (if (false) foo else bar) as String
    val s2 = ("foo" + notString) as String
  }

  private fun testSafeCast(a: Any): Foo? {
    return a as? Foo
  }

  fun testCastToSameType() {
    val a = 1 as Int
    val b = true as Boolean
    val c = 'b' as Char
    val d = "Hello World" as String
    val f = Foo() as Foo
    val g: Int? = 1
    val h = g as Int?
  }

  fun testRedundantVarianceCasts(holder: Holder<Throwable>?) {
    val a = holder as Holder<Throwable>?
    val outA = holder as Holder<out Throwable>?
    val inA = holder as Holder<in Throwable>?

    val b = holder!! as Holder<Throwable>
    val outB = holder!! as Holder<out Throwable>
    val inB = holder!! as Holder<in Throwable>

    val c = holder as Holder<Throwable>
    val outC = holder as Holder<out Throwable>
    val inC = holder as Holder<in Throwable>
  }
}

class Holder<T>
