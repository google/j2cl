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

import com.google.j2cl.transpiler.ast.AstUtils.getConstructorInvocation
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.NewInstance
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangEnum
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.ABSTRACT_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.CLASS_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.ENUM_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.FUN_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.INNER_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.INTERFACE_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.NATIVE_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.OPEN_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.ast.isForbiddenInEnumValueDeclaration
import com.google.j2cl.transpiler.backend.kotlin.ast.kotlinMembers
import com.google.j2cl.transpiler.backend.kotlin.objc.comment
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.block
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.colonSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaAndNewLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParentheses
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

fun Renderer.typeSource(type: Type): Source =
  type.declaration.let { typeDeclaration ->
    if (typeDeclaration.isKtNative) {
      nativeTypeSource(typeDeclaration)
    } else {
      newLineSeparated(
        objCAnnotationSource(typeDeclaration),
        jsInteropAnnotationsSource(typeDeclaration),
        spaceSeparated(
          inheritanceModifierSource(typeDeclaration),
          classModifiersSource(type),
          kindModifiersSource(typeDeclaration),
          colonSeparated(
            join(
              typeDeclarationSource(typeDeclaration),
              ktPrimaryConstructorParametersSource(type)
            ),
            superTypesSource(type)
          ),
          whereClauseSource(typeDeclaration.typeParameterDescriptors),
          typeBodySource(type)
        )
      )
    }
  }

fun Renderer.ktPrimaryConstructorParametersSource(type: Type): Source =
  type.ktPrimaryConstructor
    ?.let { method -> methodParametersSource(method, method.toObjCNames()?.parameterNames) }
    .orEmpty()

fun nativeTypeSource(type: TypeDeclaration): Source =
  comment(spaceSeparated(NATIVE_KEYWORD, CLASS_KEYWORD, identifierSource(type.ktSimpleName)))

fun classModifiersSource(type: Type): Source =
  Source.emptyUnless(type.declaration.isKtInner) { INNER_KEYWORD }

fun inheritanceModifierSource(typeDeclaration: TypeDeclaration): Source =
  Source.emptyUnless(typeDeclaration.isClass && !typeDeclaration.isFinal) {
    when {
      typeDeclaration.isAbstract -> ABSTRACT_KEYWORD
      typeDeclaration.isOpen -> OPEN_KEYWORD
      else -> Source.EMPTY
    }
  }

fun kindModifiersSource(typeDeclaration: TypeDeclaration): Source =
  when (typeDeclaration.kind!!) {
    Kind.CLASS -> CLASS_KEYWORD
    Kind.INTERFACE -> spaceSeparated(funModifierSource(typeDeclaration), INTERFACE_KEYWORD)
    Kind.ENUM -> spaceSeparated(ENUM_KEYWORD, CLASS_KEYWORD)
  }

fun funModifierSource(typeDeclaration: TypeDeclaration): Source =
  Source.emptyUnless(typeDeclaration.isKtFunctionalInterface) { FUN_KEYWORD }

fun Renderer.typeDeclarationSource(declaration: TypeDeclaration): Source =
  join(
    identifierSource(declaration.ktSimpleName),
    typeParametersSource(declaration.directlyDeclaredTypeParameterDescriptors)
  )

private fun Renderer.superTypesSource(type: Type): Source =
  type.declaredSuperTypeDescriptors
    .filter { !isJavaLangObject(it) && !isJavaLangEnum(it) }
    .let { superTypeDescriptors ->
      commaSeparated(superTypeDescriptors.map { superTypeSource(type, it) })
    }

private fun Renderer.superTypeSource(type: Type, superTypeDescriptor: TypeDescriptor): Source =
  join(
    typeDescriptorSource(superTypeDescriptor.toNonNullable(), asSuperType = true),
    superTypeInvocationSource(type, superTypeDescriptor)
  )

private fun Renderer.superTypeInvocationSource(
  type: Type,
  superTypeDescriptor: TypeDescriptor
): Source =
  Source.emptyUnless(superTypeDescriptor.isClass) {
    if (!type.hasConstructors) {
      inParentheses(Source.EMPTY)
    } else {
      type.ktPrimaryConstructor?.let { constructorInvocationSource(it) }.orEmpty()
    }
  }

private fun Renderer.constructorInvocationSource(method: Method): Source =
  getConstructorInvocation(method)?.let { invocationSource(it) } ?: inParentheses(Source.EMPTY)

internal fun Renderer.typeBodySource(type: Type): Source =
  forTypeBody(type).run {
    block(
      emptyLineSeparated(
        Source.emptyUnless(type.isEnum) { enumValuesSource(type) },
        emptyLineSeparated(type.kotlinMembers.map { source(it) })
      )
    )
  }

internal fun Renderer.forTypeBody(type: Type): Renderer =
  copy(
    currentType = type,
    renderThisReferenceWithLabel = false,
    localNames = localNames + type.localNames
  )

private fun Renderer.enumValuesSource(type: Type): Source =
  commaAndNewLineSeparated(type.enumFields.map(::enumValueSource)).plus(Source.SEMICOLON)

private fun Renderer.enumValueSource(field: Field): Source =
  field.initializer
    .let { it as NewInstance }
    .let { newInstance ->
      newLineSeparated(
        objCAnnotationSource(field.descriptor),
        jsInteropAnnotationsSource(field.descriptor),
        spaceSeparated(
          join(
            field.descriptor.enumValueDeclarationNameSource,
            newInstance.arguments
              .takeIf { it.isNotEmpty() }
              ?.let { invocationSource(newInstance) }
              .orEmpty()
          ),
          newInstance.anonymousInnerClass?.let(::typeBodySource).orEmpty()
        )
      )
    }

private val FieldDescriptor.enumValueDeclarationNameSource: Source
  get() =
    name!!.let {
      if (isForbiddenInEnumValueDeclaration(it)) {
        source(it.inBackTicks)
      } else {
        identifierSource(it)
      }
    }
