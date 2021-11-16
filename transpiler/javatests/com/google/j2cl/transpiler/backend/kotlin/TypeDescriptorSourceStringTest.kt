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
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.Kind
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeVariable
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private val TypeDescriptor.array: TypeDescriptor
  get() = ArrayTypeDescriptor.newBuilder().setComponentTypeDescriptor(this).build()

@RunWith(JUnit4::class)
class TypeDescriptorSourceStringTest {
  @Test
  fun primitiveType() {
    assertThat(PrimitiveTypes.VOID.sourceString).isEqualTo("Unit")
    assertThat(PrimitiveTypes.BOOLEAN.sourceString).isEqualTo("Boolean")
    assertThat(PrimitiveTypes.CHAR.sourceString).isEqualTo("Char")
    assertThat(PrimitiveTypes.BYTE.sourceString).isEqualTo("Byte")
    assertThat(PrimitiveTypes.SHORT.sourceString).isEqualTo("Short")
    assertThat(PrimitiveTypes.INT.sourceString).isEqualTo("Int")
    assertThat(PrimitiveTypes.LONG.sourceString).isEqualTo("Long")
    assertThat(PrimitiveTypes.FLOAT.sourceString).isEqualTo("Float")
    assertThat(PrimitiveTypes.DOUBLE.sourceString).isEqualTo("Double")
  }

  @Test
  fun arrayType_primitiveTypes() {
    assertThat(PrimitiveTypes.BOOLEAN.array.sourceString).isEqualTo("BooleanArray?")
    assertThat(PrimitiveTypes.CHAR.array.sourceString).isEqualTo("CharArray?")
    assertThat(PrimitiveTypes.BYTE.array.sourceString).isEqualTo("ByteArray?")
    assertThat(PrimitiveTypes.SHORT.array.sourceString).isEqualTo("ShortArray?")
    assertThat(PrimitiveTypes.INT.array.sourceString).isEqualTo("IntArray?")
    assertThat(PrimitiveTypes.LONG.array.sourceString).isEqualTo("LongArray?")
    assertThat(PrimitiveTypes.FLOAT.array.sourceString).isEqualTo("FloatArray?")
    assertThat(PrimitiveTypes.DOUBLE.array.sourceString).isEqualTo("DoubleArray?")
  }

  @Test
  fun arrayType_manyDimensions() {
    assertThat(PrimitiveTypes.BYTE.array.array.sourceString).isEqualTo("Array<ByteArray?>?")
    assertThat(PrimitiveTypes.BYTE.array.array.array.sourceString)
      .isEqualTo("Array<Array<ByteArray?>?>?")
  }

  @Test
  fun declaredTypeInTopLevelPackage() {
    assertThat(
        DeclaredTypeDescriptor.newBuilder()
          .setTypeDeclaration(
            TypeDeclaration.newBuilder()
              .setKind(Kind.CLASS)
              .setClassComponents(listOf("Foo"))
              .build()
          )
          .build()
          .sourceString
      )
      .isEqualTo("Foo?")
  }

  @Test
  fun declaredTypeInPackage() {
    assertThat(
        DeclaredTypeDescriptor.newBuilder()
          .setTypeDeclaration(
            TypeDeclaration.newBuilder()
              .setKind(Kind.CLASS)
              .setPackageName("outer.inner")
              .setClassComponents(listOf("Foo"))
              .build()
          )
          .build()
          .sourceString
      )
      .isEqualTo("outer.inner.Foo?")
  }

  @Test
  fun declaredTypeInner() {
    assertThat(
        DeclaredTypeDescriptor.newBuilder()
          .setTypeDeclaration(
            TypeDeclaration.newBuilder()
              .setKind(Kind.CLASS)
              .setClassComponents(listOf("Outer", "Inner"))
              .build()
          )
          .build()
          .sourceString
      )
      .isEqualTo("Outer.Inner?")
  }

  @Test
  fun declaredTypeWithArguments() {
    assertThat(
        DeclaredTypeDescriptor.newBuilder()
          .setTypeDeclaration(
            TypeDeclaration.newBuilder()
              .setKind(Kind.CLASS)
              .setClassComponents(listOf("Foo"))
              .build()
          )
          .setTypeArgumentDescriptors(listOf(PrimitiveTypes.INT, PrimitiveTypes.FLOAT))
          .build()
          .sourceString
      )
      .isEqualTo("Foo<Int, Float>?")
  }

  @Test
  fun typeVariable() {
    assertThat(
        TypeVariable.newBuilder()
          .setName("T")
          .setBoundTypeDescriptorSupplier { PrimitiveTypes.INT }
          .build()
          .sourceString
      )
      .isEqualTo("T?")
  }

  @Test
  fun typeVariableWithWildcard() {
    assertThat(TypeVariable.createWildcardWithBound(PrimitiveTypes.INT).sourceString).isEqualTo("*")
  }
}
