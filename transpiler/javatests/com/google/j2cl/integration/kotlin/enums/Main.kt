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
package enums

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import java.util.function.Function

/** Contains test for enums that cannot be optimized according to go/j2cl-enums-optimization. */
fun main(vararg unused: String) {
  testOrdinal()
  testName()
  testInstanceMethod()
  testEnumInCompanionObject()
  testEnumInitializedWithLambdas()
  testEnumWithConstructors()
  testEnumWithOverriddenMethodsInSeparateLibrary()
}

private fun testOrdinal() {
  assertTrue(Foo.FOO.ordinal == 0)
  assertTrue(Foo.FOZ.ordinal == 1)
  assertTrue(Bar.FOO.ordinal == 0)
  assertTrue(Bar.BAR.ordinal == 1)
  assertTrue(Bar.BAZ.ordinal == 2)
  assertTrue(Bar.BANG.ordinal == 3)
}

private fun testName() {
  assertTrue(Foo.FOO.name == "FOO")
  assertTrue(Foo.FOZ.name == "FOZ")
  assertTrue(Bar.BAR.name == "BAR")
  assertTrue(Bar.BAZ.name == "BAZ")
  assertTrue(Bar.BANG.name == "BANG")
}

private fun testInstanceMethod() {
  assertTrue(Bar.FOO.getFMethod() == -1)
  assertTrue(Bar.BAR.getFMethod() == 1)
  assertTrue(Bar.BAZ.getFMethod() == 0)
  assertTrue(Bar.BANG.getFMethod() == 7)
}

private fun testEnumInCompanionObject() {
  // Check use-before-def assigning undefined
  // Although it isn't likely the test will make it this far
  for (b in Bar.ENUM_SET) {
    assertTrue(b != null)
  }
  assertTrue(Bar.staticField == null)
}

private fun testEnumWithConstructors() {
  assertTrue(Bar.FOO.f == -1)
  assertTrue(Bar.BAR.f == 1)
  assertTrue(Bar.BAZ.f == 0)
  assertTrue(Bar.BANG.f == 5)
}

internal enum class Foo {
  FOO,
  FOZ,
  UNREFERENCED_VALUE,
  UNREFERENCED_SUBCLASS_VALUE {
    override fun toString() = "FOO"
  },
}

@SuppressWarnings("ImmutableEnumChecker")
internal enum class Bar constructor(var f: Int) {
  FOO,
  BAR(1),
  BAZ(Foo.FOO),
  BANG(5) {
    override fun getFMethod(): Int {
      return f + 2
    }
  };

  constructor() : this(-1)

  constructor(f: Foo) : this(f.ordinal)

  open fun getFMethod(): Int {
    return f
  }

  companion object {
    fun sf(o: Any?): Bar? {
      return null
    }

    var ENUM_SET = arrayOf(BAR, BAZ, BANG)
    var staticField = sf(null)
  }
}

private fun testEnumInitializedWithLambdas() {
  assertTrue("Plus1" == Functions.PLUS1.function.apply("UNUSED"))
  assertTrue("Minus1" == Functions.MINUS1.function.apply("UNUSED"))
}

@SuppressWarnings("ImmutableEnumChecker")
internal enum class Functions(val function: Function<String, String>) {
  PLUS1({ _: String -> "Plus1" }),
  MINUS1({ _: String -> "Minus1" }),
}

private fun testEnumWithOverriddenMethodsInSeparateLibrary() {
  // Repro for b/341721484.
  assertEquals("A", EnumWithOverriddenMethods.A.getConstName())
  assertEquals("B", EnumWithOverriddenMethods.B.getConstName())
}
