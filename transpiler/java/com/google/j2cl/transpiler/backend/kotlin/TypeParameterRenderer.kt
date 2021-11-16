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

import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.ast.TypeVariable

internal fun Renderer.renderTypeParameters(
  typeVariables: List<TypeVariable>,
  trailingSpace: Boolean = false
) =
  typeVariables.takeIf { it.isNotEmpty() }?.let { typeVariables ->
    renderInAngleBrackets {
      renderCommaSeparated(typeVariables) { render(it.typeParameterSourceString) }
    }
    if (trailingSpace) render(" ")
  }

internal fun Renderer.renderWhereClause(typeVariables: List<TypeVariable>) =
  typeVariables.map { it.whereClauseSourceStrings }.flatten().takeIf { it.isNotEmpty() }?.let {
    sourceStrings ->
    render(" where ")
    renderCommaSeparated(sourceStrings) { render(it) }
  }

private val TypeVariable.boundTypeDescriptors: List<TypeDescriptor>
  get() =
    boundTypeDescriptor.run {
      if (this is IntersectionTypeDescriptor) {
        intersectionTypeDescriptors
      } else {
        listOf(this).filter { !TypeDescriptors.isJavaLangObject(it) }
      }
    }

private val TypeVariable.typeParameterSourceString: String
  get() =
    name.identifierSourceString.let { nameSourceString ->
      boundTypeDescriptors.singleOrNull().let { boundTypeDescriptorOrNull ->
        if (boundTypeDescriptorOrNull == null) nameSourceString
        else "$nameSourceString: ${boundTypeDescriptorOrNull.argumentSourceString}"
      }
    }

private val TypeVariable.whereClauseSourceStrings: List<String>
  get() =
    name.identifierSourceString.let { nameSourceString ->
      boundTypeDescriptors.takeIf { it.size > 1 }?.map {
        "$nameSourceString: ${it.argumentSourceString}"
      }
        ?: listOf()
    }
