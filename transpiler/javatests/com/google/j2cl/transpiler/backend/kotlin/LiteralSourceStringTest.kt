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

import com.google.common.truth.Truth.assertThat
import com.google.j2cl.transpiler.ast.BooleanLiteral
import com.google.j2cl.transpiler.ast.NumberLiteral
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.StringLiteral
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LiteralSourceStringTest {
  @Test
  fun booleanLiteral() {
    assertThat(BooleanLiteral.get(false).sourceString).isEqualTo("false")
    assertThat(BooleanLiteral.get(true).sourceString).isEqualTo("true")
  }

  @Test
  fun intLiteral() {
    assertThat(NumberLiteral(PrimitiveTypes.INT, 123).sourceString).isEqualTo("123")
  }

  @Test
  fun longLiteral() {
    assertThat(NumberLiteral(PrimitiveTypes.LONG, 123L).sourceString).isEqualTo("123L")
  }

  @Test
  fun floatLiteral() {
    assertThat(NumberLiteral(PrimitiveTypes.FLOAT, 1.23f).sourceString).isEqualTo("1.23f")
  }

  @Test
  fun doubleLiteral() {
    assertThat(NumberLiteral(PrimitiveTypes.DOUBLE, 1.23).sourceString).isEqualTo("1.23")
  }

  @Test
  fun charLiteral() {
    assertThat(NumberLiteral(PrimitiveTypes.CHAR, 'a'.code).sourceString).isEqualTo("'a'")
  }

  @Test
  fun stringLiteral() {
    assertThat(StringLiteral("Hello, world!").sourceString).isEqualTo("\"Hello, world!\"")
  }
}
