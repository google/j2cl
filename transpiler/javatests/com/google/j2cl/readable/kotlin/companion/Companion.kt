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

import jsinterop.annotations.JsType

class Companion {
  companion object NamedCompanion {
    class Foo {
      fun foo() = companionFunction("", 1)
    }

    val bar =
      object {
        fun foo(): Any {
          return object {
            fun innerFoo() = companionFunction(bar(), 2)
          }
        }

        fun bar() = "bar"
      }

    var propertyinitializedFromInitializerBlock: Int
    val propertyWithInitializer: String = "Foo"

    init {
      propertyinitializedFromInitializerBlock = 1
    }

    fun companionFunction(p1: String, p2: Int) {}
  }

  fun testCompanionReferencesFromEnclosingClass() {
    companionFunction(propertyWithInitializer, propertyinitializedFromInitializerBlock)
  }
}

class DefaultCompanionName {
  companion object {
    const val compileTimeConstant = "foo"

    val nonCompileTimeConstant = Any()

    var property: Any = Any()

    fun companionFunction(p: Any) = compileTimeConstant + nonCompileTimeConstant + super.toString()

    fun clashingWithInstanceMethod(): Boolean = false

    fun clashingWithInstanceMethodButDifferentReturnType(): Int = 1

    fun `$isInstance`(o: Any): Boolean = true
  }

  fun clashingWithInstanceMethod(): Boolean = true

  fun clashingWithInstanceMethodButDifferentReturnType(): Boolean = true

  fun testCompanionReferencesFromEnclosingClass() {
    companionFunction(property)
  }
}

class CompanionWithJvmAnnotations {
  companion object {
    @JvmField var jvmFieldProperty = 1
    @JvmField val jvmFieldNonCompileTimeConstant = 1

    @JvmStatic var jvmStaticProperty = 1
    @JvmStatic val jvmStaticNonCompileTimeConstant = 1

    init {
      for (i in 0..10) {
        jvmFieldProperty *= i
        jvmStaticProperty *= i
      }
    }

    @JvmStatic
    fun jvmStaticFunction() = jvmStaticNonCompileTimeConstant * jvmFieldNonCompileTimeConstant
  }
}

@JsType
open class JsTypeWithCompanion() {
  companion object {
    internal const val CONST_FIELD: Int = 0
  }
}

enum class EnumWithCompanion {
  FOO,
  BAR,
  BAZ,
  // This make the enum non optimizeable and the enum entry are initialized during clinit before
  // initializing the companion instance field.
  BANG {
    override fun function(): Int {
      return 2
    }
  };

  open fun function(): Int {
    return 1
  }

  companion object {
    var ENUM_SET = arrayOf(FOO, BAR, BAZ, BANG)
  }
}

interface InterfaceWithCompanion {
  fun foo(): Int

  companion object {
    const val compileTimeConstant = "foo"

    val nonCompileTimeConstant = Any()

    var property = Any()

    init {
      for (i in 0..10) {
        property = Any()
      }
    }

    fun publicFunction(): String = InterfaceWithCompanion.privateFunction()

    private fun privateFunction(): String = compileTimeConstant + nonCompileTimeConstant
  }
}

fun companionReferencesFromExternal() {
  DefaultCompanionName.Companion.companionFunction(DefaultCompanionName.Companion.property)
  DefaultCompanionName.companionFunction(DefaultCompanionName.property)

  Companion.companionFunction(
    Companion.propertyWithInitializer,
    Companion.propertyinitializedFromInitializerBlock,
  )
  Companion.NamedCompanion.companionFunction(
    Companion.NamedCompanion.propertyWithInitializer,
    Companion.NamedCompanion.propertyinitializedFromInitializerBlock,
  )

  for (e in EnumWithCompanion.ENUM_SET) {}

  val a = InterfaceWithCompanion.compileTimeConstant
  val b = InterfaceWithCompanion.nonCompileTimeConstant
  val c = InterfaceWithCompanion.property
  val d = InterfaceWithCompanion.publicFunction()

  ThirdPartyLibClass.function(ThirdPartyLibClass.property)
  val e = ThirdPartyLibClass.compileTimeConstant
  val f = ThirdPartyLibClass.nonCompileTimeConstant

  ThirdPartyInterface.function(ThirdPartyInterface.property)
  val g = ThirdPartyInterface.compileTimeConstant
  val h = ThirdPartyInterface.nonCompileTimeConstant
}

fun interface IntToIntFunction {
  fun apply(i: Int): Int
}

@SuppressWarnings("ClassShouldBeObject")
class CompanionWithLambdas {
  val foo = ""

  companion object {
    var intProperty = 0

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
