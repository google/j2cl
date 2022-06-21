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

internal fun Renderer.renderIdentifier(identifier: String) {
  // Dollar sign ($) is not a valid identifier character since Kotlin 1.7, as well as many other
  // characters. For now, it is replaced with triple underscores (___) to minimise a risk of
  // conflict.
  // TODO(b/236360941): Implement escaping which would work across all platforms.
  val kotlinIdentifier = identifier.replace("$", "___")
  if (KotlinKeywords.isHardKeyword(kotlinIdentifier) || !kotlinIdentifier.isValidIdentifier) {
    renderInBackticks { render(kotlinIdentifier) }
  } else {
    render(kotlinIdentifier)
  }
}

internal fun Renderer.renderPackageName(packageName: String) {
  renderQualifiedIdentifier(packageName)
}

internal fun Renderer.renderQualifiedName(qualifiedName: String) {
  renderQualifiedIdentifier(qualifiedName)
}

private fun Renderer.renderQualifiedIdentifier(identifier: String) {
  renderDotSeparated(identifier.split('.')) { renderIdentifier(it) }
}

private val String.isValidIdentifier
  get() = first().isValidIdentifierFirstChar && all { it.isValidIdentifierChar }

private val Char.isValidIdentifierChar
  get() = isLetterOrDigit() || this == '_'

private val Char.isValidIdentifierFirstChar
  get() = isValidIdentifierChar && !isDigit()
