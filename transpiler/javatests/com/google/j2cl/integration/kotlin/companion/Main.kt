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
package companion

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testCompanionReferences()
  testEmptyCompanion()
  testCompanionInheritance()
  testInterfaceWithCompanion()
  testEnumWithCompanion()
  testCompanionFieldInitCycle()
  testCompanionWithLambdas()
}

private open class A(x: Int) {
  open val y: Int = x

  fun parentFunction(): Int = 5
}

private interface CompanionInterface {
  fun compute(a: Int): Int
}

private class ClassWithCompanion {
  companion object Named : A(5), CompanionInterface {
    var propertyinitializedFromInitializerBlock: Int
    val propertyWithInitializer: String = "Foo"
    override val y: Int = 15

    init {
      propertyinitializedFromInitializerBlock = 1
    }

    override fun compute(a: Int): Int = propertyinitializedFromInitializerBlock + a

    fun create(): ClassWithCompanion = ClassWithCompanion()
  }

  fun useCompanionMembers(a: Int): Int {
    return propertyinitializedFromInitializerBlock + Named.compute(a)
  }
}

var sideEffectsCount = 0

private class TestOptimizableCompanion {
  val foo = "Foo"

  companion object {
    fun getCompanionWithSideEffect(): Companion {
      sideEffectsCount++
      return Companion
    }

    @JvmStatic fun foo(unused: Int?): Int = 2

    fun foo(unused: Int): Int = 3

    fun compute(a: Int) = sideEffectsCount * a
  }
}

private fun testCompanionReferences() {
  assertTrue(ClassWithCompanion.propertyWithInitializer == "Foo")
  assertTrue(ClassWithCompanion.Named.propertyWithInitializer == "Foo")

  assertTrue(ClassWithCompanion.propertyinitializedFromInitializerBlock == 1)
  assertTrue(ClassWithCompanion.Named.propertyinitializedFromInitializerBlock == 1)

  assertTrue(ClassWithCompanion.compute(2) == 3)
  assertTrue(ClassWithCompanion.Named.compute(2) == 3)

  assertTrue(ClassWithCompanion.create().useCompanionMembers(2) == 4)

  sideEffectsCount = 0
  assertTrue(TestOptimizableCompanion.getCompanionWithSideEffect().compute(2) == 2)
  assertTrue(sideEffectsCount == 1)

  assertTrue(TestOptimizableCompanion.foo(2) == 3)
  assertTrue(TestOptimizableCompanion.foo(null) == 2)
  val i: Int? = 4
  assertTrue(TestOptimizableCompanion.foo(i) == 2)
}

private class ClassWithEmptyCompanion {
  companion object

  fun getCompanionInstance(): Any {
    return Companion
  }
}

private fun testEmptyCompanion() {
  assertTrue(ClassWithEmptyCompanion.Companion == ClassWithEmptyCompanion().getCompanionInstance())
}

private fun consumeMyInterface(i: CompanionInterface): Int {
  return i.compute(5)
}

private fun consumeA(a: A): Int {
  return a.y
}

class CompanionOverridingObjectMethods {
  val unused = ""

  interface Foo {
    fun explicitThisReference(): Int
  }

  companion object {
    val foo =
      object : Foo {
        override fun explicitThisReference(): Int = this@Companion.hashCode()
      }

    override fun hashCode(): Int {
      return 100
    }

    override fun toString(): String {
      return "CompanionOverridingObjectMethods.Companion"
    }

    override fun equals(other: Any?): Boolean {
      if (other == null) {
        return true
      }
      return super.equals(other)
    }
  }
}

private fun testCompanionInheritance() {
  assertTrue(ClassWithCompanion.Named is CompanionInterface)
  assertTrue(ClassWithCompanion.Named is A)
  assertTrue(consumeMyInterface(ClassWithCompanion.Named) == 6)
  assertTrue(consumeA(ClassWithCompanion.Named) == 15)
  assertTrue(ClassWithCompanion.Named.parentFunction() == 5)

  assertTrue(CompanionOverridingObjectMethods.hashCode() == 100)
  assertEquals(
    CompanionOverridingObjectMethods.toString(),
    "CompanionOverridingObjectMethods.Companion",
  )
  assertTrue(CompanionOverridingObjectMethods.equals(null))
  assertTrue(CompanionOverridingObjectMethods.equals(CompanionOverridingObjectMethods.Companion))
  assertTrue(!CompanionOverridingObjectMethods.equals(Any()))
  assertTrue(CompanionOverridingObjectMethods.foo.explicitThisReference() == 100)
}

private interface InterfaceWithCompanion {
  companion object {
    var property: Int = 5

    fun companionFun(): Int = 6
  }
}

private fun testInterfaceWithCompanion() {
  assertTrue(InterfaceWithCompanion.property == 5)
  assertTrue(InterfaceWithCompanion.Companion.property == 5)

  assertTrue(InterfaceWithCompanion.companionFun() == 6)
  assertTrue(InterfaceWithCompanion.Companion.companionFun() == 6)
}

private enum class EnumWithCompanion {
  A,
  B;

  companion object {
    var property: Int = 6

    fun defaultEnum(): EnumWithCompanion = A
  }
}

private fun testEnumWithCompanion() {
  assertTrue(EnumWithCompanion.property == 6)
  assertTrue(EnumWithCompanion.Companion.property == 6)

  assertTrue(EnumWithCompanion.defaultEnum() == EnumWithCompanion.A)
  assertTrue(EnumWithCompanion.Companion.defaultEnum() == EnumWithCompanion.A)
}

@SuppressWarnings("ClassShouldBeObject")
class C {
  companion object {
    val x: Any = D.c
  }
}

@SuppressWarnings("ClassShouldBeObject")
class D {
  companion object {
    val a: Any = Any()
    val b: Any = C.x
    val c: Any = Any()
  }
}

private fun testCompanionFieldInitCycle() {
  assertNotNull(D.a)
  assertNotNull(D.c)
  // This seems like it should be impossible but alas: https://youtrack.jetbrains.com/issue/KT-8970
  // Sadly this leaks a null into a non-nullable type :(
  assertNull(D.b)
}

fun interface IntToIntFunction {
  fun apply(i: Int): Int
}

@SuppressWarnings("ClassShouldBeObject")
class CompanionWithLambdas {
  companion object {
    var intProperty = 10

    override fun hashCode() = super.hashCode() + 1

    fun lambdaWithThisReference(): IntToIntFunction {
      return IntToIntFunction { i: Int -> i + this.intProperty }
    }

    fun lambdaWithSuperReference(): IntToIntFunction {
      return IntToIntFunction { i: Int -> i + super.hashCode() }
    }

    fun lambdaWithLocalClassAndThisAndSuperReferences(): IntToIntFunction {
      return IntToIntFunction { i: Int ->
        class Local {
          fun withSuper(i: Int) = i + super@Companion.hashCode()

          fun withThis(i: Int) = i + this@Companion.intProperty
        }

        Local().withSuper(i) + Local().withThis(i)
      }
    }
  }
}

private fun testCompanionWithLambdas() {
  assertTrue(CompanionWithLambdas.lambdaWithThisReference().apply(1) == 11)
  assertTrue(
    CompanionWithLambdas.lambdaWithSuperReference().apply(1) == CompanionWithLambdas.hashCode()
  )
  assertTrue(
    CompanionWithLambdas.lambdaWithLocalClassAndThisAndSuperReferences().apply(1) ==
      (CompanionWithLambdas.hashCode() + 11)
  )
}
