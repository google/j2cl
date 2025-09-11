/*
 * Copyright 2025 Google Inc.
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

import com.google.j2cl.transpiler.ast.Annotation
import com.google.j2cl.transpiler.ast.HasAnnotations
import com.google.j2cl.transpiler.ast.Literal
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotation
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.assignment
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source

internal data class AnnotationRenderer(val nameRenderer: NameRenderer) {
  private val literalRenderer: LiteralRenderer
    get() = LiteralRenderer(nameRenderer)

  fun annotationsSource(hasAnnotations: HasAnnotations): Source =
    newLineSeparated(
      hasAnnotations.annotations
        .filter { RENDERED_ANNOTATIONS.contains(it.typeDescriptor.qualifiedSourceName) }
        .map { annotationSource(it) }
    )

  private fun annotationSource(annotation: Annotation): Source =
    annotation(
      nameRenderer.qualifiedNameSource(annotation.typeDescriptor),
      annotation.singleValueOrNull().let { singleValue ->
        if (singleValue != null) {
          listOf(literalRenderer.literalSource(singleValue))
        } else {
          // TODO(b/444430700): Filter default values when they are supported.
          annotation.values.entries.map { entry ->
            assignment(source(entry.key), literalRenderer.literalSource(entry.value))
          }
        }
      },
    )

  // TODO(b/444430700): Filter default values when they are supported.
  private fun Annotation.singleValueOrNull(): Literal? =
    values.entries.singleOrNull()?.takeIf { it.key == "value" }?.value

  companion object {
    private val RENDERED_ANNOTATIONS =
      setOf(
        "com.google.j2objc.annotations.ObjectiveCName",
        "com.google.j2objc.annotations.SwiftName",
      )
  }
}
