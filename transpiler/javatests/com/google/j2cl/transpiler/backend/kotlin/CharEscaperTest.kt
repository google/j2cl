/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.transpiler.backend.kotlin

import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CharEscaperTest {
  @Test
  fun charEscapedString() {
    assertEquals("a", 'a'.escapedString)
    assertEquals("z", 'z'.escapedString)
    assertEquals("0", '0'.escapedString)
    assertEquals("9", '9'.escapedString)
    assertEquals(" ", ' '.escapedString)

    assertEquals("\\\\", '\\'.escapedString)
    assertEquals("\\t", '\t'.escapedString)
    assertEquals("\\b", '\b'.escapedString)
    assertEquals("\\r", '\r'.escapedString)
    assertEquals("\\n", '\n'.escapedString)
    assertEquals("\\$", '\$'.escapedString)
    assertEquals("\\\'", '\''.escapedString)
    assertEquals("\\\"", '"'.escapedString)

    assertEquals("\\u0080", '\u0080'.escapedString)
  }

  @Test
  fun stringEscapedString() {
    assertEquals("Hello, world!", "Hello, world!".escapedString)
    assertEquals("\\\\\\t\\b\\n\\r\\'\\\"\\\$", "\\\t\b\n\r'\"\$".escapedString)
    assertEquals("\\u001F\\u0080", "\u001f\u0080".escapedString)
    assertEquals("\\\$foo", "\$foo".escapedString)
    assertEquals("\\\${foo.bar}", "\${foo.bar}".escapedString)
    assertEquals(
      "\\uD800\\uDC00",
      StringBuilder().appendCodePoint(0x10000).toString().escapedString
    )
  }
}
