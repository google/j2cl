/*
 * Copyright 2023 Google Inc.
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

import com.google.j2cl.transpiler.ast.TypeDeclaration

/** J2KT representation of Kotlin companion object declaration. */
data class CompanionDeclaration(
  /** The enclosing Java type declaration. */
  val enclosingTypeDeclaration: TypeDeclaration
)

/** The companion object declaration inside this Java type. */
val TypeDeclaration.companionDeclaration: CompanionDeclaration
  get() = CompanionDeclaration(this)
