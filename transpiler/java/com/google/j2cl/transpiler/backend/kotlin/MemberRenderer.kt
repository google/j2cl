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
import com.google.j2cl.transpiler.ast.HasName
import com.google.j2cl.transpiler.ast.InitializerBlock
import com.google.j2cl.transpiler.ast.Member as JavaMember
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.ReturnStatement
import com.google.j2cl.transpiler.ast.Statement
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.backend.kotlin.ast.CompanionObject
import com.google.j2cl.transpiler.backend.kotlin.ast.Member

internal fun Renderer.render(member: Member) {
  when (member) {
    is Member.WithCompanionObject -> render(member.companionObject)
    is Member.WithJavaMember -> renderMember(member.javaMember)
    is Member.WithType -> renderType(member.type)
  }
}

private fun Renderer.render(companionObject: CompanionObject) {
  render("companion object ")
  renderInCurlyBrackets {
    renderNewLine()
    renderSeparatedWithEmptyLine(companionObject.members) { render(it) }
  }
}

private fun Renderer.renderMember(member: JavaMember) {
  when (member) {
    is Method -> renderMethod(member)
    is Field -> renderField(member)
    is InitializerBlock -> renderInitializerBlock(member)
    else -> throw InternalCompilerError("Unhandled ${member::class}")
  }
}

private fun Renderer.renderMethod(method: Method) {
  renderMethodHeader(method)
  if (!method.isAbstract && !method.isNative) {
    val statements = getStatements(method)

    // Constructors with no statements can be rendered without curly braces.
    if (!method.isConstructor || statements.isNotEmpty()) {
      render(" ")
      if (method.descriptor.isKtProperty) render("get() ")
      copy(currentReturnLabelIdentifier = null).run {
        renderInCurlyBrackets {
          if (method.descriptor.isTodo) {
            renderNewLine()
            renderTodo("J2KT: not yet supported")
          } else {
            renderStatements(statements)
          }
        }
      }
    }
  }
}

private fun Renderer.getStatements(method: Method): List<Statement> {
  if (!method.descriptor.isKtDisabled) {
    return method.body.statements.filter { !isConstructorInvocationStatement(it) }
  }

  if (TypeDescriptors.isPrimitiveVoid(method.descriptor.returnTypeDescriptor)) {
    return listOf()
  }

  return listOf(
    ReturnStatement.newBuilder()
      .setSourcePosition(method.sourcePosition)
      .setExpression(method.descriptor.returnTypeDescriptor.defaultValue)
      .build()
  )
}

private fun Renderer.renderField(field: Field) {
  val isFinal = field.descriptor.isFinal
  val typeDescriptor = field.descriptor.typeDescriptor

  if (field.isCompileTimeConstant && field.isStatic) {
    render("const ")
  } else {
    renderJvmFieldAnnotation()
  }
  render(if (isFinal) "val " else "var ")
  renderIdentifier(field.descriptor.ktMangledName)
  render(": ")
  renderTypeDescriptor(typeDescriptor)
  field.initializer?.let { initializer ->
    render(" = ")
    renderExpression(initializer)
  }
}

private fun Renderer.renderJvmFieldAnnotation() {
  render("@")
  renderQualifiedName("kotlin.jvm.JvmField")
  render(" ")
}

private fun Renderer.renderJvmStaticAnnotation() {
  render("@")
  renderQualifiedName("kotlin.jvm.JvmStatic")
  renderNewLine()
}

private fun Renderer.renderInitializerBlock(initializerBlock: InitializerBlock) {
  render("init ")
  renderStatement(initializerBlock.block)
}

private fun Renderer.renderMethodHeader(method: Method) {
  if (method.isStatic) {
    renderJvmStaticAnnotation()
  }
  val methodDescriptor = method.descriptor
  if (methodDescriptor.visibility.needsObjCNameAnnotation && !methodDescriptor.isKtOverride) {
    renderObjCNameAnnotation(methodDescriptor)
  }
  renderMethodModifiers(methodDescriptor)
  if (methodDescriptor.isConstructor) {
    render("constructor")
  } else {
    render(if (method.descriptor.isKtProperty) "val " else "fun ")
    val typeParameters = methodDescriptor.typeParameterTypeDescriptors
    if (typeParameters.isNotEmpty()) {
      renderTypeParameters(methodDescriptor.typeParameterTypeDescriptors)
      render(" ")
    }
    renderIdentifier(methodDescriptor.ktMangledName)
  }
  if (!method.descriptor.isKtProperty) {
    renderMethodParameters(method)
  }
  if (methodDescriptor.isConstructor) {
    renderConstructorInvocation(method)
  } else {
    renderMethodReturnType(methodDescriptor)
  }
  renderWhereClause(methodDescriptor.typeParameterTypeDescriptors)
}

private fun Renderer.renderMethodModifiers(methodDescriptor: MethodDescriptor) {
  if (!methodDescriptor.enclosingTypeDescriptor.typeDeclaration.isInterface) {
    if (methodDescriptor.isNative) {
      render("external ")
    }
    if (methodDescriptor.isAbstract) {
      render("abstract ")
    } else if (
      !methodDescriptor.isFinal &&
        !methodDescriptor.isConstructor &&
        !methodDescriptor.isStatic &&
        !methodDescriptor.visibility.isPrivate
    ) {
      render("open ")
    }
  }
  if (methodDescriptor.isKtOverride) {
    render("override ")
  }
}

private fun Renderer.renderMethodParameters(method: Method) {
  val parameterDescriptors = method.descriptor.parameterDescriptors
  val parameters = method.parameters
  val includeObjCNameAnnotation =
    method.descriptor.visibility.needsObjCNameAnnotation && !method.descriptor.isKtOverride
  val renderWithNewLines = includeObjCNameAnnotation && parameters.isNotEmpty()
  renderInParentheses {
    renderIndentedIf(renderWithNewLines) {
      renderCommaSeparated(0 until parameters.size) { index ->
        if (renderWithNewLines) renderNewLine()
        renderParameter(parameterDescriptors[index], parameters[index], includeObjCNameAnnotation)
      }
    }
    if (renderWithNewLines) renderNewLine()
  }
}

private fun Renderer.renderParameter(
  parameterDescriptor: ParameterDescriptor,
  name: HasName,
  includeObjCNameAnnotation: Boolean = false
) {
  val parameterTypeDescriptor = parameterDescriptor.typeDescriptor
  val renderedTypeDescriptor =
    if (!parameterDescriptor.isVarargs) parameterTypeDescriptor
    else (parameterTypeDescriptor as ArrayTypeDescriptor).componentTypeDescriptor!!
  if (parameterDescriptor.isVarargs) render("vararg ")
  if (includeObjCNameAnnotation) renderObjCNameAnnotation(parameterDescriptor)
  renderName(name)
  render(": ")
  renderTypeDescriptor(renderedTypeDescriptor)
}

internal fun Renderer.renderMethodReturnType(methodDescriptor: MethodDescriptor) {
  val returnTypeDescriptor = methodDescriptor.returnTypeDescriptor
  if (returnTypeDescriptor != PrimitiveTypes.VOID) {
    render(": ")
    renderTypeDescriptor(returnTypeDescriptor)
  }
}

private fun Renderer.renderConstructorInvocation(method: Method) {
  getConstructorInvocation(method)?.let { constructorInvocation ->
    render(": ")
    render(if (constructorInvocation.target.inSameTypeAs(method.descriptor)) "this" else "super")
    renderInvocationArguments(constructorInvocation)
  }
}

private val MethodDescriptor.isKtOverride
  get() =
    isJavaOverride &&
      (javaOverriddenMethodDescriptors.any { it.enclosingTypeDescriptor.isInterface } ||
        !needsVisibilityBridge(this))
