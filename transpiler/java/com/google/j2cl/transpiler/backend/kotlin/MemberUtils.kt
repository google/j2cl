/*
 * Copyright 2024 Google Inc.
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

import com.google.j2cl.transpiler.ast.AstUtils
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.ReturnStatement
import com.google.j2cl.transpiler.ast.Statement
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.backend.kotlin.source.Source

internal val Method.inheritanceModifierSource
  get() =
    when {
      descriptor.isAbstract -> KotlinSource.ABSTRACT_KEYWORD
      needsOpenModifier -> KotlinSource.OPEN_KEYWORD
      needsFinalModifier -> KotlinSource.FINAL_KEYWORD
      else -> Source.EMPTY
    }

private val Method.needsOpenModifier: Boolean
  get() = descriptor.isOpen && !isJavaOverride

private val Method.needsFinalModifier: Boolean
  get() =
    !descriptor.isOpen &&
      isJavaOverride &&
      descriptor.enclosingTypeDescriptor.typeDeclaration.isOpen

internal val Method.renderedStatements: List<Statement>
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

internal val Field.isKtLateInit: Boolean
  get() {
    val isTestProperty =
      descriptor.enclosingTypeDescriptor.typeDeclaration.let { enclosingTypeDeclaration ->
        enclosingTypeDeclaration.isTestClass ||
          enclosingTypeDeclaration.allSuperTypesIncludingSelf.any {
            it.qualifiedSourceName == "junit.framework.TestCase" ||
              it.qualifiedSourceName == "org.junit.TestCase"
          }
      }
    return (descriptor.ktInfo.isUninitializedWarningSuppressed || isTestProperty) &&
      !descriptor.isFinal &&
      !descriptor.typeDescriptor.isNullable &&
      !hasInitializer()
  }
