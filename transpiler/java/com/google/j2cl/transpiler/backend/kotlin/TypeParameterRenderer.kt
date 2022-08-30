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

import com.google.j2cl.transpiler.ast.HasName
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.ast.TypeVariable

internal fun Renderer.renderTypeParameters(typeVariables: List<TypeVariable>) {
  renderInAngleBrackets { renderCommaSeparated(typeVariables) { renderTypeParameter(it) } }
}

internal fun Renderer.renderWhereClause(typeVariables: List<TypeVariable>) {
  val whereClauseItems = typeVariables.map { it.whereClauseItems }.flatten()
  if (whereClauseItems.isNotEmpty()) {
    render(" where ")
    renderCommaSeparated(whereClauseItems) { render(it) }
  }
}

private val TypeVariable.upperBoundTypeDescriptors: List<TypeDescriptor>
  get() =
    upperBoundTypeDescriptor
      .let { if (it is IntersectionTypeDescriptor) it.intersectionTypeDescriptors else listOf(it) }
      .filter { !TypeDescriptors.isJavaLangObject(it) || !it.isNullable }

private fun Renderer.renderTypeParameter(typeVariable: TypeVariable) {
  renderName(typeVariable)
  typeVariable.upperBoundTypeDescriptors.singleOrNull()?.let { boundTypeDescriptor ->
    render(": ")
    renderTypeDescriptor(boundTypeDescriptor, TypeDescriptorUsage.REFERENCE)
  }
}

private data class WhereClauseItem(val hasName: HasName, val boundTypeDescriptor: TypeDescriptor)

private val TypeVariable.whereClauseItems: List<WhereClauseItem>
  get() =
    upperBoundTypeDescriptors.takeIf { it.size > 1 }?.map { WhereClauseItem(this, it) } ?: listOf()

private fun Renderer.render(whereClauseItem: WhereClauseItem) {
  renderName(whereClauseItem.hasName)
  render(": ")
  renderTypeDescriptor(whereClauseItem.boundTypeDescriptor, TypeDescriptorUsage.REFERENCE)
}
