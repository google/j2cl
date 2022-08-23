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
package com.google.j2cl.transpiler.backend.kotlin

import com.google.j2cl.common.InternalCompilerError
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDescriptor

internal fun Renderer.renderQualifiedName(typeDescriptor: TypeDescriptor) {
  when (typeDescriptor) {
    is ArrayTypeDescriptor -> renderArrayQualifiedName(typeDescriptor)
    is DeclaredTypeDescriptor -> renderDeclaredQualifiedName(typeDescriptor)
    is PrimitiveTypeDescriptor -> renderPrimitiveQualifiedName(typeDescriptor)
    else -> throw InternalCompilerError("Unhandled $typeDescriptor")
  }
}

private fun Renderer.renderArrayQualifiedName(arrayTypeDescriptor: ArrayTypeDescriptor) {
  when (arrayTypeDescriptor.componentTypeDescriptor!!) {
    PrimitiveTypes.BOOLEAN -> render("kotlin.BooleanArray")
    PrimitiveTypes.CHAR -> render("kotlin.CharArray")
    PrimitiveTypes.BYTE -> render("kotlin.ByteArray")
    PrimitiveTypes.SHORT -> render("kotlin.ShortArray")
    PrimitiveTypes.INT -> render("kotlin.IntArray")
    PrimitiveTypes.LONG -> render("kotlin.LongArray")
    PrimitiveTypes.FLOAT -> render("kotlin.FloatArray")
    PrimitiveTypes.DOUBLE -> render("kotlin.DoubleArray")
    else -> render("kotlin.Array")
  }
}

private fun Renderer.renderDeclaredQualifiedName(declaredTypeDescriptor: DeclaredTypeDescriptor) {
  renderQualifiedName(declaredTypeDescriptor.typeDeclaration.ktQualifiedName)
}

private fun Renderer.renderPrimitiveQualifiedName(
  primitiveTypeDescriptor: PrimitiveTypeDescriptor
) {
  render(
    when (primitiveTypeDescriptor) {
      PrimitiveTypes.VOID -> "kotlin.Unit"
      PrimitiveTypes.BOOLEAN -> "kotlin.Boolean"
      PrimitiveTypes.CHAR -> "kotlin.Char"
      PrimitiveTypes.BYTE -> "kotlin.Byte"
      PrimitiveTypes.SHORT -> "kotlin.Short"
      PrimitiveTypes.INT -> "kotlin.Int"
      PrimitiveTypes.LONG -> "kotlin.Long"
      PrimitiveTypes.FLOAT -> "kotlin.Float"
      PrimitiveTypes.DOUBLE -> "kotlin.Double"
      else -> throw InternalCompilerError("Unhandled $primitiveTypeDescriptor")
    }
  )
}
