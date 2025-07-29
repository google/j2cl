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
package enumobfuscation

import com.google.j2cl.integration.testing.Asserts.assertTrue

internal enum class Foo {
  FOO,
  FOZ,
}

internal enum class Bar(open var f: Int) {
  BAR(1),
  BAZ(Foo.FOO),
  BANG(5) {
    override var f: Int = 5
      get() = field + 2
  };

  companion object {
    var ENUM_SET = arrayOf(BAR, BAZ, BANG)
  }

  constructor(f: Foo) : this(f.ordinal) {}
}

fun main(vararg unused: String) {
  assertTrue(Foo.FOO.ordinal == 0)
  assertTrue(Foo.FOO.name == "FOO")
  assertTrue(Foo.FOZ.ordinal == 1)
  assertTrue(Foo.FOZ.name == "FOZ")
  assertTrue(Bar.BAR.ordinal == 0)
  assertTrue(Bar.BAR.f == 1)
  // Bar is expected to be obfuscated
  assertTrue(Bar.BAR.name != "BAR")
  assertTrue(Bar.BAZ.ordinal == 1)
  assertTrue(Bar.BAZ.f == 0)
  // Bar is expected to be obfuscated
  assertTrue(Bar.BAZ.name != "BAZ")
  assertTrue(Bar.BANG.ordinal == 2)
  assertTrue(Bar.BANG.f == 7)
  // Bar is expected to be obfuscated
  assertTrue(Bar.BANG.name != "BANG")

  // Check use-before-def assigning undefined
  // Although it isn't likely the test will make it this far
  for (b in Bar.ENUM_SET) {
    assertTrue(b != null)
  }

  // Assert idempotent name lookup non-obfuscated case
  for (foo in Foo.values()) {
    assertTrue(Foo.valueOf(foo.name) == foo)
  }

  // Assert idempotent name lookup for obfuscated case
  for (bar in Bar.values()) {
    assertTrue(Bar.valueOf(bar.name) == bar)
  }
}
