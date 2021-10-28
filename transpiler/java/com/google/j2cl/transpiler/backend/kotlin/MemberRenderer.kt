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
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.InitializerBlock
import com.google.j2cl.transpiler.ast.Kind
import com.google.j2cl.transpiler.ast.Member
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.ast.Visibility

internal fun Renderer.renderMember(member: Member, kind: Kind) {
  when (member) {
    is Method -> renderMethod(member, kind)
    is Field -> renderField(member)
    is InitializerBlock -> renderInitializerBlock(member)
    else -> throw InternalCompilerError("Unhandled ${member::class}")
  }
}

private fun Renderer.renderMethod(method: Method, kind: Kind) {
  renderMethodHeader(method, kind)
  if (!method.isAbstract) {
    render(" ")
    renderStatement(method.body)
  }
}

private fun Renderer.renderField(field: Field) {
  val isFinal = field.descriptor.isFinal
  val typeDescriptor = field.descriptor.typeDescriptor

  if (field.isStatic) render("@JvmStatic ")
  renderVisibility(field.descriptor.visibility)
  render(if (isFinal) "val " else "var ")
  render("${field.descriptor.name!!.identifierSourceString}: ${typeDescriptor.sourceString}")
  field.initializer?.let { initializer ->
    render(" = ")
    renderExpression(initializer)
  }
}

private fun Renderer.renderInitializerBlock(initializerBlock: InitializerBlock) {
  render("init ")
  renderStatement(initializerBlock.block)
}

private fun Renderer.renderMethodHeader(method: Method, kind: Kind) {
  if (method.isStatic) {
    render("@JvmStatic")
    renderNewLine()
  }
  val methodDescriptor = method.descriptor
  renderMethodModifiers(methodDescriptor, kind)
  if (methodDescriptor.isConstructor) {
    render("constructor")
  } else {
    render("fun ")
    renderMethodDescriptorTypeParameters(methodDescriptor)
    render(methodDescriptor.name!!.identifierSourceString)
  }
  renderMethodParameters(method)
  renderMethodReturnType(methodDescriptor)
  // TODO(b/202527616): Render this() and super() constructor calls after ":".
}

private fun Renderer.renderMethodModifiers(methodDescriptor: MethodDescriptor, kind: Kind) {
  renderVisibility(methodDescriptor.visibility)
  if (kind != Kind.INTERFACE) {
    if (methodDescriptor.isAbstract) render("abstract ")
    if (!methodDescriptor.isFinal &&
        !methodDescriptor.isConstructor &&
        !methodDescriptor.isStatic &&
        !methodDescriptor.visibility.isPrivate
    ) {
      render("open ")
    }
  }
}

private fun Renderer.renderMethodParameters(method: Method) {
  val parameters = method.parameters
  val varargParameterIndex = if (method.descriptor.isVarargs) parameters.size.dec() else -1
  renderInParentheses {
    renderCommaSeparated(parameters.mapIndexed(::IndexedValue)) {
      renderParameter(it.value, isVararg = it.index == varargParameterIndex)
    }
  }
}

private fun Renderer.renderParameter(variable: Variable, isVararg: Boolean) {
  val variableTypeDescriptor = variable.typeDescriptor
  val renderedTypeDescriptor =
    if (!isVararg) variableTypeDescriptor
    else (variableTypeDescriptor as ArrayTypeDescriptor).componentTypeDescriptor!!
  if (isVararg) render("vararg ")
  renderName(variable)
  render(": ${renderedTypeDescriptor.sourceString}")
}

private fun Renderer.renderMethodDescriptorTypeParameters(methodDescriptor: MethodDescriptor) {
  methodDescriptor.typeParameterTypeDescriptors.takeIf { it.isNotEmpty() }?.let {
    typeParameterTypeDescriptors ->
    renderInAngleBrackets {
      renderCommaSeparated(typeParameterTypeDescriptors) { render(it.declarationSourceString) }
    }
    render(" ")
  }
}

private fun Renderer.renderMethodReturnType(methodDescriptor: MethodDescriptor) {
  if (!methodDescriptor.isConstructor) {
    methodDescriptor.returnTypeDescriptor.takeIf { it != PrimitiveTypes.VOID }?.let {
      render(": ${it.sourceString}")
    }
  }
}

private fun Renderer.renderVisibility(visibility: Visibility) {
  visibility.sourceStringOrNull?.let { render("$it ") }
}
