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
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangAnnotation
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangEnum
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.INTERFACE_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotation
import com.google.j2cl.transpiler.backend.kotlin.objc.comment
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.block
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.colonSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inNewLine
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParentheses
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParenthesesIfNotEmpty
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.indented
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

/**
 * Provides context for rendering Kotlin types.
 *
 * @property nameRenderer underlying name renderer
 */
internal data class TypeRenderer(val nameRenderer: NameRenderer) {
  private val environment: Environment
    get() = nameRenderer.environment

  private fun memberRenderer(type: Type): MemberRenderer =
    MemberRenderer(nameRenderer.plusLocalNames(type), type)

  private fun expressionRenderer(type: Type): ExpressionRenderer =
    ExpressionRenderer(nameRenderer.plusLocalNames(type), type)

  private val jsInteropAnnotationRenderer: JsInteropAnnotationRenderer
    get() = JsInteropAnnotationRenderer(nameRenderer)

  private val objCNameRenderer: ObjCNameRenderer
    get() = ObjCNameRenderer(nameRenderer)

  /** Returns source for the given type. */
  fun typeSource(type: Type): Source =
    type.declaration
      .let { typeDeclaration ->
        if (typeDeclaration.isKtNative) {
          nativeTypeSource(typeDeclaration)
        } else {
          newLineSeparated(
            objCNameRenderer.objCAnnotationSource(typeDeclaration),
            jsInteropAnnotationRenderer.jsInteropAnnotationsSource(typeDeclaration),
            autoValueAnnotationsSource(typeDeclaration),
            spaceSeparated(
              inheritanceModifierSource(typeDeclaration),
              classModifiersSource(typeDeclaration),
              kindModifiersSource(typeDeclaration),
              colonSeparated(
                spaceSeparated(
                  typeDeclarationSource(typeDeclaration),
                  ktPrimaryConstructorSource(type),
                ),
                superTypesSource(type),
              ),
              nameRenderer.whereClauseSource(
                typeDeclaration.directlyDeclaredTypeParameterDescriptors
              ),
              typeBodySource(type, skipEmptyBlock = true),
            ),
          )
        }
      }
      .withMapping(type.sourcePosition)

  /** Returns source with body of the given type. */
  fun typeBodySource(type: Type, skipEmptyBlock: Boolean = false): Source =
    memberRenderer(type).run {
      block(
        emptyLineSeparated(
          Source.emptyUnless(type.isEnum) { memberRenderer(type).enumValuesSource(type) },
          emptyLineSeparated(type.ktMembers.map { memberRenderer(type).source(it) }),
        ),
        skipEmptyBlock = skipEmptyBlock,
      )
    }

  private fun ktPrimaryConstructorSource(type: Type): Source {
    val ktPrimaryConstructor = type.ktPrimaryConstructor
    return when {
      type.declaration.isAnnotation ->
        // Render non-static annotation methods as constructor parameters.
        memberRenderer(type).run {
          inParenthesesIfNotEmpty(
            indented(
              commaSeparated(
                type.methods.filter { !it.isStatic }.map { inNewLine(memberSource(it)) }
              )
            )
          )
        }
      ktPrimaryConstructor != null ->
        memberRenderer(type).run {
          spaceSeparated(
            annotationsSource(ktPrimaryConstructor),
            join(KotlinSource.CONSTRUCTOR_KEYWORD, methodParametersSource(ktPrimaryConstructor)),
          )
        }
      type.needExplicitPrimaryConstructor ->
        // Implicit constructors needs to follow the visiblity transpilation rules for members that
        // are different than the visibility transpilation rules for the class.
        spaceSeparated(
          type.declaration.visibility.defaultMemberKtVisibility.source,
          join(KotlinSource.CONSTRUCTOR_KEYWORD, inParentheses(Source.EMPTY)),
        )
      else -> Source.EMPTY
    }
  }

  private fun typeDeclarationSource(declaration: TypeDeclaration): Source =
    join(
      identifierSource(declaration.ktSimpleName),
      nameRenderer.typeParametersSource(declaration.directlyDeclaredTypeParameterDescriptors),
    )

  private fun superTypesSource(type: Type): Source =
    type.declaredSuperTypeDescriptors
      .asSequence()
      .filter { !isJavaLangObject(it) }
      .filter { !isJavaLangEnum(it) }
      .filter { !isJavaLangAnnotation(it) || !type.declaration.isAnnotation }
      .map { superTypeSource(type, it) }
      .toList()
      .let { commaSeparated(it) }

  private fun superTypeSource(type: Type, superTypeDescriptor: TypeDescriptor): Source =
    join(
      nameRenderer.typeDescriptorSource(superTypeDescriptor.toNonNullable(), asSuperType = true),
      superTypeInvocationSource(type, superTypeDescriptor),
    )

  private fun superTypeInvocationSource(type: Type, superTypeDescriptor: TypeDescriptor): Source =
    Source.emptyUnless(superTypeDescriptor.isClass) {
      if (!type.hasConstructors) {
        inParentheses(Source.EMPTY)
      } else {
        type.ktPrimaryConstructor?.let { constructorInvocationSource(type, it) }.orEmpty()
      }
    }

  private fun constructorInvocationSource(type: Type, method: Method): Source =
    getConstructorInvocation(method)?.let { expressionRenderer(type).invocationSource(it) }
      ?: inParentheses(Source.EMPTY)

  private fun autoValueAnnotationsSource(typeDeclaration: TypeDeclaration): Source =
    when {
      typeDeclaration.hasAnnotation("com.google.auto.value.AutoValue") ->
        annotation(
          nameRenderer.topLevelQualifiedNameSource("javaemul.lang.annotations.WasAutoValue")
        )
      typeDeclaration.hasAnnotation("com.google.auto.value.AutoValue.Builder") ->
        annotation(
          nameRenderer.topLevelQualifiedNameSource("javaemul.lang.annotations.WasAutoValue.Builder")
        )
      else -> Source.EMPTY
    }

  /**
   * Returns source with:
   * ```
   * interface TypeCompanionSupplier {
   *   val companion: Type.Companion
   * }
   * ```
   */
  internal fun companionSupplierInterfaceSource(type: Type): Source =
    Source.emptyUnless(type.needsCompanionSupplierInterface) {
      spaceSeparated(
        INTERFACE_KEYWORD,
        source(type.declaration.ktSimpleName + "CompanionSupplier"),
        block(memberRenderer(type).companionSupplierInterfaceMethodSource(type)),
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
          identifierSource(type.ktSimpleName),
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
          if (typeDeclaration.isAnnotation) {
            spaceSeparated(KotlinSource.ANNOTATION_KEYWORD, KotlinSource.CLASS_KEYWORD)
          } else {
            spaceSeparated(funModifierSource(typeDeclaration), KotlinSource.INTERFACE_KEYWORD)
          }
        TypeDeclaration.Kind.ENUM ->
          spaceSeparated(KotlinSource.ENUM_KEYWORD, KotlinSource.CLASS_KEYWORD)
      }

    fun funModifierSource(typeDeclaration: TypeDeclaration): Source =
      Source.emptyUnless(typeDeclaration.isKtFunctionalInterface) { KotlinSource.FUN_KEYWORD }
  }
}
