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

// A set of method signatures which will be rendered as TODO("J2KT: not yet supported").
private val todoMethodSignatures: Set<String> =
  setOf(
    // Do not remove this method, it's used for testing.
    "nativekttypes.Main.todoMethod(java.lang.String)"

    // JRE and user-defined methods follows here...
    )

private val MethodDescriptor.todoSignature: String
  get() = "${enclosingTypeDescriptor.qualifiedBinaryName}.$signature"

internal val MethodDescriptor.isTodo: Boolean
  get() = todoMethodSignatures.contains(todoSignature)
