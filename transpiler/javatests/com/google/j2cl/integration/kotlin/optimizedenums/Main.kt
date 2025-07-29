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
package optimizedenums

import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testOrdinal()
  testName()
  testInstanceMethod()
  testCompanionObjectFields()
  testEnumWithConstructors()
  testInstanceOf()
  testDefaultMethod()
  testPublicEnumInitialized()
  testPrivateEnumInitialized()
}

private fun testOrdinal() {
  assertTrue(SimpleEnum.FOO.ordinal == 0)
  assertTrue(SimpleEnum.BAR.ordinal == 1)
  assertTrue(EnumWithStaticField.FOO.ordinal == 0)
  assertTrue(EnumWithStaticField.BAR.ordinal == 1)
  assertTrue(OtherOptimizedEnum.FOO.ordinal == 0)
  assertTrue(OtherOptimizedEnum.BAR.ordinal == 1)
  assertTrue(EnumWithCtor.FOO.ordinal == 0)
  assertTrue(EnumWithCtor.BAR.ordinal == 1)
  assertTrue(EnumWithInstanceMethod.FOO.ordinal == 0)
  assertTrue(EnumWithInstanceMethod.BAR.ordinal == 1)
}

private fun testName() {
  assertTrue(SimpleEnum.FOO.name == "FOO")
  assertTrue(SimpleEnum.BAR.name == "BAR")
  assertTrue(EnumWithStaticField.FOO.name == "FOO")
  assertTrue(EnumWithStaticField.BAR.name == "BAR")
  assertTrue(OtherOptimizedEnum.FOO.name == "FOO")
  assertTrue(OtherOptimizedEnum.BAR.name == "BAR")
  assertTrue(EnumWithCtor.FOO.name == "FOO")
  assertTrue(EnumWithCtor.BAR.name == "BAR")
  assertTrue(EnumWithInstanceMethod.FOO.name == "FOO")
  assertTrue(EnumWithInstanceMethod.BAR.name == "BAR")
}

private fun testInstanceMethod() {
  assertTrue("S0" == EnumWithInstanceMethod.FOO.getString())
  assertTrue("S1" == EnumWithInstanceMethod.BAR.getString())
}

private fun testCompanionObjectFields() {
  assertTrue(EnumWithStaticField.ENUM_SET.size == 2)
  for (e in EnumWithStaticField.ENUM_SET) {
    assertTrue(e != null)
  }
  assertTrue(EnumWithStaticField.OTHER_ENUM_SET.size == 2)
  for (e in EnumWithStaticField.OTHER_ENUM_SET) {
    assertTrue(e != null)
  }
  assertTrue(EnumWithStaticField.field == null)
}

private fun testEnumWithConstructors() {
  assertTrue(EnumWithCtor.FOO.i == 0)
  assertTrue(EnumWithCtor.FOO.b)
  assertTrue(EnumWithCtor.FOO.string == "default")
  assertTrue(EnumWithCtor.FOO.stringConstant == "stringConstant")
  assertTrue(EnumWithCtor.BAR.i == 1)
  assertFalse(EnumWithCtor.BAR.b)
  assertTrue(EnumWithCtor.BAR.string == "bar")
  assertTrue(EnumWithCtor.BAR.stringConstant == "stringConstant")
  assertTrue(EnumWithCtor.BAZ.i == 2)
  assertFalse(EnumWithCtor.BAZ.b)
  assertTrue(EnumWithCtor.BAZ.string == "baz")
  assertTrue(EnumWithCtor.BAZ.stringConstant == "stringConstant")
}

private fun testInstanceOf() {
  val foo: Any = SimpleEnum.FOO
  assertTrue(foo is SimpleEnum)
  assertTrue(foo is Enum<*>)
  assertFalse(foo is EmptyEnum)
  val bar: Any = EnumWithInstanceMethod.BAR
  assertTrue(bar is EnumWithInstanceMethod)
  assertTrue(bar is MyEnumInterface)
}

private fun testDefaultMethod() {
  assertTrue(EnumWithInstanceMethod.FOO.getDefaultInstance().getString() == "defaultInstanceString")
}

private fun testPublicEnumInitialized() {
  assertTrue(publicEnumClinitCalled == 0)
  assertTrue(PubliEnumWithStaticInit.FOO.getString().equals("foo"))
  assertTrue(publicEnumClinitCalled == 1)
}

private fun testPrivateEnumInitialized() {
  assertTrue(privateEnumClinitCalled == 0)
  // Hide the call through polymorphic dispatch
  val foo: MyEnumInterface = PrivateEnumWithStaticInit.FOO
  assertTrue(foo.getString().equals("foo"))
  assertTrue(privateEnumClinitCalled == 1)
}

internal enum class SimpleEnum {
  FOO,
  BAR,
  UNREFERENCED_VALUE,
}

internal enum class EnumWithStaticField {
  FOO,
  BAR;

  companion object {
    fun f(o: Any?): EnumWithStaticField? {
      return null
    }

    var ENUM_SET = arrayOf(FOO, BAR)
    var OTHER_ENUM_SET = arrayOf(OtherOptimizedEnum.FOO, OtherOptimizedEnum.BAR)
    var field = f(Any())
  }
}

internal enum class OtherOptimizedEnum {
  FOO,
  BAR,
}

internal enum class EmptyEnum

internal enum class EnumWithCtor constructor(val i: Int, val b: Boolean, val string: String) {
  FOO,
  BAR(1, false, "bar"),
  BAZ(2, "baz");

  val stringConstant = "stringConstant"

  constructor() : this(0, true, "default") {}

  constructor(i: Int, string: String) : this(i, false, string) {}
}

internal enum class EnumWithInstanceMethod : MyEnumInterface {
  FOO,
  BAR;

  override fun getString(): String {
    return if (this == FOO) {
      "S0"
    } else {
      "S1"
    }
  }
}

internal interface MyEnumInterface {
  fun getString(): String

  fun getDefaultInstance(): MyEnumInterface =
    object : MyEnumInterface {
      override fun getString() = "defaultInstanceString"
    }
}

private var publicEnumClinitCalled = 0

private enum class PubliEnumWithStaticInit {
  FOO;

  companion object {
    init {
      publicEnumClinitCalled++
    }
  }

  // Supposed to be private to test clinit is still triggered, Kotlin doesn't allow that.
  fun getString(): String {
    return "foo"
  }
}

private var privateEnumClinitCalled = 0

private enum class PrivateEnumWithStaticInit : MyEnumInterface {
  FOO;

  companion object {
    init {
      privateEnumClinitCalled++
    }
  }

  override fun getString(): String {
    return "foo"
  }
}
