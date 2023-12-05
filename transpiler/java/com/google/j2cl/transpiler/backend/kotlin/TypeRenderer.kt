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
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangEnum
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.backend.kotlin.ast.Keywords
import com.google.j2cl.transpiler.backend.kotlin.common.inBackTicks
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
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

/**
 * Provides context for rendering Kotlin types.
 *
 * @property nameRenderer underlying name renderer
 * @property currentBodyType type of currently rendered type body, or null if not rendering a body
 * @property currentReturnLabelIdentifier optional label to render in return statements
 * @property renderThisReferenceWithLabel whether to render this reference with explicit qualifier
 */
internal data class TypeRenderer(
  val nameRenderer: NameRenderer,
  val currentBodyType: Type? = null,
  val currentReturnLabelIdentifier: String? = null,
  // TODO(b/252138814): Remove when KT-54349 is fixed
  val renderThisReferenceWithLabel: Boolean = false
) {
  /** Returns source for the given type. */
  fun typeSource(type: Type): Source =
    type.declaration.let { typeDeclaration ->
      if (typeDeclaration.isKtNative) {
        nativeTypeSource(typeDeclaration)
      } else {
        newLineSeparated(
          nameRenderer.objCAnnotationSource(typeDeclaration),
          jsInteropAnnotationsSource(typeDeclaration),
          spaceSeparated(
            inheritanceModifierSource(typeDeclaration),
            classModifiersSource(typeDeclaration),
            kindModifiersSource(typeDeclaration),
            colonSeparated(
              spaceSeparated(
                typeDeclarationSource(typeDeclaration),
                ktPrimaryConstructorSource(type)
              ),
              superTypesSource(type)
            ),
            whereClauseSource(typeDeclaration.typeParameterDescriptors),
            typeBodySource(type)
          )
        )
      }
    }

  /** Returns source with body of the given type. */
  fun typeBodySource(type: Type): Source =
    copy(
        nameRenderer = nameRenderer.run { copy(localNames = localNames + type.localNamesSet) },
        currentBodyType = type,
        renderThisReferenceWithLabel = false
      )
      .run {
        block(
          emptyLineSeparated(
            Source.emptyUnless(type.isEnum) { enumValuesSource(type) },
            emptyLineSeparated(type.ktMembers.map { source(it) })
          )
        )
      }

  private fun ktPrimaryConstructorSource(type: Type): Source {
    val ktPrimaryConstructor = type.ktPrimaryConstructor
    return when {
      ktPrimaryConstructor != null ->
        join(
          KotlinSource.CONSTRUCTOR_KEYWORD,
          methodParametersSource(
            ktPrimaryConstructor,
            ktPrimaryConstructor.toObjCNames()?.parameterNames
          )
        )
      type.needExplicitPrimaryConstructor ->
        // Implicit constructors needs to follow the visiblity transpilation rules for members that
        // are different than the visibility transpilation rules for the class.
        spaceSeparated(
          type.declaration.visibility.memberKtVisibility.source,
          join(KotlinSource.CONSTRUCTOR_KEYWORD, inParentheses(Source.EMPTY))
        )
      else -> Source.EMPTY
    }
  }

  private fun typeDeclarationSource(declaration: TypeDeclaration): Source =
    join(
      identifierSource(declaration.ktSimpleName),
      typeParametersSource(declaration.directlyDeclaredTypeParameterDescriptors)
    )

  private fun superTypesSource(type: Type): Source =
    type.declaredSuperTypeDescriptors
      .filter { !isJavaLangObject(it) && !isJavaLangEnum(it) }
      .let { superTypeDescriptors ->
        commaSeparated(superTypeDescriptors.map { superTypeSource(type, it) })
      }

  private fun superTypeSource(type: Type, superTypeDescriptor: TypeDescriptor): Source =
    join(
      nameRenderer.typeDescriptorSource(superTypeDescriptor.toNonNullable(), asSuperType = true),
      superTypeInvocationSource(type, superTypeDescriptor)
    )

  private fun superTypeInvocationSource(type: Type, superTypeDescriptor: TypeDescriptor): Source =
    Source.emptyUnless(superTypeDescriptor.isClass) {
      if (!type.hasConstructors) {
        inParentheses(Source.EMPTY)
      } else {
        type.ktPrimaryConstructor?.let { constructorInvocationSource(it) }.orEmpty()
      }
    }

  private fun constructorInvocationSource(method: Method): Source =
    getConstructorInvocation(method)?.let { invocationSource(it) } ?: inParentheses(Source.EMPTY)

  private fun enumValuesSource(type: Type): Source =
    commaAndNewLineSeparated(type.enumFields.map(::enumValueSource)).plus(Source.SEMICOLON)

  private fun enumValueSource(field: Field): Source =
    field.initializer
      .let { it as NewInstance }
      .let { newInstance ->
        newLineSeparated(
          nameRenderer.objCAnnotationSource(field.descriptor),
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

  private companion object {
    fun classModifiersSource(typeDeclaration: TypeDeclaration): Source =
      Source.emptyUnless(typeDeclaration.isKtInner) { KotlinSource.INNER_KEYWORD }

    fun nativeTypeSource(type: TypeDeclaration): Source =
      comment(
        spaceSeparated(
          KotlinSource.NATIVE_KEYWORD,
          KotlinSource.CLASS_KEYWORD,
          identifierSource(type.ktSimpleName)
        )
      )

    fun inheritanceModifierSource(typeDeclaration: TypeDeclaration): Source =
      Source.emptyUnless(typeDeclaration.isClass && !typeDeclaration.isFinal) {
        when {
          typeDeclaration.isAbstract -> KotlinSource.ABSTRACT_KEYWORD
          typeDeclaration.isOpen -> KotlinSource.OPEN_KEYWORD
          else -> Source.EMPTY
        }
      }

    fun kindModifiersSource(typeDeclaration: TypeDeclaration): Source =
      when (typeDeclaration.kind!!) {
        TypeDeclaration.Kind.CLASS -> KotlinSource.CLASS_KEYWORD
        TypeDeclaration.Kind.INTERFACE ->
          spaceSeparated(funModifierSource(typeDeclaration), KotlinSource.INTERFACE_KEYWORD)
        TypeDeclaration.Kind.ENUM ->
          spaceSeparated(KotlinSource.ENUM_KEYWORD, KotlinSource.CLASS_KEYWORD)
      }

    fun funModifierSource(typeDeclaration: TypeDeclaration): Source =
      Source.emptyUnless(typeDeclaration.isKtFunctionalInterface) { KotlinSource.FUN_KEYWORD }

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
