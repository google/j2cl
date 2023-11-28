/*
 * Copyright 2023 Google Inc.
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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CharEscaperTest {
  @Test
  fun charLiteralString_nonEscaping() {
    assertThat('a'.literalString).isEqualTo("'a'")
    assertThat('A'.literalString).isEqualTo("'A'")
    assertThat('0'.literalString).isEqualTo("'0'")
    assertThat(' '.literalString).isEqualTo("' '")
  }

  @Test
  fun charLiteralString_identifierEscaping() {
    assertThat('\t'.literalString).isEqualTo("""'\t'""")
    assertThat('\b'.literalString).isEqualTo("""'\b'""")
    assertThat('\n'.literalString).isEqualTo("""'\n'""")
    assertThat('\r'.literalString).isEqualTo("""'\r'""")
    assertThat('\''.literalString).isEqualTo("""'\''""")
    assertThat('\"'.literalString).isEqualTo("""'\"'""")
    assertThat('$'.literalString).isEqualTo("""'\$'""")
  }

  @Test
  fun charLiteralString_dollarSignEscaping() {
    assertThat('$'.literalString).isEqualTo("""'\$'""")
  }

  @Test
  fun charLiteralString_unicodeEscaping() {
    assertThat('\u0000'.literalString).isEqualTo("""'\u0000'""")
    assertThat('\u001F'.literalString).isEqualTo("""'\u001F'""")
    assertThat('\u0100'.literalString).isEqualTo("""'\u0100'""")
    assertThat('\u1234'.literalString).isEqualTo("""'\u1234'""")
  }

  @Test
  fun stringLiteralString_nonEscaping() {
    assertThat("aA0 ".literalString).isEqualTo(""""aA0 """")
  }

  @Test
  fun stringLiteralString_identifierEscaping() {
    assertThat("\t\b\n\r\'\"".literalString).isEqualTo(""""\t\b\n\r\'\""""")
  }

  @Test
  fun stringLiteralString_dollarSignEscaping() {
    assertThat("\$foo".literalString).isEqualTo("\"\\\$foo\"")
  }

  @Test
  fun stringLiteralString_unicodeEscaping() {
    assertThat("\u0000\u001F\u0100\u1234".literalString).isEqualTo(""""\u0000\u001F\u0100\u1234"""")
  }
}
