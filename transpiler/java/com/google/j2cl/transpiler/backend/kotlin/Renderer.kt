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

import com.google.j2cl.transpiler.ast.Type

/**
 * Provides context for rendering Kotlin source code.
 *
 * @property environment rendering environment
 * @property currentReturnLabelIdentifier optional label to render in return statements
 * @property currentType currently rendered type, or null if not rendering a type
 * @property renderThisReferenceWithLabel whether to render this reference with explicit qualifier
 * @property localNames a set of local names which are potentially shadowing imports
 * @property topLevelQualifiedNames top-level qualified names, which will be rendered as simple name
 *   without import
 */
internal data class Renderer(
  val environment: Environment,
  val currentReturnLabelIdentifier: String? = null,
  val currentType: Type? = null,
  // TODO(b/252138814): Remove when KT-54349 is fixed
  val renderThisReferenceWithLabel: Boolean = false,
  val localNames: Set<String> = setOf(),
  val topLevelQualifiedNames: Set<String> = setOf()
)
