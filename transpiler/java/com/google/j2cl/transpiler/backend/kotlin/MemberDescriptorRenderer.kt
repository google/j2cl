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

import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.CONSTRUCTOR_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.ast.Keywords
import com.google.j2cl.transpiler.backend.kotlin.common.inBackTicks
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty
import java.lang.Boolean.getBoolean

/**
 * Renderer of member descriptors.
 *
 * @param nameRenderer underlying name renderer
 */
internal data class MemberDescriptorRenderer(val nameRenderer: NameRenderer) {
  fun methodKindAndNameSource(methodDescriptor: MethodDescriptor): Source =
    if (methodDescriptor.isConstructor) {
      CONSTRUCTOR_KEYWORD
    } else {
      spaceSeparated(
        if (methodDescriptor.isKtProperty) KotlinSource.VAL_KEYWORD else KotlinSource.FUN_KEYWORD,
        nameRenderer.typeParametersSource(methodDescriptor.typeParameterTypeDescriptors),
        identifierSource(methodDescriptor.ktMangledName),
      )
    }

  fun methodReturnTypeSource(methodDescriptor: MethodDescriptor): Source =
    methodDescriptor.returnTypeDescriptor
      .takeIf { it != PrimitiveTypes.VOID }
      ?.let { nameRenderer.typeDescriptorSource(it) }
      .orEmpty()

  fun jvmThrowsAnnotationSource(methodDescriptor: MethodDescriptor): Source =
    methodDescriptor.exceptionTypeDescriptors
      .takeIf { it.isNotEmpty() }
      ?.let { exceptionTypeDescriptors ->
        KotlinSource.annotation(
          nameRenderer.topLevelQualifiedNameSource("kotlin.jvm.Throws"),
          exceptionTypeDescriptors.map {
            KotlinSource.classLiteral(
              nameRenderer.typeDescriptorSource(it.toRawTypeDescriptor().toNonNullable())
            )
          },
        )
      }
      .orEmpty()

  fun nativeThrowsAnnotationSource(methodDescriptor: MethodDescriptor): Source =
    methodDescriptor.ktInfo
      .takeIf { it.isThrows && SHOULD_RENDER_NATIVE_THROWS }
      ?.let {
        KotlinSource.annotation(
          nameRenderer.topLevelQualifiedNameSource("javaemul.lang.NativeThrows"),
          KotlinSource.classLiteral(
            nameRenderer.typeDescriptorSource(
              TypeDescriptors.get().javaLangThrowable.toNonNullable()
            )
          ),
        )
      }
      .orEmpty()

  companion object {
    // TODO(b/316324154): Remove when no longer necessary
    val SHOULD_RENDER_NATIVE_THROWS: Boolean =
      !getBoolean("com.google.j2cl.transpiler.backend.kotlin.isNativeThrowsDisabled")

    val MethodDescriptor.methodModifiersSource: Source
      get() =
        spaceSeparated(
          visibilityModifierSource,
          Source.emptyUnless(!enclosingTypeDescriptor.typeDeclaration.isInterface) {
            spaceSeparated(
              Source.emptyUnless(isNative) { KotlinSource.EXTERNAL_KEYWORD },
              inheritanceModifierSource,
            )
          },
          Source.emptyUnless(isKtOverride) { KotlinSource.OVERRIDE_KEYWORD },
        )

    val MemberDescriptor.visibilityModifierSource: Source
      get() = ktVisibility.takeUnless { it == inferredKtVisibility }?.source.orEmpty()

    val MethodDescriptor.inheritanceModifierSource
      get() =
        when {
          isAbstract -> KotlinSource.ABSTRACT_KEYWORD
          needsOpenModifier -> KotlinSource.OPEN_KEYWORD
          needsFinalModifier -> KotlinSource.FINAL_KEYWORD
          else -> Source.EMPTY
        }

    val FieldDescriptor.enumValueDeclarationNameSource: Source
      get() =
        name!!.let {
          if (Keywords.isForbiddenInEnumValueDeclaration(it)) {
            Source.source(it.inBackTicks)
          } else {
            identifierSource(it)
          }
        }
  }
}
