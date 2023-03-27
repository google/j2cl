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
import com.google.j2cl.transpiler.ast.NewInstance
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangEnum
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.backend.kotlin.ast.isForbiddenInEnumValueDeclaration
import com.google.j2cl.transpiler.backend.kotlin.ast.kotlinMembers
import com.google.j2cl.transpiler.backend.kotlin.objc.comment
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.block
import com.google.j2cl.transpiler.backend.kotlin.source.colonSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.commaAndNewLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.emptySource
import com.google.j2cl.transpiler.backend.kotlin.source.ifNotNullSource
import com.google.j2cl.transpiler.backend.kotlin.source.inRoundBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.join
import com.google.j2cl.transpiler.backend.kotlin.source.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.plusSemicolon
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.sourceIf
import com.google.j2cl.transpiler.backend.kotlin.source.spaceSeparated

fun Renderer.typeSource(type: Type): Source =
  type.declaration.let { typeDeclaration ->
    if (typeDeclaration.isKtNative) nativeTypeSource(typeDeclaration)
    else
      newLineSeparated(
        objCAnnotationSource(typeDeclaration),
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

fun Renderer.ktPrimaryConstructorParametersSource(type: Type): Source =
  type.ktPrimaryConstructor.ifNotNullSource { method ->
    methodParametersSource(method, method.toObjCNames()?.parameterNames)
  }

fun nativeTypeSource(type: TypeDeclaration): Source =
  comment(spaceSeparated(source("native"), source("class"), identifierSource(type.ktSimpleName)))

fun classModifiersSource(type: Type): Source =
  sourceIf(type.declaration.isKtInner) { source("inner") }

fun inheritanceModifierSource(typeDeclaration: TypeDeclaration): Source =
  sourceIf(typeDeclaration.isClass && !typeDeclaration.isFinal) {
    when {
      typeDeclaration.isAbstract -> source("abstract")
      typeDeclaration.isOpen -> source("open")
      else -> emptySource
    }
  }

fun kindModifiersSource(typeDeclaration: TypeDeclaration): Source =
  when (typeDeclaration.kind!!) {
    Kind.CLASS -> source("class")
    Kind.INTERFACE -> spaceSeparated(funModifierSource(typeDeclaration), source("interface"))
    Kind.ENUM -> spaceSeparated(source("enum"), source("class"))
  }

fun funModifierSource(typeDeclaration: TypeDeclaration): Source =
  sourceIf(typeDeclaration.isKtFunctionalInterface) { source("fun") }

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
  sourceIf(superTypeDescriptor.isClass) {
    if (!type.hasConstructors) inRoundBrackets(emptySource)
    else
      type.ktPrimaryConstructor.let { ktPrimaryConstructor ->
        sourceIf(ktPrimaryConstructor != null) {
          getConstructorInvocation(ktPrimaryConstructor).let {
            if (it == null) inRoundBrackets(emptySource) else invocationSource(it)
          }
        }
      }
  }

internal fun Renderer.typeBodySource(type: Type): Source =
  forTypeBody(type).run {
    block(
      emptyLineSeparated(
        sourceIf(type.isEnum) { enumValuesSource(type) },
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
  commaAndNewLineSeparated(type.enumFields.map(::enumValueSource)).plusSemicolon

private fun Renderer.enumValueSource(field: Field): Source =
  field.initializer
    .let { it as NewInstance }
    .let { newInstance ->
      newLineSeparated(
        objCAnnotationSource(field.descriptor),
        spaceSeparated(
          join(
            field.descriptor.enumValueDeclarationNameSource,
            newInstance.arguments
              .takeIf { it.isNotEmpty() }
              .ifNotNullSource { invocationSource(newInstance) }
          ),
          newInstance.anonymousInnerClass.ifNotNullSource(::typeBodySource)
        )
      )
    }

private val FieldDescriptor.enumValueDeclarationNameSource: Source
  get() =
    name!!.let {
      if (isForbiddenInEnumValueDeclaration(it)) source(it.inBackTicks) else identifierSource(it)
    }
