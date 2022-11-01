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

import com.google.j2cl.transpiler.backend.kotlin.ast.Import
import com.google.j2cl.transpiler.backend.kotlin.ast.defaultImports

internal fun Renderer.renderImports(importedQualifiedNames: Set<String>) {
  val imports = defaultImports + importedQualifiedNames.map { Import(it.qualifiedNameComponents()) }
  if (imports.isNotEmpty()) {
    renderSeparatedWith(imports.sorted(), "\n") { render(it) }
    renderNewLine()
    renderNewLine()
  }
}

private fun Renderer.render(import: Import) {
  render("import ")
  renderSeparatedWith(import.components, ".") { renderIdentifier(it) }
  if (import.isStar) render(".*")
}
