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

import com.google.j2cl.transpiler.ast.Member as JavaMember
import com.google.j2cl.transpiler.ast.Type

/** Kotlin member. */
sealed class Member {
  /** Kotlin member mapping to a Java member. */
  data class WithJavaMember(val javaMember: JavaMember) : Member()

  /** Kotlin member mapping to a Java Type. */
  data class WithType(val type: Type) : Member()

  /** Kotlin companion object member. */
  data class WithCompanionObject(val companionObject: CompanionObject) : Member()
}
