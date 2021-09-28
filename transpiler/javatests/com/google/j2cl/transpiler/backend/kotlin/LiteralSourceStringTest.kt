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

import com.google.j2cl.common.SourcePosition
import com.google.j2cl.transpiler.ast.BooleanLiteral
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.Kind
import com.google.j2cl.transpiler.ast.NumberLiteral
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.StringLiteral
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeLiteral
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LiteralSourceStringTest {
  @Test
  fun literalSourceString() {
    assertEquals("false", BooleanLiteral.get(false).sourceString)
    assertEquals("true", BooleanLiteral.get(true).sourceString)

    assertEquals("0", NumberLiteral(PrimitiveTypes.INT, 0).sourceString)
    assertEquals("0L", NumberLiteral(PrimitiveTypes.LONG, 0L).sourceString)
    assertEquals("0.0f", NumberLiteral(PrimitiveTypes.FLOAT, 0.0f).sourceString)
    assertEquals("0.0", NumberLiteral(PrimitiveTypes.DOUBLE, 0.0).sourceString)

    assertEquals("'a'", NumberLiteral(PrimitiveTypes.CHAR, 'a'.code).sourceString)

    assertEquals("\"Hello, world!\"", StringLiteral("Hello, world!").sourceString)
    assertEquals(
      "Foo::class.java",
      TypeLiteral(
          SourcePosition.NONE,
          DeclaredTypeDescriptor.newBuilder()
            .setTypeDeclaration(
              TypeDeclaration.newBuilder()
                .setKind(Kind.CLASS)
                .setClassComponents(listOf("Foo"))
                .build()
            )
            .build()
        )
        .sourceString
    )
  }
}
