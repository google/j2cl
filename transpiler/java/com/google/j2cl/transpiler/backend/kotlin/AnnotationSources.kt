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

import com.google.j2cl.common.InternalCompilerError
import com.google.j2cl.transpiler.ast.Annotation
import com.google.j2cl.transpiler.ast.AnnotationValue
import com.google.j2cl.transpiler.ast.HasAnnotations
import com.google.j2cl.transpiler.ast.Literal
import com.google.j2cl.transpiler.ast.TypeLiteral
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotation
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotationName
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.assignment
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.classLiteral
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source

internal data class AnnotationSources(val nameSources: NameSources) {
  private val literalSources: LiteralSources
    get() = LiteralSources(nameSources)

  fun annotationsSource(hasAnnotations: HasAnnotations, isProperty: Boolean = false): Source =
    newLineSeparated(
      hasAnnotations.annotations
        .filter { SUPPORTED_ANNOTATIONS.contains(it.typeDescriptor.qualifiedSourceName) }
        .map { annotationSource(it, isProperty) }
    )

  private fun annotationSource(annotation: Annotation, isProperty: Boolean): Source =
    annotation(
      annotationNameSource(annotation, isProperty),
      annotation.singleValueOrNull().let { singleValue ->
        if (singleValue is Literal) {
          listOf(annotationValueSource(singleValue))
        } else {
          // TODO(b/444430700): Filter default values when they are supported.
          annotation.values.entries.map { entry ->
            assignment(source(entry.key), annotationValueSource(entry.value))
          }
        }
      },
    )

  internal fun annotationNameSource(annotation: Annotation, isProperty: Boolean): Source =
    annotationName(
      annotationTargetSource(annotation, isProperty),
      nameSources.qualifiedNameSource(annotation.typeDescriptor),
    )

  private fun annotationValueSource(annotationValue: AnnotationValue): Source =
    when (annotationValue) {
      is TypeLiteral ->
        classLiteral(nameSources.qualifiedNameSource(annotationValue.referencedTypeDescriptor))
      is Literal -> literalSources.literalSource(annotationValue)
      else -> throw InternalCompilerError("Unexpected ${annotationValue::class.simpleName}")
    }

  fun volatileAnnotationSource(): Source =
    annotation(nameSources.topLevelQualifiedNameSource("kotlin.concurrent.Volatile"))

  // TODO(b/444430700): Filter default values when they are supported.
  private fun Annotation.singleValueOrNull(): AnnotationValue? =
    values.entries.singleOrNull()?.takeIf { it.key == "value" }?.value

  companion object {
    private val SUPPORTED_ANNOTATIONS =
      setOf(
        "com.google.errorprone.annotations.CanIgnoreReturnValue",
        "com.google.errorprone.annotations.ResultIgnorabilityUnspecified",
        "com.google.j2objc.annotations.ObjectiveCKmpMethod",
        "com.google.j2objc.annotations.ObjectiveCName",
        "com.google.j2objc.annotations.SwiftName",
      )

    private val PROPERTY_GET_ANNOTATIONS =
      setOf(
        "com.google.j2objc.annotations.ObjectiveCName",
        "com.google.j2objc.annotations.SwiftName",
      )

    private fun hasGetTarget(annotation: Annotation, isProperty: Boolean): Boolean =
      isProperty && PROPERTY_GET_ANNOTATIONS.contains(annotation.typeDescriptor.qualifiedSourceName)

    fun annotationTargetSource(annotation: Annotation, isProperty: Boolean): Source =
      Source.emptyUnless(hasGetTarget(annotation, isProperty)) { KotlinSource.GET_KEYWORD }
  }
}
