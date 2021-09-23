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

import com.google.j2cl.common.InternalCompilerError
import com.google.j2cl.transpiler.ast.BooleanLiteral
import com.google.j2cl.transpiler.ast.Literal
import com.google.j2cl.transpiler.ast.NullLiteral
import com.google.j2cl.transpiler.ast.NumberLiteral
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.StringLiteral

/** Returns Kotlin source string representation of this literal. */
val Literal.sourceString: String
  get() =
    when (this) {
      is NullLiteral -> "null"
      is BooleanLiteral -> "$value"
      is StringLiteral -> "\"${value.escapedString}\""
      is NumberLiteral ->
        when (typeDescriptor.toUnboxedType()) {
          PrimitiveTypes.CHAR -> "'${value.toChar().escapedString}'"
          PrimitiveTypes.BYTE -> "${value.toByte()}"
          PrimitiveTypes.SHORT -> "${value.toShort()}"
          PrimitiveTypes.INT -> "${value.toInt()}"
          PrimitiveTypes.LONG -> "${value.toLong()}L"
          PrimitiveTypes.FLOAT -> "${value.toFloat()}f"
          PrimitiveTypes.DOUBLE -> "${value.toDouble()}"
          else -> throw InternalCompilerError("Unhandled $this")
        }
      else -> throw InternalCompilerError("Unhandled $this}")
    }
