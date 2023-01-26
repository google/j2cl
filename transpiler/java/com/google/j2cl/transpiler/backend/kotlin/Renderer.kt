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

import com.google.j2cl.common.Problems
import com.google.j2cl.transpiler.ast.Type

/** Renderer of the Kotlin source code. */
data class Renderer(
  /** Rendering environment. */
  val environment: Environment,

  /** Rendering problems. */
  val problems: Problems,

  /** Label to render with the return statement. */
  val currentReturnLabelIdentifier: String? = null,

  /** Currently rendered type. */
  val currentType: Type? = null,

  /** Whether to render this reference with explicit qualifier. */
  // TODO(b/252138814): Remove when KT-54349 is fixed
  val renderThisReferenceWithLabel: Boolean = false,

  /** A set of local names which are potentially shadowing imports. */
  val localNames: Set<String> = setOf(),

  /** Top-level qualified names, which will be rendered as simple name without import. */
  val topLevelQualifiedNames: Set<String> = setOf()
)
