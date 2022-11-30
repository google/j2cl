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

import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.backend.kotlin.ast.Member
import com.google.j2cl.transpiler.backend.kotlin.ast.kotlinMembers

internal val Type.localNames: Set<String>
  get() =
    kotlinMembers
      .map { member ->
        when (member) {
          is Member.WithCompanionObject -> null
          is Member.WithJavaMember -> (member.javaMember as? Field)?.descriptor?.ktName
          is Member.WithType -> member.type.declaration.ktSimpleName
        }
      }
      .filterNotNull()
      .toSet()

internal val Type.declaredSuperTypeDescriptors: List<TypeDescriptor>
  get() = listOfNotNull(superTypeDescriptor).plus(superInterfaceTypeDescriptors)
