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
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.ifNotEmpty
import com.google.j2cl.transpiler.backend.kotlin.source.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.spaceSeparated

internal fun Renderer.fileHeaderSource(compilationUnit: CompilationUnit): Source =
  newLineSeparated(fileCommentSource(compilationUnit), fileAnnotationsSource())

private fun fileCommentSource(compilationUnit: CompilationUnit) =
  source("// Generated from \"${compilationUnit.packageRelativePath}\"")

private fun Renderer.fileAnnotationsSource(): Source =
  newLineSeparated(optInExperimentalObjCNameFileAnnotationSource())

internal fun Renderer.packageAndImportsSource(compilationUnit: CompilationUnit): Source =
  emptyLineSeparated(packageSource(compilationUnit), importsSource())

private fun packageSource(compilationUnit: CompilationUnit): Source =
  source(compilationUnit.packageName).ifNotEmpty { spaceSeparated(source("package"), it) }

internal fun Renderer.typesSource(compilationUnit: CompilationUnit): Source =
  emptyLineSeparated(compilationUnit.types.map(::typeSource))
