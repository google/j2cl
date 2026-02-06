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
package varargs

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import varargs.innerpackage.SubclassWithImplicitConstructor
import varargs.innerpackage.SuperWithNoPublicConstructors

/** Tests varargs. */
fun main(vararg unused: String) {
  testVarargs_method()
  testVarargs_constructor()
  testVarargs_superMethodCall()
  testVarargs_implicitSuperConstructorCall()
  testVarargs_implicitSuperConstructorCall_implicitParameters()
  testVarargs_implicitSuperConstructorCall_enum()
  testVarargs_implicitSuperConstructorCall_genericTypes()
  testVarargs_implicitSuperConstructorCall_visibility()
  testVarargs_genericVarargsParameter()
  testVarargs_spread()
  testVarargs_indirectSpread()
  testVarargs_overloaded()
  testVarargs_primitiveArray()
  testVarargs_boxedArray()
}

private fun testVarargs_constructor() {
  open class Parent(vararg args: Int?) {
    var value = 0

    init {
      for (i in args) {
        value += i!!
      }
    }

    // call the vararg-constructor by this() constructor call.
    constructor(f: String?) : this(1)
  }

  class Child : Parent(2)

  val p = Parent(1, 2, 3) // constructor call with varargs.
  assertTrue(p.value == 6)
  val pp = Parent("") // constructor call with varargs is invoked by this() call.
  assertTrue(pp.value == 1)
  val cc = Child() // constructor call with varargs is invoked by super() constructor call.
  assertTrue(cc.value == 2)
  val spread = Parent(*arrayOf(3, 4, 5))
  assertTrue(spread.value == 12)
}

private fun testVarargs_genericVarargsParameter() {
  assertTrue(generics(1, 2) == 1)
  assertTrue(generics("abc", "def") == "abc")
  assertTrue(generics(*arrayOf("abc", "def"), "ghi") == "abc")
}

private fun <T> generics(vararg elements: T?): T? {
  return elements[0]
}

private fun testVarargs_superMethodCall() {
  open class Parent {
    fun sum(vararg args: Int?): Int {
      var sum = 0
      for (i in args) {
        sum += i!!
      }
      return sum
    }
  }

  class Child : Parent() {
    fun sum(a: Int, b: Int, c: Int, d: Int): Int {
      // call the vararg-constructor by super() method call.
      return super.sum(a, b, c, d)
    }
  }
  // method call with varargs is invoked by super() method call.
  assertTrue(Child().sum(1, 2, 3, 4) == Parent().sum(1, 2, 3, 4))
  assertTrue(Child().sum(1, *arrayOf(2, 3), 4) == Parent().sum(1, 2, 3, 4))
}

private open class SuperWithVarargsConstructors {
  var which: String = "None"

  constructor(vararg args: Any?) {
    which = "Any"
  }

  // Lead to an ambiguous constructor call in koltin
  // constructor(vararg args: String?) {
  //   which = "String"
  // }
}

private class ChildWithImplicitSuperCall : SuperWithVarargsConstructors()

private fun testVarargs_implicitSuperConstructorCall() {
  assertEquals("Any", ChildWithImplicitSuperCall().which)
}

private fun testVarargs_implicitSuperConstructorCall_implicitParameters() {
  // Lead to an ambiguous constructor call in koltin
  //   val captured = 1
  //    class Outer {
  //     inner class Parent {
  //       var which: String
  //       var value: Int
  //        constructor(vararg args: Any?) {
  //         which = "Any"
  //         value = captured
  //       }
  //        constructor(vararg args: String?) {
  //         which = "String"
  //         value = captured
  //       }
  //     }
  //      inner class Child : Parent()
  //   }
  //   Asserts.assertEquals("String", Outer().Child().which)
}

private fun testVarargs_implicitSuperConstructorCall_visibility() {
  open class Parent {
    var which: String

    private constructor() {
      which = "Private"
    }

    constructor(vararg args: Any?) {
      which = "Public"
    }

    // Private ctor is only visible inside the class.
    inner class Child : Parent()
  }

  // private is accessible in the class context.
  assertEquals("Private", Parent().Child().which)
  // No package private visibility modifier in kotlin
  assertEquals("Protected", SubclassWithImplicitConstructor().which)
  class SubclassInDifferentPackage : SuperWithNoPublicConstructors()
  assertEquals("Protected", SubclassInDifferentPackage().which)
}

private fun testVarargs_implicitSuperConstructorCall_genericTypes() {
  val captured = 1

  open class Parent<T : java.util.List<*>?, U : java.util.ArrayList<*>?> {
    var which: String
    var value: Int

    constructor(vararg args: T) {
      which = "NotList"
      value = captured
    }

    constructor(vararg args: U) {
      which = "List"
      value = captured
    }
  }

  // Lead to an ambiguous ctor call
  // class Child : Parent<List<*>?, java.util.ArrayList<*>?>()
  // assertEquals("List", Child().which)
}

@SuppressWarnings("ImmutableEnumChecker")
private enum class MyEnum {
  A(),
  B(*arrayOfNulls<Any>(0)),
  C(),
  D(*arrayOfNulls<Any>(0)),
  E(*arrayOfNulls<String>(0));

  // Without this ctor, A and C initialization lead to an ambiguous ctor call.
  constructor() {
    which = "None"
  }

  constructor(vararg args: Any?) {
    which = "Any"
  }

  constructor(vararg args: String?) {
    which = "String"
  }

  var which: String
}

private fun testVarargs_implicitSuperConstructorCall_enum() {
  assertEquals("None", MyEnum.A.which)
  assertEquals("Any", MyEnum.B.which)
  assertEquals("None", MyEnum.C.which)
  assertEquals("Any", MyEnum.D.which)
  assertEquals("String", MyEnum.E.which)
}

private fun testVarargs_method() {
  val i1 = 1
  val i2 = 2
  val i3 = 3
  val i = 4

  val a = bar(i1, i2, i3, i) // varargs with multiple arguments.
  assertTrue(a == 11)

  val b = bar(i1) // no argument for varargs.
  assertTrue(b == 2)

  val c = bar(i1, i2) // varargs with one element.
  assertTrue(c == 4)

  val e = bar(i1, *arrayOf()) // empty array for the varargs.
  assertTrue(e == 2)

  // unlike java, you cannot pass null for the vararg array
  // val f = bar(i1, null)
  // assertTrue(f == 1)

  val f = nonTrailingVararg(i1, i2, i3, b = i)
  assertTrue(f == 19)

  val g = nonTrailingVararg(i1, b = i)
  assertTrue(g == 14)

  val h = nonTrailingVarargWithDefault(i1, i2, i3)
  assertTrue(h == 7)

  val j = nonTrailingVarargWithDefault(i1)
  assertTrue(j == 2)
}

fun bar(a: Int, vararg args: Int?): Int {
  var result = 2 * a
  for (i in args) {
    result += i!!
  }
  return result
}

fun nonTrailingVarargWithDefault(a: Int, vararg args: Int?, b: Int = 0): Int {
  var result = 2 * a + 3 * b
  for (i in args) {
    result += i!!
  }
  return result
}

fun nonTrailingVararg(a: Int, vararg args: Int?, b: Int): Int {
  var result = 2 * a + 3 * b
  for (i in args) {
    result += i!!
  }
  return result
}

private fun testVarargs_spread() {
  val i1 = 1
  val i2 = 2
  val i3 = 3
  val i = 4

  val d = bar(i1, *arrayOf(i2, i3, i))
  assertTrue(d == 11)

  val g = bar(i1, i2, *arrayOf(1, 2, 3), i3)
  assertTrue(g == 13)

  val f = nonTrailingVararg(i1, *arrayOf(i2, i3), b = i)
  assertTrue(f == 19)

  val h = nonTrailingVarargWithDefault(i1, *arrayOf(i2, i3))
  assertTrue(h == 7)

  val j = nonTrailingVarargWithDefault(i1)
  assertTrue(j == 2)

  val k = bar(i1, *inlineCopyArray(arrayOf(i2, i3, i)))
  assertTrue(k == 11)
}

private inline fun <reified T> inlineCopyArray(array: Array<T>) = Array<T>(array.size) { array[it] }

private fun testVarargs_indirectSpread() {
  val a = indirectSpread(arrayOf(1, 2, 3))
  assertTrue(a == 1)
}

private fun <T : Comparable<T>> indirectSpread(a: Array<T>): T? = generics(*a)

private fun overloaded(o: Any?) = "overloaded(Any?)"

private fun overloaded(o: String?, vararg rest: Any?) = "overloaded(String?, Any?...)"

private fun overloadedEqualNumParams(x: Any?, y: Any?) = "overloadedEqualNumParams(Any?, Any?)"

private fun overloadedEqualNumParams(x: String?, vararg rest: Any?) =
  "overloadedEqualNumParams(String?, Any?...)"

private fun overloaded(l: Long) = "overloaded(Long)"

private fun overloaded(l: Long, vararg rest: Long) = "overloaded(Long, Long...)"

private fun testVarargs_overloaded() {
  assertEquals("overloaded(String?, Any?...)", overloaded("foo"))
  assertEquals("overloaded(Any?)", overloaded("foo" as Any))
  assertEquals("overloaded(String?, Any?...)", overloaded("foo", "bar"))
  assertEquals("overloadedEqualNumParams(String?, Any?...)", overloadedEqualNumParams("foo", "bar"))
  assertEquals(
    "overloadedEqualNumParams(Any?, Any?)",
    overloadedEqualNumParams("foo" as Any?, "bar"),
  )
  assertEquals(
    "overloadedEqualNumParams(String?, Any?...)",
    overloadedEqualNumParams("foo", "bar", "buzz"),
  )
  assertEquals(
    "overloadedEqualNumParams(String?, Any?...)",
    overloadedEqualNumParams("foo", arrayOf("bar")),
  )
  assertEquals(
    "overloadedEqualNumParams(String?, Any?...)",
    overloadedEqualNumParams("foo", arrayOf<Any?>("bar", "buzz")),
  )
  assertEquals(
    "overloadedEqualNumParams(String?, Any?...)",
    overloadedEqualNumParams("foo", *arrayOf<Any?>("bar", "buzz")),
  )
  assertEquals(
    "overloadedEqualNumParams(Any?, Any?)",
    overloadedEqualNumParams("foo" as Any, arrayOf("bar")),
  )
  assertEquals("overloaded(Long)", overloaded(1))
  assertEquals("overloaded(Any?)", overloaded(1L as Long?))
  assertEquals("overloaded(Long)", overloaded(1L))
  assertEquals("overloaded(Long, Long...)", overloaded(1L, 2, 3L))
  assertEquals("overloaded(Long, Long...)", overloaded(1, 2, 3L))
}

private fun returnVarargs_primitiveArray(vararg integers: Int): IntArray = integers

private fun returnVarargs_boxedArray(vararg integers: Int?): Array<out Int?> = integers

private fun testVarargs_primitiveArray() {
  val unused = returnVarargs_primitiveArray(1, 2, 3)
}

private fun testVarargs_boxedArray() {
  val unused2 = returnVarargs_boxedArray(1, 2, 3)
}
