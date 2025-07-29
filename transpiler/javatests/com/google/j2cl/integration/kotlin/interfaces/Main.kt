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
package interfaces

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testInterfaceDispatch()
  testDefaultMethods()
  testDefaultMethods_superCall()
  testDefaultMethods_diamondProperty()
  testPrivateMethods()
  testAccidentalOverrides()
  testCompanionFields()
  testCompanionMethods()
}

interface SomeInterface {
  fun run(): Int
}

internal class SomeClass : SomeInterface {
  override fun run(): Int {
    return 1
  }
}

fun run(someInterface: SomeInterface): Int {
  return someInterface.run()
}

private fun testInterfaceDispatch() {
  val s: SomeClass = SomeClass()
  assertEquals(1, run(s))
}

interface InterfaceWithFields {
  companion object {
    val A = 1
    val B = 2
  }
}

fun testCompanionFields() {
  assertEquals(1, InterfaceWithFields.A)
  assertEquals(2, InterfaceWithFields.B)
  assertEquals(1, InterfaceWithFields.Companion.A)
  assertEquals(2, InterfaceWithFields.Companion.B)
}

val COLLECTION_ADD = 1
val LIST_ADD = 2
val ABSTRACT_COLLECTION_ADD = 3
val ANOTHER_STRING_LIST_ADD = 4
val ANOTHER_LIST_INTERFACE_ADD = 5

internal interface Collection<T> {
  fun add(elem: T?): Int {
    assertTrue(this is Collection)
    return COLLECTION_ADD
  }
}

internal interface List<T> : Collection<T> {
  override fun add(elem: T?): Int {
    assertTrue(this is List)
    return LIST_ADD
  }

  companion object {
    fun returnOne(): Int {
      return 1
    }
  }
}

abstract class AbstractCollection<T> : Collection<T> {
  override fun add(elem: T?): Int {
    assertTrue(this is AbstractCollection)
    return ABSTRACT_COLLECTION_ADD
  }
}

class ACollection<T> : Collection<T>

abstract class AbstractList<T> : AbstractCollection<T>(), List<T> {
  // In contrast to Java where the implementation in AbstractCollection is the one inherited,
  // in Kotlin it is ambiguous and needs to be declared explicitly.
  override fun add(elem: T?): Int {
    return super<AbstractCollection>.add(elem)
  }
}

open class AConcreteList<T> : AbstractList<T>()

open class SomeOtherCollection<T> : Collection<T>

open class SomeOtherList<T> : SomeOtherCollection<T>(), List<T>

// Should inherit List.add  even though the interface is not directly declared.
open class YetAnotherList<T> : SomeOtherList<T>(), Collection<T>

open class StringList : List<String>

open class YetAnotherStringList : YetAnotherList<String>()

open class AnotherStringList : List<String> {
  override fun add(elem: String?): Int {
    return ANOTHER_STRING_LIST_ADD
  }
}

internal interface AnotherListInterface<T> {
  fun add(elem: T?): Int {
    assertTrue(this is AnotherListInterface)
    return ANOTHER_LIST_INTERFACE_ADD
  }
}

class AnotherCollection<T> : List<T>, AnotherListInterface<T> {
  override fun add(elem: T?): Int {
    return super<AnotherListInterface>.add(elem)
  }
}

abstract class AbstractCollectionWithDefaults<T> : Collection<T>

class FinalCollection<T> : AbstractCollectionWithDefaults<T>()

private fun testDefaultMethods() {
  assertEquals(COLLECTION_ADD, ACollection<Any>().add(null))
  assertEquals(ABSTRACT_COLLECTION_ADD, AConcreteList<Any>().add(null))
  assertEquals(COLLECTION_ADD, SomeOtherCollection<Any>().add(null))
  assertEquals(LIST_ADD, SomeOtherList<Any>().add(null))
  assertEquals(LIST_ADD, YetAnotherList<Any>().add(null))
  assertEquals(LIST_ADD, StringList().add(null))
  assertEquals(LIST_ADD, YetAnotherStringList().add(null))
  assertEquals(ANOTHER_STRING_LIST_ADD, AnotherStringList().add(null))
  assertEquals(ANOTHER_LIST_INTERFACE_ADD, AnotherCollection<Any>().add(null))
  assertEquals(COLLECTION_ADD, FinalCollection<Any>().add(null))
}

private fun testCompanionMethods() {
  assertEquals(1, List.returnOne())
}

interface InterfaceWithPrivateMethods {
  fun defaultMethod(): Int {
    return privateInstanceMethod()
  }

  private fun privateInstanceMethod(): Int {
    return m()
  }

  fun m(): Int

  companion object {
    private fun privateCompanionMethod(): Int {
      return mReturns1.privateInstanceMethod() + 1
    }

    fun callPrivateCompanionMethod(): Int {
      return privateCompanionMethod()
    }
  }
}

val mReturns1: InterfaceWithPrivateMethods =
  object : InterfaceWithPrivateMethods {
    override fun m(): Int {
      return 1
    }
  }

private fun testPrivateMethods() {
  assertEquals(2, InterfaceWithPrivateMethods.callPrivateCompanionMethod())
  assertEquals(2, InterfaceWithPrivateMethods.Companion.callPrivateCompanionMethod())
  assertEquals(1, mReturns1.defaultMethod())
}

internal abstract class AbstractClassWithFinalMethod {
  open fun run() = 2
}

internal open class ChildClassWithFinalMethod : AbstractClassWithFinalMethod(), SomeInterface

private fun testAccidentalOverrides() {
  val c = ChildClassWithFinalMethod()
  val a: AbstractClassWithFinalMethod = c
  assertTrue(run(c) == 2)
  assertTrue(c.run() == 2)
  assertTrue(a.run() == 2)
}

internal interface InterfaceWithDefaultMethod {
  fun defaultMethod(): String {
    return "default-method"
  }
}

private fun testDefaultMethods_superCall() {
  abstract class AbstractClass : InterfaceWithDefaultMethod

  class SubClass : AbstractClass() {
    override fun defaultMethod(): String {
      return super.defaultMethod()
    }
  }

  assertEquals("default-method", SubClass().defaultMethod())
}

val DIAMONDLEFT = "DiamondLeft"

interface DiamondLeft<T : DiamondLeft<T>> {
  fun name(t: T?): String {
    return DIAMONDLEFT
  }
}

val DIAMONDRIGHT = "DiamondRight"

interface DiamondRight<T : DiamondRight<T>> {
  fun name(t: T?): String {
    return DIAMONDRIGHT
  }
}

val BOTTOM = "Bottom"

interface Bottom<T : Bottom<T>> : DiamondLeft<T>, DiamondRight<T> {
  override fun name(t: T?): String {
    return BOTTOM
  }
}

open class A<T : DiamondLeft<T>, V : DiamondRight<V>> : DiamondLeft<T>, DiamondRight<V>

open class B : A<B, B>(), Bottom<B>

val CNAME = "C"

open class C : Bottom<C> {
  override fun name(c: C?): String {
    return CNAME
  }
}

private fun <T : DiamondLeft<T>, V : DiamondRight<V>> testDefaultMethods_diamondProperty() {
  val a: A<T, V> = A<T, V>()
  val dl: DiamondLeft<T> = a
  assertEquals(DIAMONDLEFT, dl.name(null))
  val dr: DiamondRight<V> = a
  assertEquals(DIAMONDRIGHT, dr.name(null))

  val b = B()
  val dlb: DiamondLeft<B> = b
  assertEquals(BOTTOM, dlb.name(null))
  val drb: DiamondRight<B> = b
  assertEquals(BOTTOM, drb.name(null))

  val c = C()
  val dlc: DiamondLeft<C> = c
  assertEquals(CNAME, dlc.name(null))
  val drc: DiamondRight<C> = c
  assertEquals(CNAME, drc.name(null))
  assertEquals(CNAME, c.name(null))
}
