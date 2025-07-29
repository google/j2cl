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
package rawstring

import com.google.j2cl.integration.testing.Asserts.assertEquals

fun main(vararg unused: String) {
  testRawString()
  testRawStringWithEscapedCharacters()
  testRawStringWithStringTemplate()
}

fun testRawString() {
  val a =
    """Tell me and I forget.
  Teach me and I remember.
  Involve me and I learn."""
  val b = "Tell me and I forget.\n  Teach me and I remember.\n  Involve me and I learn."
  assertEquals(a, b)
}

fun testRawStringWithEscapedCharacters() {
  assertEquals("\\n will not be substituted.", """\n will not be substituted.""")

  val rawStringWithStringTemplate = """This ${'$'}{variable} will not be substituted."""
  assertEquals("This \${variable} will not be substituted.", rawStringWithStringTemplate)
}

fun testRawStringWithStringTemplate() {
  val variable = "number"
  val rawStringWithStringTemplate = """This ${variable} will be substituted."""
  assertEquals("This number will be substituted.", rawStringWithStringTemplate)
}
