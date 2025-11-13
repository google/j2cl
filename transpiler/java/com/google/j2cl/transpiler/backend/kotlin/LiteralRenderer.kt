/*
 * Copyright 2025 Google Inc.
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

import com.google.j2cl.common.InternalCompilerError
import com.google.j2cl.transpiler.ast.BooleanLiteral
import com.google.j2cl.transpiler.ast.Literal
import com.google.j2cl.transpiler.ast.NullLiteral
import com.google.j2cl.transpiler.ast.NumberLiteral
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.StringLiteral
import com.google.j2cl.transpiler.ast.TypeLiteral
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.NULL_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.classLiteral
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.literal
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.nonNull
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.dotSeparated

internal data class LiteralRenderer(val nameRenderer: NameRenderer) {
  fun literalSource(literal: Literal): Source =
    when (literal) {
      is NullLiteral -> NULL_KEYWORD
      is BooleanLiteral -> booleanLiteralSource(literal)
      is StringLiteral -> stringLiteralSource(literal)
      is TypeLiteral -> typeLiteralSource(literal)
      is NumberLiteral -> numberLiteralSource(literal)
    }

  private fun booleanLiteralSource(booleanLiteral: BooleanLiteral): Source =
    literal(booleanLiteral.value)

  private fun stringLiteralSource(stringLiteral: StringLiteral): Source =
    literal(stringLiteral.value)

  private fun typeLiteralSource(typeLiteral: TypeLiteral): Source =
    dotSeparated(
      classLiteral(nameRenderer.qualifiedNameSource(typeLiteral.referencedTypeDescriptor)),
      if (typeLiteral.referencedTypeDescriptor.isPrimitive) {
        nonNull(nameRenderer.extensionMemberQualifiedNameSource("kotlin.jvm.javaPrimitiveType"))
      } else {
        nameRenderer.extensionMemberQualifiedNameSource("kotlin.jvm.javaObjectType")
      },
    )

  private fun numberLiteralSource(numberLiteral: NumberLiteral): Source =
    when (numberLiteral.typeDescriptor.toUnboxedType()) {
      PrimitiveTypes.CHAR -> literal(numberLiteral.value.toInt().toChar())
      PrimitiveTypes.INT -> literal(numberLiteral.value.toInt())
      PrimitiveTypes.LONG -> literal(numberLiteral.value.toLong())
      PrimitiveTypes.FLOAT -> literal(numberLiteral.value.toFloat())
      PrimitiveTypes.DOUBLE -> literal(numberLiteral.value.toDouble())
      else -> throw InternalCompilerError("renderNumberLiteral($numberLiteral)")
    }
}
