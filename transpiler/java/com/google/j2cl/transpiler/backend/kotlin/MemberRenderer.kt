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
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.AstUtils
import com.google.j2cl.transpiler.ast.AstUtils.getConstructorInvocation
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.InitializerBlock
import com.google.j2cl.transpiler.ast.Member as JavaMember
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor
import com.google.j2cl.transpiler.ast.MethodLike
import com.google.j2cl.transpiler.ast.NewInstance
import com.google.j2cl.transpiler.ast.ReturnStatement
import com.google.j2cl.transpiler.ast.Statement
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.COMPANION_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.CONST_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.GET_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.INIT_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.LATEINIT_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.OBJECT_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.SUPER_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.THIS_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.VAL_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.VARARG_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.VAR_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotation
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.initializer
import com.google.j2cl.transpiler.backend.kotlin.MemberDescriptorRenderer.Companion.enumValueDeclarationNameSource
import com.google.j2cl.transpiler.backend.kotlin.MemberDescriptorRenderer.Companion.methodModifiersSource
import com.google.j2cl.transpiler.backend.kotlin.MemberDescriptorRenderer.Companion.visibilityModifierSource
import com.google.j2cl.transpiler.backend.kotlin.ast.CompanionObject
import com.google.j2cl.transpiler.backend.kotlin.ast.Member
import com.google.j2cl.transpiler.backend.kotlin.ast.Visibility as KtVisibility
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.block
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.colonSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParentheses
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.indentedIf
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

/**
 * Provides context for rendering Kotlin members.
 *
 * @property nameRenderer underlying name renderer
 * @property enclosingType enclosing type
 */
internal data class MemberRenderer(val nameRenderer: NameRenderer, val enclosingType: Type) {
  /** Returns renderer for enclosed types. */
  private val typeRenderer: TypeRenderer
    get() = TypeRenderer(nameRenderer)

  private val memberDescriptorRenderer: MemberDescriptorRenderer
    get() = MemberDescriptorRenderer(nameRenderer)

  private val statementRenderer: StatementRenderer
    get() = StatementRenderer(nameRenderer, enclosingType)

  private val expressionRenderer: ExpressionRenderer
    get() = ExpressionRenderer(nameRenderer, enclosingType)

  private val jsInteropAnnotationRenderer: JsInteropAnnotationRenderer
    get() = JsInteropAnnotationRenderer(nameRenderer)

  private val objCNameRenderer: ObjCNameRenderer
    get() = ObjCNameRenderer(nameRenderer)

  fun source(member: Member): Source =
    when (member) {
      is Member.WithCompanionObject -> source(member.companionObject)
      is Member.WithJavaMember -> memberSource(member.javaMember)
      is Member.WithType -> typeRenderer.typeSource(member.type)
    }

  private fun source(companionObject: CompanionObject): Source =
    newLineSeparated(
      objCNameRenderer.objCAnnotationSource(companionObject),
      spaceSeparated(
        COMPANION_KEYWORD,
        OBJECT_KEYWORD,
        block(emptyLineSeparated(companionObject.members.map { source(it) })),
      ),
    )

  private fun memberSource(member: JavaMember): Source =
    when (member) {
      is Method -> methodSource(member).withMapping(member.descriptor)
      is Field -> fieldSource(member).withMapping(member.descriptor)
      is InitializerBlock -> initializerBlockSource(member)
      else -> throw InternalCompilerError("Unhandled ${member::class}")
    }

  private fun methodSource(method: Method): Source =
    method.renderedStatements.let { statements ->
      // Don't render primary constructor if it's empty.
      Source.emptyUnless(!isKtPrimaryConstructor(method) || !statements.isEmpty()) {
        spaceSeparated(
          methodHeaderSource(method),
          Source.emptyUnless(!method.isAbstract && !method.isNative) {
            // Constructors with no statements can be rendered without curly braces.
            Source.emptyUnless(!method.isConstructor || statements.isNotEmpty()) {
              spaceSeparated(
                Source.emptyUnless(method.descriptor.isKtProperty) {
                  join(GET_KEYWORD, inParentheses(Source.EMPTY))
                },
                block(statementRenderer.statementsSource(statements)),
              )
            }
          },
        )
      }
    }

  private fun isKtPrimaryConstructor(method: Method): Boolean =
    method == enclosingType.ktPrimaryConstructor

  private fun fieldSource(field: Field): Source {
    val fieldDescriptor = field.descriptor
    val isFinal = fieldDescriptor.isFinal
    val typeDescriptor = fieldDescriptor.typeDescriptor
    val isConst = field.isCompileTimeConstant && field.isStatic
    val isJvmField =
      !jvmFieldsAreIllegal &&
        !isConst &&
        !field.isKtLateInit &&
        fieldDescriptor.ktVisibility != KtVisibility.PRIVATE
    val initializer = field.initializer

    return newLineSeparated(
      Source.emptyUnless(isJvmField) { jvmFieldAnnotationSource() },
      objCNameRenderer.objCAnnotationSource(fieldDescriptor),
      jsInteropAnnotationRenderer.jsInteropAnnotationsSource(fieldDescriptor),
      spaceSeparated(
        field.descriptor.visibilityModifierSource,
        Source.emptyUnless(isConst) { CONST_KEYWORD },
        Source.emptyUnless(field.isKtLateInit) { LATEINIT_KEYWORD },
        if (isFinal) VAL_KEYWORD else VAR_KEYWORD,
        colonSeparated(
          identifierSource(fieldDescriptor.ktMangledName),
          nameRenderer.typeDescriptorSource(typeDescriptor),
        ),
        initializer(
          if (initializer == null && field.isNative) {
            nameRenderer.topLevelQualifiedNameSource("kotlin.js.definedExternally")
          } else {
            initializer?.let { expressionRenderer.expressionSource(it) }.orEmpty()
          }
        ),
      ),
    )
  }

  private val jvmFieldsAreIllegal: Boolean
    get() = enclosingType.jvmFieldsAreIllegal

  private fun jvmFieldAnnotationSource(): Source =
    annotation(nameRenderer.topLevelQualifiedNameSource("kotlin.jvm.JvmField"))

  private fun jvmStaticAnnotationSource(): Source =
    annotation(nameRenderer.topLevelQualifiedNameSource("kotlin.jvm.JvmStatic"))

  private fun initializerBlockSource(initializerBlock: InitializerBlock): Source =
    spaceSeparated(INIT_KEYWORD, statementRenderer.statementSource(initializerBlock.block))

  private fun methodHeaderSource(method: Method): Source =
    if (isKtPrimaryConstructor(method)) {
      INIT_KEYWORD
    } else {
      val methodDescriptor = method.descriptor
      val methodObjCNames = method.toObjCNames()
      newLineSeparated(
        Source.emptyUnless(methodDescriptor.isStatic) { jvmStaticAnnotationSource() },
        annotationsSource(methodDescriptor, methodObjCNames),
        spaceSeparated(
          methodDescriptor.methodModifiersSource,
          colonSeparated(
            join(
              memberDescriptorRenderer.methodKindAndNameSource(methodDescriptor),
              methodParametersSource(method, methodObjCNames?.parameterNames),
            ),
            if (methodDescriptor.isConstructor) {
              constructorInvocationSource(method)
            } else {
              memberDescriptorRenderer.methodReturnTypeSource(methodDescriptor)
            },
          ),
          nameRenderer.whereClauseSource(methodDescriptor.typeParameterTypeDescriptors),
        ),
      )
    }

  fun annotationsSource(methodDescriptor: MethodDescriptor, methodObjCNames: MethodObjCNames?) =
    newLineSeparated(
      objCNameRenderer.objCAnnotationSource(methodDescriptor, methodObjCNames),
      jsInteropAnnotationRenderer.jsInteropAnnotationsSource(methodDescriptor),
      memberDescriptorRenderer.jvmThrowsAnnotationSource(methodDescriptor),
      memberDescriptorRenderer.nativeThrowsAnnotationSource(methodDescriptor),
    )

  fun methodParametersSource(method: MethodLike, objCParameterNames: List<String>? = null): Source {
    val methodDescriptor = method.descriptor
    val parameterDescriptors = methodDescriptor.parameterDescriptors
    val parameters = method.parameters
    val renderWithNewLines = objCParameterNames != null && parameters.isNotEmpty()
    val optionalNewLineSource = Source.emptyUnless(renderWithNewLines) { Source.NEW_LINE }
    return Source.emptyUnless(!methodDescriptor.isKtProperty) {
      inParentheses(
        join(
          indentedIf(
            renderWithNewLines,
            commaSeparated(
              0.until(parameters.size).map { index ->
                join(
                  optionalNewLineSource,
                  parameterSource(
                    parameterDescriptors[index],
                    parameters[index],
                    objCParameterNames?.get(index),
                  ),
                )
              }
            ),
          ),
          optionalNewLineSource,
        )
      )
    }
  }

  private fun parameterSource(
    parameterDescriptor: ParameterDescriptor,
    parameter: Variable,
    objCParameterName: String? = null,
  ): Source {
    val parameterTypeDescriptor = parameterDescriptor.typeDescriptor
    val renderedTypeDescriptor =
      if (!parameterDescriptor.isVarargs) {
        parameterTypeDescriptor
      } else {
        (parameterTypeDescriptor as ArrayTypeDescriptor).componentTypeDescriptor!!
      }
    return spaceSeparated(
      Source.emptyUnless(parameterDescriptor.isVarargs) { VARARG_KEYWORD },
      objCParameterName?.let { objCNameRenderer.objCNameAnnotationSource(it) }.orEmpty(),
      jsInteropAnnotationRenderer.jsInteropAnnotationsSource(parameterDescriptor),
      colonSeparated(
        nameRenderer.nameSource(parameter),
        nameRenderer.typeDescriptorSource(renderedTypeDescriptor),
      ),
    )
  }

  private fun constructorInvocationSource(method: Method): Source =
    getConstructorInvocation(method)
      ?.let { constructorInvocation ->
        join(
          if (constructorInvocation.target.inSameTypeAs(method.descriptor)) {
            THIS_KEYWORD
          } else {
            SUPER_KEYWORD
          },
          expressionRenderer.invocationSource(constructorInvocation),
        )
      }
      .orEmpty()

  fun enumValuesSource(type: Type): Source =
    Source.commaAndNewLineSeparated(type.enumFields.map(::enumValueSource)).plus(Source.SEMICOLON)

  private fun enumValueSource(field: Field): Source =
    field.initializer
      .let { it as NewInstance }
      .let { newInstance ->
        newLineSeparated(
          objCNameRenderer.objCAnnotationSource(field.descriptor),
          jsInteropAnnotationRenderer.jsInteropAnnotationsSource(field.descriptor),
          spaceSeparated(
            join(
              field.descriptor.enumValueDeclarationNameSource,
              newInstance.arguments
                .takeIf { it.isNotEmpty() }
                ?.let { expressionRenderer.invocationSource(newInstance) }
                .orEmpty(),
            ),
            newInstance.anonymousInnerClass?.let { typeRenderer.typeBodySource(it) }.orEmpty(),
          ),
        )
      }

  companion object {
    val Method.renderedStatements: List<Statement>
      get() {
        if (!descriptor.isKtDisabled) {
          return body.statements.filter { !AstUtils.isConstructorInvocationStatement(it) }
        }

        if (TypeDescriptors.isPrimitiveVoid(descriptor.returnTypeDescriptor)) {
          return listOf()
        }

        return listOf(
          ReturnStatement.newBuilder()
            .setSourcePosition(sourcePosition)
            .setExpression(descriptor.returnTypeDescriptor.defaultValue)
            .build()
        )
      }
  }
}
