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

import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors

fun Renderer.renderMethod(method: Method) {
  renderNewLine()
  renderMethodHeader(method)
  renderStatement(method.body)
}

private fun Renderer.renderMethodHeader(method: Method) {
  if (method.isStatic) render("@JvmStatic")
  renderNewLine()
  val methodDescriptor = method.descriptor
  render("fun ${methodDescriptor.name!!}")
  renderMethodParameters(method)
  renderMethodDescriptorReturnType(methodDescriptor)
  render(" ")
}

private fun Renderer.renderMethodParameters(method: Method) {
  renderInParentheses { renderCommaSeparated(method.parameters) { renderVariable(it) } }
}

private fun Renderer.renderMethodDescriptorReturnType(methodDescriptor: MethodDescriptor) {
  if (!TypeDescriptors.isPrimitiveVoid(methodDescriptor.returnTypeDescriptor)) {
    render(": ")
    renderTypeDescriptor(methodDescriptor.returnTypeDescriptor)
  }
}
