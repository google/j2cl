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
import com.google.j2cl.transpiler.ast.AstUtils.isConstructorInvocationStatement
import com.google.j2cl.transpiler.ast.AstUtils.needsVisibilityBridge
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.FunctionExpression
import com.google.j2cl.transpiler.ast.InitializerBlock
import com.google.j2cl.transpiler.ast.Member as JavaMember
import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor
import com.google.j2cl.transpiler.ast.MethodLike
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.ReturnStatement
import com.google.j2cl.transpiler.ast.Statement
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.ABSTRACT_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.COMPANION_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.CONSTRUCTOR_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.CONST_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.EXTERNAL_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.FUN_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.GET_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.INIT_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.LATEINIT_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.OBJECT_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.OPEN_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.OVERRIDE_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.SUPER_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.THIS_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.VAL_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.VARARG_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.VAR_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotation
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.classLiteral
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.initializer
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

internal fun Renderer.source(member: Member): Source =
  when (member) {
    is Member.WithCompanionObject -> source(member.companionObject)
    is Member.WithJavaMember -> memberSource(member.javaMember)
    is Member.WithType -> typeSource(member.type)
  }

private fun Renderer.source(companionObject: CompanionObject): Source =
  newLineSeparated(
    objCAnnotationSource(companionObject),
    spaceSeparated(
      COMPANION_KEYWORD,
      OBJECT_KEYWORD,
      block(emptyLineSeparated(companionObject.members.map { source(it) }))
    )
  )

private fun Renderer.memberSource(member: JavaMember): Source =
  when (member) {
    is Method -> methodSource(member)
    is Field -> fieldSource(member)
    is InitializerBlock -> initializerBlockSource(member)
    else -> throw InternalCompilerError("Unhandled ${member::class}")
  }

private fun Renderer.methodSource(method: Method): Source =
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
              copy(currentReturnLabelIdentifier = null).run { block(statementsSource(statements)) }
            )
          }
        }
      )
    }
  }

private val Method.renderedStatements: List<Statement>
  get() {
    if (!descriptor.isKtDisabled) {
      return body.statements.filter { !isConstructorInvocationStatement(it) }
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

private fun Renderer.isKtPrimaryConstructor(method: Method): Boolean =
  method == currentType!!.ktPrimaryConstructor

private fun Renderer.fieldSource(field: Field): Source {
  val fieldDescriptor = field.descriptor
  val isFinal = fieldDescriptor.isFinal
  val typeDescriptor = fieldDescriptor.typeDescriptor
  val isConst = field.isCompileTimeConstant && field.isStatic
  val isJvmField =
    !currentType!!.jvmFieldsAreIllegal &&
      !isConst &&
      !field.isKtLateInit &&
      fieldDescriptor.ktVisibility != KtVisibility.PRIVATE
  val initializer = field.initializer

  return newLineSeparated(
    Source.emptyUnless(isJvmField) { jvmFieldAnnotationSource() },
    objCAnnotationSource(fieldDescriptor),
    jsInteropAnnotationsSource(fieldDescriptor),
    spaceSeparated(
      visibilityModifierSource(field.descriptor),
      Source.emptyUnless(isConst) { CONST_KEYWORD },
      Source.emptyUnless(field.isKtLateInit) { LATEINIT_KEYWORD },
      if (isFinal) VAL_KEYWORD else VAR_KEYWORD,
      colonSeparated(
        identifierSource(fieldDescriptor.ktMangledName),
        typeDescriptorSource(typeDescriptor)
      ),
      initializer(
        if (initializer == null && field.isNative) {
          topLevelQualifiedNameSource("kotlin.js.definedExternally")
        } else {
          initializer?.let(::expressionSource).orEmpty()
        }
      )
    )
  )
}

private fun Renderer.jvmFieldAnnotationSource(): Source =
  annotation(topLevelQualifiedNameSource("kotlin.jvm.JvmField"))

private fun Renderer.jvmStaticAnnotationSource(): Source =
  annotation(topLevelQualifiedNameSource("kotlin.jvm.JvmStatic"))

private fun Renderer.jvmThrowsAnnotationSource(methodDescriptor: MethodDescriptor): Source =
  methodDescriptor.exceptionTypeDescriptors
    .takeIf { it.isNotEmpty() }
    ?.let { exceptionTypeDescriptors ->
      annotation(
        topLevelQualifiedNameSource("kotlin.jvm.Throws"),
        exceptionTypeDescriptors.map {
          classLiteral(typeDescriptorSource(it.toRawTypeDescriptor().toNonNullable()))
        }
      )
    }
    .orEmpty()

private fun Renderer.nativeThrowsAnnotationSource(methodDescriptor: MethodDescriptor): Source =
  methodDescriptor.ktInfo
    .takeIf { it.isThrows }
    ?.let {
      annotation(
        topLevelQualifiedNameSource("javaemul.lang.NativeThrows"),
        classLiteral(typeDescriptorSource(TypeDescriptors.get().javaLangThrowable.toNonNullable()))
      )
    }
    .orEmpty()

private fun Renderer.initializerBlockSource(initializerBlock: InitializerBlock): Source =
  spaceSeparated(INIT_KEYWORD, statementSource(initializerBlock.block))

private fun Renderer.methodHeaderSource(method: Method): Source =
  if (isKtPrimaryConstructor(method)) {
    INIT_KEYWORD
  } else {
    val methodDescriptor = method.descriptor
    val methodObjCNames = method.toObjCNames()
    newLineSeparated(
      Source.emptyUnless(methodDescriptor.isStatic) { jvmStaticAnnotationSource() },
      objCAnnotationSource(methodDescriptor, methodObjCNames),
      jsInteropAnnotationsSource(methodDescriptor),
      jvmThrowsAnnotationSource(methodDescriptor),
      nativeThrowsAnnotationSource(methodDescriptor),
      spaceSeparated(
        methodModifiersSource(methodDescriptor),
        colonSeparated(
          join(
            methodKindAndNameSource(methodDescriptor),
            methodParametersSource(method, methodObjCNames?.parameterNames)
          ),
          if (methodDescriptor.isConstructor) {
            constructorInvocationSource(method)
          } else {
            methodReturnTypeSource(methodDescriptor)
          }
        ),
        whereClauseSource(methodDescriptor.typeParameterTypeDescriptors)
      )
    )
  }

internal fun Renderer.methodHeaderSource(functionExpression: FunctionExpression): Source =
  functionExpression.descriptor.let { methodDescriptor ->
    newLineSeparated(
      spaceSeparated(
        OVERRIDE_KEYWORD,
        colonSeparated(
          join(
            methodKindAndNameSource(methodDescriptor),
            methodParametersSource(functionExpression)
          ),
          methodReturnTypeSource(methodDescriptor)
        ),
        whereClauseSource(methodDescriptor.typeParameterTypeDescriptors)
      )
    )
  }

private fun Renderer.methodKindAndNameSource(methodDescriptor: MethodDescriptor): Source =
  if (methodDescriptor.isConstructor) {
    CONSTRUCTOR_KEYWORD
  } else {
    spaceSeparated(
      if (methodDescriptor.isKtProperty) VAL_KEYWORD else FUN_KEYWORD,
      typeParametersSource(methodDescriptor.typeParameterTypeDescriptors),
      identifierSource(methodDescriptor.ktMangledName)
    )
  }

private fun methodModifiersSource(methodDescriptor: MethodDescriptor): Source =
  spaceSeparated(
    visibilityModifierSource(methodDescriptor),
    Source.emptyUnless(!methodDescriptor.enclosingTypeDescriptor.typeDeclaration.isInterface) {
      spaceSeparated(
        Source.emptyUnless(methodDescriptor.isNative) { EXTERNAL_KEYWORD },
        methodDescriptor.inheritanceModifierSource
      )
    },
    Source.emptyUnless(methodDescriptor.isKtOverride) { OVERRIDE_KEYWORD }
  )

private fun visibilityModifierSource(memberDescriptor: MemberDescriptor): Source =
  memberDescriptor.ktVisibility
    .takeUnless { it == memberDescriptor.inferredKtVisibility }
    ?.source
    .orEmpty()

private val MethodDescriptor.inheritanceModifierSource
  get() =
    when {
      isAbstract -> ABSTRACT_KEYWORD
      isOpen -> OPEN_KEYWORD
      else -> Source.EMPTY
    }

internal fun Renderer.methodParametersSource(
  method: MethodLike,
  objCParameterNames: List<String>? = null
): Source {
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
                  objCParameterNames?.get(index)
                )
              )
            }
          )
        ),
        optionalNewLineSource
      )
    )
  }
}

private fun Renderer.parameterSource(
  parameterDescriptor: ParameterDescriptor,
  parameter: Variable,
  objCParameterName: String? = null
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
    objCParameterName?.let { objCNameAnnotationSource(it) }.orEmpty(),
    jsInteropAnnotationsSource(parameterDescriptor),
    colonSeparated(nameSource(parameter), typeDescriptorSource(renderedTypeDescriptor))
  )
}

internal fun Renderer.methodReturnTypeSource(methodDescriptor: MethodDescriptor): Source =
  methodDescriptor.returnTypeDescriptor
    .takeIf { it != PrimitiveTypes.VOID }
    ?.let { typeDescriptorSource(it) }
    .orEmpty()

private fun Renderer.constructorInvocationSource(method: Method): Source =
  getConstructorInvocation(method)
    ?.let { constructorInvocation ->
      join(
        if (constructorInvocation.target.inSameTypeAs(method.descriptor)) {
          THIS_KEYWORD
        } else {
          SUPER_KEYWORD
        },
        invocationSource(constructorInvocation)
      )
    }
    .orEmpty()

internal val MemberDescriptor.isKtOverride: Boolean
  get() =
    when (this) {
      is MethodDescriptor ->
        isJavaOverride &&
          !directlyOverridesJavaObjectClone &&
          (javaOverriddenMethodDescriptors.any { it.enclosingTypeDescriptor.isInterface } ||
            !needsVisibilityBridge(this))
      else -> false
    }

private val MethodDescriptor.directlyOverridesJavaObjectClone: Boolean
  get() =
    signature == "clone()" &&
      !enclosingTypeDescriptor.isSubtypeOf(typeDescriptors.javaLangCloneable) &&
      javaOverriddenMethodDescriptors.all {
        it.declarationDescriptor.isMemberOf(typeDescriptors.javaLangObject)
      }
