/*
 * Copyright 2025 Google Inc.
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

import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.Variable

internal val Variable.swiftParameterName: String?
  get() = if (typeDescriptor.useWithParameterSwiftName) "with" else null

private val TypeDescriptor.useWithParameterSwiftName: Boolean
  get() =
    when (this) {
      PrimitiveTypes.INT,
      PrimitiveTypes.FLOAT,
      PrimitiveTypes.DOUBLE -> true
      is DeclaredTypeDescriptor -> !isJavaLangObject(this)
      is TypeVariable -> upperBoundTypeDescriptor.useWithParameterSwiftName
      else -> false
    }
