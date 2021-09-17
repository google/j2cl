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

import com.google.j2cl.common.Problems
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.CompilationUnit
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveVoid
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.backend.common.SourceBuilder

class KotlinGenerator(private val sourceBuilder: SourceBuilder, private val problems: Problems) {
  private val statementTranspiler = StatementTranspiler(sourceBuilder)

  fun renderCompilationUnit(compilationUnit: CompilationUnit) {
    renderPackageName(compilationUnit.packageName)
    compilationUnit.types.forEach { renderType(it) }
  }

  private fun renderPackageName(packageName: String) {
    if (packageName.isNotEmpty()) {
      sourceBuilder.append("package ")
      sourceBuilder.appendln(packageName)
      sourceBuilder.newLine()
    }
  }

  private fun renderType(type: Type) {
    if (!type.declaration.isFinal) {
      sourceBuilder.append("open ")
    }
    sourceBuilder.append("class ")
    sourceBuilder.append(type.declaration.simpleSourceName)

    // TODO(dpo): add support for class hierarchies
    renderTypeExtendsClause(type)

    sourceBuilder.inBraces { renderTypeBody(type) }
  }

  private fun renderTypeExtendsClause(type: Type) {
    val superTypeDescriptor = type.superTypeDescriptor
    if (superTypeDescriptor != null && !isJavaLangObject(superTypeDescriptor)) {
      val superTypeName = superTypeDescriptor.qualifiedSourceName
      sourceBuilder.append(" extends ")
      sourceBuilder.append(superTypeName)
    }
  }

  private fun renderTypeBody(type: Type) {
    // TODO(dpo): add support for field declarations
    // TODO(dpo): Remove short term hack to pull static methods into companion object.
    val (staticMethods, instanceMethods) = type.methods.partition { it.isStatic }

    instanceMethods.forEach { renderMethod(it) }

    if (staticMethods.isNotEmpty()) {
      sourceBuilder.append("companion object ")
      sourceBuilder.inBraces { type.methods.forEach { renderMethod(it) } }
    }
  }

  private fun renderMethod(method: Method) {
    sourceBuilder.newLine()
    renderMethodHeader(method)
    statementTranspiler.renderStatement(method.body)
  }

  private fun renderMethodHeader(method: Method) {
    if (method.isStatic) {
      sourceBuilder.appendln("@JvmStatic")
    }
    val methodDescriptor = method.descriptor
    sourceBuilder.append("fun ")
    renderMethodDescriptorName(methodDescriptor)
    renderMethodParameters(method)
    renderMethodDescriptorReturnType(methodDescriptor)
    sourceBuilder.append(" ")
  }

  private fun renderMethodDescriptorName(methodDescriptor: MethodDescriptor) {
    // TODO(micapolos): Handle Kotlin reserved keywords, like 'in' etc...
    sourceBuilder.append(methodDescriptor.name)
  }

  private fun renderMethodParameters(method: Method) {
    sourceBuilder.inParens {
      sourceBuilder.separatedWith(method.parameters, ", ") { renderVariable(it) }
    }
  }

  private fun renderMethodDescriptorReturnType(methodDescriptor: MethodDescriptor) {
    if (!isPrimitiveVoid(methodDescriptor.returnTypeDescriptor)) {
      sourceBuilder.append(": ")
      renderTypeDescriptor(methodDescriptor.returnTypeDescriptor)
    }
  }

  private fun renderVariable(variable: Variable) {
    sourceBuilder.append(variable.name)
    sourceBuilder.append(": ")
    renderTypeDescriptor(variable.typeDescriptor)
  }

  /**
   * Emits the proper fully rendered type for the give type descriptor at the current location.
   * TODO(dpo): Move this to a better long term place (this logic is likely to get pretty complex).
   */
  private fun renderTypeDescriptor(typeDescriptor: TypeDescriptor) {
    when (typeDescriptor) {
      is ArrayTypeDescriptor -> renderArrayTypeDescriptor(typeDescriptor)
      // TODO(dpo): Other type descriptor logic.
      else -> renderTypeDescriptorDescription(typeDescriptor)
    }
  }

  private fun renderArrayTypeDescriptor(arrayTypeDescriptor: ArrayTypeDescriptor) {
    sourceBuilder.append("Array<")
    renderTypeDescriptor(arrayTypeDescriptor.componentTypeDescriptor!!)
    sourceBuilder.append(">")
  }

  private fun renderTypeDescriptorDescription(typeDescriptor: TypeDescriptor) {
    sourceBuilder.append(typeDescriptor.readableDescription)
  }
}
