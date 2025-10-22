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
import com.google.j2cl.transpiler.ast.AstUtils.getConstructorInvocation
import com.google.j2cl.transpiler.ast.AstUtils.isAnnotatedWithDoNotAutobox
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.InitializerBlock
import com.google.j2cl.transpiler.ast.Member as JavaMember
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor
import com.google.j2cl.transpiler.ast.MethodLike
import com.google.j2cl.transpiler.ast.NewInstance
import com.google.j2cl.transpiler.ast.Type
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
import com.google.j2cl.transpiler.backend.kotlin.ast.CompanionObject
import com.google.j2cl.transpiler.backend.kotlin.ast.Member
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.block
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.colonSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inNewLine
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParentheses
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.indented
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

/**
 * Provides context for rendering Kotlin members.
 *
 * @property nameRenderer underlying name renderer
 * @property enclosingType enclosing type
 */
internal data class MemberRenderer(val nameRenderer: NameRenderer, val enclosingType: Type) {
  private val environment: Environment
    get() = nameRenderer.environment

  /** Returns renderer for enclosed types. */
  private val typeRenderer: TypeRenderer
    get() = TypeRenderer(nameRenderer)

  private val annotationRenderer: AnnotationRenderer
    get() = AnnotationRenderer(nameRenderer)

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

  internal fun memberSource(member: JavaMember): Source =
    when (member) {
      is Method -> methodSource(member).withMapping(member.descriptor)
      is Field -> fieldSource(member).withMapping(member.descriptor)
      is InitializerBlock -> initializerBlockSource(member)
      else -> throw InternalCompilerError("Unhandled ${member::class}")
    }

  private fun methodSource(method: Method): Source =
    spaceSeparated(
      methodHeaderSource(method),
      when {
        method.isAbstract -> Source.EMPTY
        method.isNative -> Source.EMPTY
        method.isConstructor && method.renderedStatements.isEmpty() -> Source.EMPTY
        method.descriptor.isKtProperty -> ktPropertyGetterSource(method)
        else -> bodySource(method)
      },
    )

  private fun ktPropertyGetterSource(method: Method): Source =
    indented(
      inNewLine(spaceSeparated(join(GET_KEYWORD, inParentheses(Source.EMPTY)), bodySource(method)))
    )

  private fun bodySource(method: Method): Source =
    block(statementRenderer.statementsSource(method.renderedStatements))

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
        !environment.ktVisibility(fieldDescriptor).isPrivate
    val initializer = field.initializer

    return newLineSeparated(
      Source.emptyUnless(isJvmField) { jvmFieldAnnotationSource() },
      objCNameRenderer.objCAnnotationSource(fieldDescriptor),
      jsInteropAnnotationRenderer.jsInteropAnnotationsSource(field),
      spaceSeparated(
        memberDescriptorRenderer.visibilityModifierSource(field.descriptor),
        Source.emptyUnless(isConst) { CONST_KEYWORD },
        Source.emptyUnless(field.isKtLateInit) { LATEINIT_KEYWORD },
        if (isFinal) VAL_KEYWORD else VAR_KEYWORD,
        colonSeparated(
          identifierSource(environment.ktMangledName(fieldDescriptor)),
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
    spaceSeparated(INIT_KEYWORD, statementRenderer.statementSource(initializerBlock.body))

  private fun methodHeaderSource(method: Method): Source =
    if (isKtPrimaryConstructor(method)) {
      INIT_KEYWORD
    } else {
      val methodDescriptor = method.descriptor
      newLineSeparated(
        Source.emptyUnless(methodDescriptor.isStatic) { jvmStaticAnnotationSource() },
        annotationsSource(method),
        spaceSeparated(
          methodModifiersSource(method),
          colonSeparated(
            join(
              memberDescriptorRenderer.methodKindAndNameSource(methodDescriptor),
              methodParametersSource(method),
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

  private fun methodModifiersSource(method: Method): Source =
    spaceSeparated(
      memberDescriptorRenderer.visibilityModifierSource(method.descriptor),
      Source.emptyIf(method.descriptor.enclosingTypeDescriptor.typeDeclaration.isInterface) {
        spaceSeparated(
          Source.emptyUnless(method.descriptor.isNative) { KotlinSource.EXTERNAL_KEYWORD },
          method.inheritanceModifierSource,
        )
      },
      Source.emptyUnless(method.isJavaOverride) { KotlinSource.OVERRIDE_KEYWORD },
    )

  fun annotationsSource(method: Method): Source =
    newLineSeparated(
      annotationRenderer.annotationsSource(method.descriptor),
      objCNameRenderer.objCAnnotationSource(method.descriptor),
      jsInteropAnnotationRenderer.jsInteropAnnotationsSource(method),
      memberDescriptorRenderer.jvmThrowsAnnotationSource(method.descriptor),
      memberDescriptorRenderer.nativeThrowsAnnotationSource(method.descriptor),
      suppressNothingToOverrideSource(method),
    )

  private fun suppressNothingToOverrideSource(method: Method): Source =
    Source.emptyUnless(method.hasSuppressNothingToOverrideAnnotation()) {
      return annotation(
        memberDescriptorRenderer.nameRenderer.topLevelQualifiedNameSource("kotlin.Suppress"),
        KotlinSource.literal("NOTHING_TO_OVERRIDE"),
      )
    }

  fun methodParametersSource(method: MethodLike): Source {
    val methodDescriptor = method.descriptor
    val parameterDescriptors = methodDescriptor.parameterDescriptors
    val parameters = method.parameters
    return Source.emptyIf(methodDescriptor.isKtProperty) {
      inParentheses(
        commaSeparated(
          0.until(parameters.size).map { index ->
            parameterSource(parameterDescriptors[index], parameters[index])
          }
        )
      )
    }
  }

  private fun parameterSource(
    parameterDescriptor: ParameterDescriptor,
    parameter: Variable,
  ): Source {
    val parameterTypeDescriptor = parameterDescriptor.typeDescriptor
    val renderedTypeDescriptor =
      if (!parameterDescriptor.isVarargs) {
        parameterTypeDescriptor
      } else {
        (parameterTypeDescriptor as ArrayTypeDescriptor).componentTypeDescriptor!!
      }
    return spaceSeparated(
      jsInteropAnnotationRenderer.jsInteropAnnotationsSource(parameterDescriptor),
      Source.emptyUnless(isAnnotatedWithDoNotAutobox(parameterDescriptor)) {
        annotation(
          nameRenderer.topLevelQualifiedNameSource("javaemul.internal.annotations.DoNotAutobox")
        )
      },
      Source.emptyUnless(parameterDescriptor.isVarargs) { VARARG_KEYWORD },
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
          jsInteropAnnotationRenderer.jsInteropAnnotationsSource(field),
          spaceSeparated(
            join(
              field.descriptor.enumValueDeclarationNameSource,
              newInstance.arguments
                .takeIf { it.isNotEmpty() }
                ?.let { expressionRenderer.invocationSource(newInstance) }
                .orEmpty(),
            ),
            newInstance.anonymousInnerClass
              ?.let { typeRenderer.typeBodySource(it, skipEmptyBlock = true) }
              .orEmpty(),
          ),
        )
      }

  /** Returns source with `val companion: Type.Companion`. */
  internal fun companionSupplierInterfaceMethodSource(type: Type): Source =
    spaceSeparated(
      VAL_KEYWORD,
      colonSeparated(
        source("companion"),
        dotSeparated(nameRenderer.qualifiedNameSource(type.typeDescriptor), source("Companion")),
      ),
    )
}
