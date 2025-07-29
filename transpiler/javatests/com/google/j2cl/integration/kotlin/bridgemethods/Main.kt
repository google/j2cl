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
package bridgemethods

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils.isJvm

/** Tests for bridge methods with accidental overriding. */
fun main(vararg unused: String) {
  testSimpleBridges()
  testBridgeForwardsToSpecializedMethod()
  testBridgeSpecializesSuperclassMethod()
  testAbstractHidesSuperGenericMethod()
  testBridgesMultipleOverloads()
  testParameterizedMethodBridge()
}

private fun testSimpleBridges() {
  open class Parent<T> {
    open fun foo(t: T?): String {
      return "Parent"
    }
  }

  class Child : Parent<String?>() {
    // a bridge method should be generated for Parent.foo(T).
    override fun foo(s: String?): String {
      return "Child"
    }
  }

  class AnotherChild : Parent<String?>() {
    // no bridge method should be generated, so no ClassCastException will be caught even
    // an argument of wrong type is passed.
    // NOTE: Why might make this more restrictive if we start synthesizing bridges as high as
    // possible in the class hierarchy, and that is OK.
  }

  val c = Child() as Parent<Any>
  val ac = AnotherChild() as Parent<Any>
  assertEquals("Child", c.foo(null))
  assertEquals("Child", c.foo("String"))
  assertEquals("Parent", ac.foo(Object())) // no ClassCastException. But we can change this.
  assertEquals("Parent", ac.foo("String"))
  assertThrowsClassCastException({ c.foo(Object()) }, String::class.java)
}

private fun testBridgeForwardsToSpecializedMethod() {
  open class Parent {
    open fun foo(e: Error): Error {
      return e
    }
  }

  class Child : Parent(), SuperInterface<Error> {
    // Parent.foo(Error) accidentally overrides SuperInterface.foo(T)
    // there should be a bridge method foo__Object for SuperInterface.foo(T), and the bridge
    // method delegates to foo__Error() in Parent.
  }

  val c = Child()
  val e = Error()
  assertTrue((callInterfaceFoo(c as SuperInterface<Any>, e) == e))
  assertTrue((c.foo(e) == callInterfaceFoo(c as SuperInterface<Any>, e)))
  assertThrowsClassCastException { callInterfaceFoo(c as SuperInterface<Any>, Any()) }
}

interface SuperInterface<T> {
  fun foo(t: T): T
}

private fun callInterfaceFoo(intf: SuperInterface<Any>, t: Any): Any {
  return intf.foo(t)
}

private fun testBridgeSpecializesSuperclassMethod() {
  abstract class AbstractSupplier<T>(var t: T) {
    fun get(): T {
      return t
    }

    fun f(t: T?): T? {
      return this.t
    }
  }

  class SupplierStringImpl : AbstractSupplier<String>, SupplierString {
    // T AbstractSupplier.get() accidentally overrides String SupplierString.get(). Hence there
    // should be a bridge method String SupplierStringImpl.get() that has a cast check on the
    // return.
    constructor(s: String) : super(s)
  }

  val sImpl = SupplierStringImpl("Hello")
  val s: SupplierString = sImpl
  assertTrue(s.get().equals("Hello"))

  // Assign to raw type to subvert the value.
  val abss = sImpl as AbstractSupplier<Any>
  abss.t = Object()
  assertThrowsClassCastException { s.f(null) }
  assertThrowsClassCastException { s.get() }
}

interface SupplierString {
  fun get(): String

  fun f(s: String?): String?
}

private fun testAbstractHidesSuperGenericMethod() {
  open class Setter<T> {
    open fun set(t: T): String {
      return "Setter"
    }
  }

  abstract class StringSetterAbstract : Setter<String>(), StringSetter<String> {
    abstract override fun set(t: String): String
  }

  // This class is to make sure that when b/64282599 is fixed the proper bridges are still in
  // place.
  class StringSetterImpl : StringSetterAbstract() {
    override fun set(t: String): String {
      return "StringSetterImpl"
    }
  }
  assertEquals("StringSetterImpl", StringSetterImpl().set(""))
  assertEquals("StringSetterImpl", (StringSetterImpl() as Setter<String>).set(""))
}

interface StringSetter<String> {
  fun set(t: String): String
}

private fun testBridgesMultipleOverloads() {
  open class Parent<T : Number?> {
    open fun foo(t: T?): String {
      return "Parent"
    }
  }

  class Child : Parent<Int?>(), SomeInterface<Int?> {
    // accidental overrides.
  }

  class AnotherChild : Parent<Number>(), SomeInterface<Int?> {
    // SomeInterface<Integer>.foo does NOT override Parent<Number>.foo
    override fun foo(t: Int?): String {
      return "AnotherChild"
    }
  }

  val int = 1
  val short: Short? = 1

  val c = Child()

  assertEquals("Parent", callByInterface(c, int))

  // The bridging in Kotlin/JVM creates a bridge with number as the parameter, but it does not
  // throw. See b/235884461.
  if (!isJvm()) {
    assertThrowsClassCastException(
      { callByInterface(c as SomeInterface<Number?>, short as Number?) },
      Integer::class.javaObjectType,
    )
  }

  // This one throws in all three, but the message of the exception does not match since in
  // Java/JVM and Kotlin/JVM the failing cast is a cast to Number, whereas in J2CL the failing cast
  // is a cast to Integer.
  assertThrowsClassCastException { (c as SomeInterface<Any?>).foo("String") }

  val pc: Parent<Int?> = c
  assertEquals("Parent", pc.foo(int))
  assertEquals("Parent", c.foo(int))

  val ac = AnotherChild()
  val asParent: Parent<Number> = ac

  assertEquals("Parent", asParent.foo(short))
  assertEquals("Parent", asParent.foo(int))
  assertEquals("AnotherChild", callByInterface(ac, int))
  assertEquals("AnotherChild", ac.foo(int))
  assertEquals("Parent", ac.foo(short)) // does not match foo(integer) so this is foo(Number)
}

private fun testParameterizedMethodBridge() {
  open class Parent<T> {
    open fun <E : T> m(e: E): String {
      return "super"
    }
  }
  class Child : Parent<String>() {
    override fun <F : String> m(e: F): String {
      return "child"
    }
  }
  assertEquals("child", Child().m(""))
  val p: Parent<String> = Child()
  assertEquals("child", p.m(""))
}

interface SomeInterface<T> {
  fun foo(t: T): String
}

private fun callByInterface(intf: SomeInterface<Int?>, ae: Int?): String {
  return intf.foo(ae)
}

private fun callByInterface(intf: SomeInterface<Number?>, e: Number?): String {
  return intf.foo(e)
}
