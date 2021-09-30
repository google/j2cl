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

import com.google.j2cl.transpiler.ast.CompilationUnit

fun Renderer.renderCompilationUnit(compilationUnit: CompilationUnit) {
  renderPackageDeclaration(compilationUnit.packageName)
  renderSeparatedWithEmptyLine(compilationUnit.types) { renderType(it) }
}

private fun Renderer.renderPackageDeclaration(packageName: String) {
  if (packageName.isNotEmpty()) {
    // TODO(micapolos): Render each package component as identifier, with potential backticks
    render("package $packageName")
    renderNewLine()
    renderNewLine()
  }
}
