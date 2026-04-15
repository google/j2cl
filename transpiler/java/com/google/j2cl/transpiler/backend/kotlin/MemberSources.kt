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
import com.google.j2cl.transpiler.ast.AstUtils.getConstructorInvocationStatement
import com.google.j2cl.transpiler.ast.AstUtils.isAnnotatedWithDoNotAutobox
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.InitializerBlock
import com.google.j2cl.transpiler.ast.Member as JavaMember
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor
import com.google.j2cl.transpiler.ast.MethodLike
import com.google.j2cl.transpiler.ast.NewInstance
import com.google.j2cl.transpiler.ast.Type
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
import com.google.j2cl.transpiler.backend.kotlin.MemberDescriptorSources.Companion.enumValueDeclarationNameSource
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
 * Sources for members.
 *
 * @property nameSources underlying name sources
 * @property enclosingType enclosing type
 */
internal data class MemberSources(val nameSources: NameSources, val enclosingType: Type) {
  private val environment: Environment
    get() = nameSources.environment

  /** Returns sources for enclosed types. */
  private val typeSources: TypeSources
    get() = TypeSources(nameSources)

  private val annotationSources: AnnotationSources
    get() = AnnotationSources(nameSources)

  private val memberDescriptorSources: MemberDescriptorSources
    get() = MemberDescriptorSources(nameSources)

  private val statementSources: StatementSources
    get() = StatementSources(nameSources, enclosingType)

  private val expressionSources: ExpressionSources
    get() = ExpressionSources(nameSources, enclosingType)

  private val jsInteropAnnotationSources: JsInteropAnnotationSources
    get() = JsInteropAnnotationSources(nameSources)

  private val objCNameSources: ObjCNameSources
    get() = ObjCNameSources(nameSources)

  fun source(member: Member): Source =
    when (member) {
      is Member.WithCompanionObject -> source(member.companionObject)
      is Member.WithJavaMember -> memberSource(member.javaMember)
      is Member.WithType -> typeSources.typeSource(member.type)
    }

  private fun source(companionObject: CompanionObject): Source =
    newLineSeparated(
      spaceSeparated(
        COMPANION_KEYWORD,
        OBJECT_KEYWORD,
        block(emptyLineSeparated(companionObject.members.map { source(it) })),
      )
    )

  internal fun memberSource(member: JavaMember): Source =
    when (member) {
      is Method -> methodSource(member).withMapping(member.descriptor)
      is Field -> fieldSource(member).withMapping(member.descriptor)
      is InitializerBlock -> initializerBlockSource(member)
      else -> throw InternalCompilerError("Unhandled ${member::class}")
    }

  private fun methodSource(method: Method): Source =
    join(
      methodHeaderSource(method),
      when {
        method.isAbstract -> Source.EMPTY
        method.isNative -> Source.EMPTY
        method.isConstructor && method.includedStatements.isEmpty() -> Source.EMPTY
        method.descriptor.isKtProperty -> indented(inNewLine(ktPropertyGetterSource(method)))
        else -> Source.SPACE.plus(bodySource(method))
      },
    )

  private fun ktPropertyGetterSource(method: Method): Source =
    spaceSeparated(join(GET_KEYWORD, inParentheses(Source.EMPTY)), bodySource(method))

  private fun bodySource(method: Method): Source =
    block(statementSources.statementsSource(method.includedStatements))

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
        memberDescriptorSources.volatileAnnotationSource(fieldDescriptor),
        objCNameSources.objCAnnotationSource(fieldDescriptor),
        jsInteropAnnotationSources.jsInteropAnnotationsSource(field),
        spaceSeparated(
          memberDescriptorSources.visibilityModifierSource(field.descriptor),
          Source.emptyUnless(isConst) { CONST_KEYWORD },
          Source.emptyUnless(field.isKtLateInit) { LATEINIT_KEYWORD },
          if (isFinal) VAL_KEYWORD else VAR_KEYWORD,
          colonSeparated(fieldNameSource(field), nameSources.typeDescriptorSource(typeDescriptor)),
          initializer(
            if (initializer == null && field.isNative) {
              nameSources.topLevelQualifiedNameSource("kotlin.js.definedExternally")
            } else {
              initializer?.let { expressionSources.expressionSource(it) }.orEmpty()
            }
          ),
        ),
      )
      .withMapping(field.sourcePosition)
  }

  private val jvmFieldsAreIllegal: Boolean
    get() = enclosingType.jvmFieldsAreIllegal

  private fun jvmFieldAnnotationSource(): Source =
    annotation(nameSources.topLevelQualifiedNameSource("kotlin.jvm.JvmField"))

  private fun jvmStaticAnnotationSource(): Source =
    annotation(nameSources.topLevelQualifiedNameSource("kotlin.jvm.JvmStatic"))

  private fun initializerBlockSource(initializerBlock: InitializerBlock): Source =
    spaceSeparated(INIT_KEYWORD, statementSources.statementSource(initializerBlock.body))

  private fun methodHeaderSource(method: Method): Source =
    if (isKtPrimaryConstructor(method)) {
      INIT_KEYWORD.withMapping(method.sourcePosition)
    } else {
      val methodDescriptor = method.descriptor
      newLineSeparated(
        Source.emptyUnless(methodDescriptor.isStatic) { jvmStaticAnnotationSource() },
        annotationsSource(method),
        spaceSeparated(
          methodModifiersSource(method),
          colonSeparated(
            join(methodKindAndNameSource(method), methodParametersSource(method)),
            if (methodDescriptor.isConstructor) {
              constructorInvocationSource(method)
            } else {
              memberDescriptorSources.methodReturnTypeSource(methodDescriptor)
            },
          ),
          nameSources.whereClauseSource(methodDescriptor.typeParameterTypeDescriptors),
        ),
      )
    }

  private fun methodModifiersSource(method: Method): Source =
    spaceSeparated(
      memberDescriptorSources.visibilityModifierSource(method.descriptor),
      Source.emptyIf(method.descriptor.enclosingTypeDescriptor.typeDeclaration.isInterface) {
        spaceSeparated(
          Source.emptyUnless(method.descriptor.isNative) { KotlinSource.EXTERNAL_KEYWORD },
          method.inheritanceModifierSource,
        )
      },
      Source.emptyUnless(method.isJavaOverride) { KotlinSource.OVERRIDE_KEYWORD },
    )

  private fun methodKindAndNameSource(method: Method): Source =
    if (method.descriptor.isConstructor) {
      KotlinSource.CONSTRUCTOR_KEYWORD.withMapping(method.sourcePosition)
    } else {
      spaceSeparated(
        if (method.descriptor.isKtProperty) KotlinSource.VAL_KEYWORD else KotlinSource.FUN_KEYWORD,
        nameSources.typeParametersSource(method.descriptor.typeParameterTypeDescriptors),
        methodNameSource(method),
      )
    }

  private fun methodNameSource(method: Method): Source =
    memberDescriptorSources.nameSource(method.descriptor).withMapping(method.sourcePosition)

  private fun fieldNameSource(field: Field): Source =
    memberDescriptorSources.nameSource(field.descriptor).withMapping(field.sourcePosition)

  fun annotationsSource(method: Method): Source =
    newLineSeparated(
      annotationSources.annotationsSource(
        method.descriptor,
        isProperty = method.descriptor.isKtProperty,
      ),
      objCNameSources.objCAnnotationSource(method.descriptor),
      jsInteropAnnotationSources.jsInteropAnnotationsSource(method),
      memberDescriptorSources.jvmThrowsAnnotationSource(method.descriptor),
      memberDescriptorSources.nativeThrowsAnnotationSource(method.descriptor),
      suppressNothingToOverrideSource(method),
    )

  private fun suppressNothingToOverrideSource(method: Method): Source =
    Source.emptyUnless(method.hasSuppressNothingToOverrideAnnotation()) {
      return annotation(
        memberDescriptorSources.nameSources.topLevelQualifiedNameSource("kotlin.Suppress"),
        KotlinSource.literal("NOTHING_TO_OVERRIDE"),
      )
    }

  fun methodParametersSource(method: MethodLike): Source {
    val methodDescriptor = method.descriptor
    val parameterDescriptors = methodDescriptor.parameterDescriptors
    val parameters = method.parameters
    val nameSources = parameters.map { nameSources.variableNameSource(it) }
    return Source.emptyIf(methodDescriptor.isKtProperty) {
      inParentheses(
        commaSeparated(
          0.until(parameters.size).map { index ->
            parameterSource(parameterDescriptors[index], nameSources[index])
          }
        )
      )
    }
  }

  private fun parameterSource(
    parameterDescriptor: ParameterDescriptor,
    nameSource: Source,
  ): Source {
    val parameterTypeDescriptor = parameterDescriptor.typeDescriptor
    val typeDescriptor =
      if (!parameterDescriptor.isVarargs) {
        parameterTypeDescriptor
      } else {
        (parameterTypeDescriptor as ArrayTypeDescriptor).componentTypeDescriptor!!
      }
    return spaceSeparated(
      jsInteropAnnotationSources.jsInteropAnnotationsSource(parameterDescriptor),
      Source.emptyUnless(isAnnotatedWithDoNotAutobox(parameterDescriptor)) {
        annotation(
          nameSources.topLevelQualifiedNameSource("javaemul.internal.annotations.DoNotAutobox")
        )
      },
      Source.emptyUnless(parameterDescriptor.isVarargs) { VARARG_KEYWORD },
      colonSeparated(nameSource, nameSources.typeDescriptorSource(typeDescriptor)),
    )
  }

  private fun constructorInvocationSource(method: Method): Source =
    getConstructorInvocationStatement(method)
      ?.let { statement ->
        val invocation = getConstructorInvocation(statement)
        join(
            if (invocation.target.inSameTypeAs(method.descriptor)) {
              THIS_KEYWORD
            } else {
              SUPER_KEYWORD
            },
            expressionSources.invocationSource(invocation),
          )
          .withMapping(statement.sourcePosition)
      }
      .orEmpty()

  fun enumValuesSource(type: Type): Source =
    Source.commaAndNewLineSeparated(type.enumFields.map(::enumValueSource)).plus(Source.SEMICOLON)

  private fun enumValueSource(field: Field): Source =
    field.initializer
      .let { it as NewInstance }
      .let { newInstance ->
        newLineSeparated(
          objCNameSources.objCAnnotationSource(field.descriptor),
          jsInteropAnnotationSources.jsInteropAnnotationsSource(field),
          spaceSeparated(
            join(
              field.descriptor.enumValueDeclarationNameSource.withMapping(field.sourcePosition),
              newInstance.arguments
                .takeIf { it.isNotEmpty() }
                ?.let { expressionSources.invocationSource(newInstance) }
                .orEmpty(),
            ),
            newInstance.anonymousInnerClass
              ?.let { typeSources.typeBodySource(it, skipEmptyBlock = true) }
              .orEmpty(),
          ),
        )
      }
      .withMapping(field.descriptor)

  /** Returns source with `val companion: Type.Companion`. */
  internal fun companionSupplierInterfaceMethodSource(type: Type): Source =
    spaceSeparated(
      VAL_KEYWORD,
      colonSeparated(
        source("companion"),
        dotSeparated(nameSources.qualifiedNameSource(type.typeDescriptor), source("Companion")),
      ),
    )
}
