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
package typewildcards

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue

open class Foo {
  fun unbounded(g: GenericType<*>): Any {
    return g.field!!
  }

  fun upperBound(g: GenericType<out Foo>): Any {
    return g.field
  }

  fun lowerBound(g: GenericType<in Foo>): Any {
    return g.field!!
  }

  interface I<T : Foo?> {
    fun f(t: T): String {
      return "default"
    }
  }

  class RawSubclass : I<Foo?> {
    override fun f(t: Foo?): String {
      return "RawSubclass"
    }
  }

  class GenericType<T>(var field: T)

  class SubClass : Foo()
}

fun <T : String> concat(t1: T, t2: T): String = t1 + t2

fun <T : Int> add(t1: T, t2: T): Int = t1 + t2

fun <T : Double> add(t1: T, t2: T): Double = t1 + t2

fun <T : Double, S : Int> add(t1: T, t2: S): Double = t1 + t2

fun main(vararg unused: String) {
  val m = Foo()
  val o = Any()
  val s = Foo.SubClass()

  val gm = Foo.GenericType(m)
  val go = Foo.GenericType(o)
  val gs = Foo.GenericType(s)

  assertTrue(m.unbounded(gm) === m)
  assertTrue(m.unbounded(go) === o)

  assertTrue(m.upperBound(gm) === m)
  assertTrue(m.upperBound(gs) === s)

  assertTrue(m.lowerBound(gm) === m)
  assertTrue(m.lowerBound(go) === o)

  assertEquals("RawSubclass", Foo.RawSubclass().f(null))

  assertTrue("Hi there" == concat("Hi ", "there"))
  assertTrue(3 == add(1, 2))
  assertTrue(3.0 == add(1.0, 2))
  assertTrue(3.0 == add(1.0, 2.0))
}
