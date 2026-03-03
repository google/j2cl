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
import com.google.j2cl.transpiler.backend.kotlin.ast.Keywords
import com.google.j2cl.transpiler.backend.kotlin.common.inBackTicks
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty
import java.lang.Boolean.getBoolean

/**
 * Sources for member descriptors.
 *
 * @param nameSources underlying name sources
 */
internal data class MemberDescriptorSources(val nameSources: NameSources) {
  private val environment: Environment
    get() = nameSources.environment

  private val annotationSources: AnnotationSources
    get() = AnnotationSources(nameSources)

  fun nameSource(memberDescriptor: MemberDescriptor): Source =
    identifierSource(environment.ktMangledName(memberDescriptor))

  fun methodReturnTypeSource(methodDescriptor: MethodDescriptor): Source =
    methodDescriptor.returnTypeDescriptor
      .takeIf { it != PrimitiveTypes.VOID }
      ?.let { nameSources.typeDescriptorSource(it) }
      .orEmpty()

  fun jvmThrowsAnnotationSource(methodDescriptor: MethodDescriptor): Source =
    methodDescriptor.thrownTypeDescriptors
      .takeIf { it.isNotEmpty() }
      ?.let { thrownTypeDescriptors ->
        KotlinSource.annotation(
          nameSources.topLevelQualifiedNameSource("kotlin.jvm.Throws"),
          thrownTypeDescriptors.map {
            KotlinSource.classLiteral(
              nameSources.typeDescriptorSource(it.toRawTypeDescriptor().toNonNullable())
            )
          },
        )
      }
      .orEmpty()

  fun nativeThrowsAnnotationSource(methodDescriptor: MethodDescriptor): Source =
    methodDescriptor.ktInfo
      .takeIf { it.isThrows && SHOULD_INCLUDE_NATIVE_THROWS }
      ?.let {
        KotlinSource.annotation(
          nameSources.topLevelQualifiedNameSource("javaemul.lang.NativeThrows"),
          KotlinSource.classLiteral(
            nameSources.typeDescriptorSource(
              TypeDescriptors.get().javaLangThrowable.toNonNullable()
            )
          ),
        )
      }
      .orEmpty()

  fun volatileAnnotationSource(fieldDescriptor: FieldDescriptor): Source =
    fieldDescriptor
      .takeIf { it.isVolatile }
      ?.let { annotationSources.volatileAnnotationSource() }
      .orEmpty()

  fun visibilityModifierSource(memberDescriptor: MemberDescriptor): Source =
    environment
      .ktVisibility(memberDescriptor)
      .takeUnless { it == environment.inferredKtVisibility(memberDescriptor) }
      ?.source
      .orEmpty()

  companion object {
    // TODO(b/316324154): Remove when no longer necessary
    val SHOULD_INCLUDE_NATIVE_THROWS: Boolean =
      !getBoolean("com.google.j2cl.transpiler.backend.kotlin.isNativeThrowsDisabled")

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
