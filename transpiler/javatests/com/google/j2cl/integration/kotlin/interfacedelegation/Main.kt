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
package interfacedelegation

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNotEquals

fun main(vararg unused: String) {
  testDelegation_simple()
  testEvaluationOrder()
  testDelegation_byProperty()
  testDelegation_byMethodCall()
  testDelegation_byObjectClass()
  testDelegation_objectMethods()
  testDelegation_withInterfaceImplementation()
  testDelegation_accidentalOverride()
  testMultipleInterfacesDelegation()
  testMultipleInterfacesDelegation_parameterizedTypes()
}

interface I {
  val message: String
  val propertyOverridenInDelegatingClass: String

  fun retrieveString(): String

  fun methodOverridenInDelegatingClass(): String

  fun nonOveriddenfunctionUsingOverridenProperty(): String

  fun defaultMethod(): String {
    return "DefaultMethod from interface"
  }

  fun defaultMethodOverridenInD(): String {
    return "DefaultMethod from interface"
  }
}

private fun testDelegation_simple() {
  class Implementor() : I {
    val m = "Implemented by Implementor"
    override val message = m
    override val propertyOverridenInDelegatingClass = m

    override fun retrieveString(): String {
      return m
    }

    override fun methodOverridenInDelegatingClass(): String {
      return m
    }

    override fun nonOveriddenfunctionUsingOverridenProperty(): String {
      return propertyOverridenInDelegatingClass
    }
  }

  val implementor = Implementor()

  class DelegatingClass() : I by implementor {
    override val propertyOverridenInDelegatingClass = "DelegatingClass"

    override fun methodOverridenInDelegatingClass(): String {
      return "Overriden in DelegatingClass"
    }

    override fun defaultMethodOverridenInD(): String {
      return "DefaultMethod overriden in DelegatingClass"
    }
  }

  assertEquals(implementor.retrieveString(), DelegatingClass().retrieveString())
  assertEquals(implementor.message, DelegatingClass().message)
  assertEquals(implementor.defaultMethod(), DelegatingClass().defaultMethod())
  // The function being not overridden, the implementation of implementor will be used and will
  // return the value of the property defined in implementor.
  assertEquals(
    implementor.nonOveriddenfunctionUsingOverridenProperty(),
    DelegatingClass().nonOveriddenfunctionUsingOverridenProperty(),
  )

  assertNotEquals(
    implementor.methodOverridenInDelegatingClass(),
    DelegatingClass().methodOverridenInDelegatingClass(),
  )
  assertNotEquals(
    implementor.propertyOverridenInDelegatingClass,
    DelegatingClass().propertyOverridenInDelegatingClass,
  )
  assertNotEquals(
    implementor.defaultMethodOverridenInD(),
    DelegatingClass().defaultMethodOverridenInD(),
  )
}

interface I1

interface I2

interface I3

var sequence = "start"

class Implementor() : I1, I2, I3

fun addToSequence(s: String): String {
  sequence += "+" + s
  return sequence
}

fun <T> addToSequence(s: String, t: T): T {
  addToSequence(s)
  return t
}

fun testEvaluationOrder() {
  open class Super(s: String)

  class DelegatingClass(var i: I3 = addToSequence("DefaultParams", Implementor())) :
    Super(addToSequence("SuperType")),
    I1 by addToSequence("I1Delegation", Implementor()),
    I2 by addToSequence("I2Delegation", Implementor()),
    I3 by i

  assertEquals("start", sequence)
  DelegatingClass()
  assertEquals("start+DefaultParams+SuperType+I1Delegation+I2Delegation", sequence)
}

interface Foo1 {
  fun f1(): String
}

interface Foo2 {
  fun f2(): String
}

class Foo1Implementor : Foo1 {
  var a = this

  override fun f1(): String {
    return "f1 implementation from Foo1Implementor"
  }

  fun createFoo1(): Foo1Implementor {
    return this
  }
}

private fun testDelegation_byProperty() {
  class DelegatingClass() : Foo1 by Foo1Implementor().a

  assertEquals(Foo1Implementor().a.f1(), DelegatingClass().f1())
}

private fun testDelegation_byMethodCall() {
  val foo1Implementor = Foo1Implementor()

  class DelegatingClass() : Foo1 by foo1Implementor.createFoo1()

  assertEquals(foo1Implementor.createFoo1().f1(), DelegatingClass().f1())
}

object Foo1ImplementorObject : Foo1 {
  override fun f1(): String {
    return "Foo1ImplementorObject"
  }
}

private fun testDelegation_byObjectClass() {
  class DelegatingClass() : Foo1 by Foo1ImplementorObject

  assertEquals(Foo1ImplementorObject.f1(), DelegatingClass().f1())
}

interface FooWithSameMethodAsFoo1 {
  fun f1(): String
}

// Test whether method from interface delegation or method from interface
// implementation wins.
private fun testDelegation_withInterfaceImplementation() {
  class FullDelegatingClass(foo1: Foo1) : Foo1 by foo1, FooWithSameMethodAsFoo1

  class DelegatingClassWithImplementation(foo1: Foo1) : Foo1 by foo1, FooWithSameMethodAsFoo1 {
    override fun f1(): String {
      return "f1 from FooWithSameMethodAsFoo1"
    }
  }

  val foo1Implementor = Foo1Implementor()
  assertEquals(foo1Implementor.f1(), FullDelegatingClass(foo1Implementor).f1())
  assertNotEquals(foo1Implementor.f1(), DelegatingClassWithImplementation(foo1Implementor).f1())
}

interface A {
  override fun hashCode(): Int

  override fun toString(): String

  override fun equals(other: Any?): Boolean
}

private fun testDelegation_objectMethods() {
  class AImplementor : A {
    override fun toString(): String {
      return "AImplementor"
    }

    override fun hashCode(): Int {
      return 1
    }

    override fun equals(other: Any?): Boolean {
      return false
    }
  }

  class DelegatingClass(a: A) : A by a {
    override fun hashCode(): Int {
      return 2
    }

    override fun toString(): String {
      return "DelegatingClass"
    }

    override fun equals(other: Any?): Boolean {
      return true
    }
  }

  class FullDelegatingClass(a: A) : A by a

  val aImplementor = AImplementor()

  assertNotEquals(aImplementor.hashCode(), DelegatingClass(aImplementor).hashCode())
  assertNotEquals(aImplementor.toString(), DelegatingClass(aImplementor).toString())
  assertNotEquals(
    aImplementor.equals(aImplementor),
    DelegatingClass(aImplementor).equals(aImplementor),
  )

  assertEquals(aImplementor.hashCode(), FullDelegatingClass(aImplementor).hashCode())
  assertEquals(aImplementor.toString(), FullDelegatingClass(aImplementor).toString())
  assertEquals(
    aImplementor.equals(aImplementor),
    FullDelegatingClass(aImplementor).equals(aImplementor),
  )
}

private fun testDelegation_accidentalOverride() {
  open class Bar {
    open fun f1(): String {
      return "bar"
    }
  }

  class DelegationWithAccidentalOverride(foo1: Foo1) : Foo1 by foo1, Bar()

  val foo1Implementor = Foo1Implementor()
  assertEquals(foo1Implementor.f1(), DelegationWithAccidentalOverride(foo1Implementor).f1())
}

private fun testMultipleInterfacesDelegation() {
  class Foo2Implementor : Foo2 {
    override fun f2(): String {
      return "f2 implementation from Foo2Implementor"
    }
  }

  class DelegationWithMultipleInterfaces(foo1: Foo1, foo2: Foo2) : Foo1 by foo1, Foo2 by foo2

  val foo1 = Foo1Implementor()
  val foo2 = Foo2Implementor()
  assertEquals(foo1.f1(), DelegationWithMultipleInterfaces(foo1, foo2).f1())
  assertEquals(foo2.f2(), DelegationWithMultipleInterfaces(foo1, foo2).f2())
}

interface Consumer<T> {
  fun consume(): T
}

interface Accepter<T> {
  fun accept(): T

  fun fCommon(): String {
    return "the method is from default method in Accepter"
  }
}

private fun testMultipleInterfacesDelegation_parameterizedTypes() {
  class ConsumerImpl : Consumer<String> {
    override fun consume(): String {
      return "from ConsumerImpl"
    }

    fun fCommon(): String {
      return "the method is from ConsumerImpl"
    }
  }

  class AccepterImpl : Accepter<String> {
    override fun accept(): String {
      return "from AccepterImpl"
    }
  }

  class MultipleInterfacesDelegation(consumer: Consumer<String>, accepter: Accepter<String>) :
    Consumer<String> by consumer, Accepter<String> by accepter

  val consumer = ConsumerImpl()
  val accepter = AccepterImpl()
  assertEquals(consumer.consume(), MultipleInterfacesDelegation(consumer, accepter).consume())
  assertEquals(accepter.accept(), MultipleInterfacesDelegation(consumer, accepter).accept())

  // No matter what sequence the interface delegation takes, the default method from interface wins
  // the same method from implementation
  assertEquals(accepter.fCommon(), MultipleInterfacesDelegation(consumer, accepter).fCommon())
}
