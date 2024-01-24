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
package com.google.j2cl.transpiler.backend.kotlin.ast

import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDeclaration

/** J2KT representation of Kotlin companion object. */
data class CompanionObject(
  /** The enclosing Java type declaration. */
  val enclosingTypeDeclaration: TypeDeclaration,

  /** The list of members inside this companion object. */
  val members: List<Member>,
)

/** Returns the declaration of this companion object. */
val CompanionObject.declaration: CompanionDeclaration
  get() = enclosingTypeDeclaration.companionDeclaration

/**
 * Returns companion object for this Java type, or null if the type does not need to have one, since
 * it has no static members.
 */
fun Type.toCompanionObjectOrNull(): CompanionObject? =
  members
    .filter { it.isStatic && !it.isEnumField }
    .map { Member.WithJavaMember(it) }
    .takeIf { it.isNotEmpty() }
    ?.let { CompanionObject(declaration, it) }
