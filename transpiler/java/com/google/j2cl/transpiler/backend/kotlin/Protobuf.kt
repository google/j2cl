/*
 * Copyright 2022 Google Inc.
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

import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.backend.kotlin.common.camelCaseStartsWith
import com.google.j2cl.transpiler.backend.kotlin.common.runIf

internal val TypeDeclaration.isProtobuf: Boolean
  get() = allSuperTypesIncludingSelf.any { it.packageName == "com.google.protobuf" }

internal val MethodDescriptor.isProtobuf: Boolean
  get() = !isStatic && enclosingTypeDescriptor.typeDeclaration.isProtobuf

private const val GET: String = "get"

internal val MethodDescriptor.isProtobufGetter: Boolean
  get() = isProtobuf && parameterDescriptors.isEmpty() && name?.startsWith(GET) ?: false

internal fun String.toProtobufPropertyName() =
  runIf(camelCaseStartsWith(GET)) { substring(GET.length).replaceFirstChar { it.toLowerCase() } }
