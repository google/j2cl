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
package overloadedmethods

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test method overloading */
open class Parent

class Child : Parent()

class Foo {
  fun foo(a: Int): String {
    return "signature int"
  }

  fun foo(a: Long): String {
    return "signature long"
  }

  fun foo(a: Double): String {
    return "signature double"
  }

  fun foo(a: Int, b: Double): String {
    return "signature int double"
  }

  fun foo(a: Double, b: Int): String {
    return "signature double int"
  }

  fun foo(a: Int, b: Int): String {
    return "signature int int"
  }

  fun foo(a: Int, b: Double, c: Double): String {
    return "signature int double double"
  }

  fun foo(a: Child?): String {
    return "signature child"
  }

  fun foo(a: Parent?): String {
    return "signature parent"
  }

  fun foo(a: Any?): String {
    return "signature object"
  }

  fun foo(a: Parent?, b: Parent?): String {
    return "signature parent parent"
  }

  fun foo(a: Any?, b: Any?): String {
    return "signature object object"
  }
}

fun main(vararg unused: String) {
  val o = Foo()

  assertTrue(o.foo(1) == "signature int")
  assertTrue(o.foo(1.0) == "signature double")
  assertTrue(o.foo(1, 1.0) == "signature int double")
  assertTrue(o.foo(1.0, 1) == "signature double int")
  assertTrue(o.foo(1, 1) == "signature int int")
  assertTrue(o.foo(1, 1.0, 1.0) == "signature int double double")

  val parent = Parent()
  val child = Child()

  val objectParent: Any = Parent()
  val objectChild: Any = Child()

  assertTrue(o.foo(child) == "signature child")
  assertTrue(o.foo(parent) == "signature parent")
  assertTrue(o.foo(objectChild) == "signature object")
  assertTrue(o.foo(objectParent) == "signature object")
  assertTrue(o.foo(child, parent) == "signature parent parent")
  assertTrue(o.foo(objectChild, objectParent) == "signature object object")

  assertTrue(o.foo(0x80000000) == "signature long")
  assertTrue(o.foo(-0x80000000) == "signature int")
  assertTrue(o.foo(-(0x80000000)) == "signature int")
  assertTrue(o.foo(0x7fffffff + 1) == "signature int")
  assertTrue(o.foo(0x7fffffff * 2) == "signature int")
  assertTrue(o.foo(0x7fffffff shl 2) == "signature int")

  assertTrue(o.foo(0x80000000 - 1) == "signature long")
  assertTrue(o.foo(0x80000000 - 1 + 1) == "signature long")
  assertTrue(o.foo(2147483648 - 1 + 1) == "signature long")
}
