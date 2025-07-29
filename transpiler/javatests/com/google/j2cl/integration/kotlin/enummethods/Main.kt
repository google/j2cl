/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package enummethods

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod

/** Checks that .name() and .ordinal() are callable from javascript */
fun main(vararg unused: String) {
  assertTrue(getOrdinal(MyEnum.A) == 0)
  assertTrue(getOrdinal(MyEnum.B) == 1)
  assertTrue(getOrdinal(MyEnum.C) == 2)
  assertTrue(getName(MyEnum.A) === "A")
  assertTrue(getName(MyEnum.B) === "B")
  assertTrue(getName(MyEnum.C) === "C")
  assertTrue(compare(MyEnum.A, MyEnum.A) == 0)
  assertTrue(compare(MyEnum.B, MyEnum.A) > 0)
  assertTrue(compare(MyEnum.A, MyEnum.C) < 0)
}

@JsMethod(namespace = "foo.test.Test") external fun getOrdinal(someEnum: Enum<MyEnum>): Int

@JsMethod(namespace = "foo.test.Test") external fun getName(someEnum: Enum<MyEnum>): String

@JsMethod(namespace = "foo.test.Test")
external fun compare(enumA: Enum<MyEnum>, enumB: Enum<MyEnum>): Int

enum class MyEnum {
  A,
  B,
  C,
}
