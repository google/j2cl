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

import com.google.j2cl.common.InternalCompilerError
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.INTERSECTION_OPERATOR
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.IN_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.NULLABLE_OPERATOR
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.OUT_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.STAR_OPERATOR
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.ampersandSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inAngleBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.infix
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

internal fun Renderer.typeDescriptorSource(
  typeDescriptor: TypeDescriptor,
  asSuperType: Boolean = false,
  projectRawToWildcards: Boolean = false
): Source =
  TypeDescriptorRenderer(
      this,
      asSuperType = asSuperType,
      projectRawToWildcards = projectRawToWildcards
    )
    .source(typeDescriptor.withImplicitNullability)

internal fun Renderer.typeArgumentsSource(typeArguments: List<TypeArgument>): Source =
  TypeDescriptorRenderer(this).argumentsSource(typeArguments)

internal fun Renderer.qualifiedNameSource(
  typeDescriptor: TypeDescriptor,
  asSuperType: Boolean = false
): Source =
  if (typeDescriptor is DeclaredTypeDescriptor) {
    if (typeDescriptor.typeDeclaration.isLocal) {
      identifierSource(typeDescriptor.typeDeclaration.ktSimpleName(asSuperType))
    } else {
      typeDescriptor.enclosingTypeDescriptor.let { enclosingTypeDescriptor ->
        if (enclosingTypeDescriptor == null) {
          topLevelQualifiedNameSource(typeDescriptor.ktQualifiedName(asSuperType))
        } else {
          dotSeparated(
            qualifiedNameSource(enclosingTypeDescriptor),
            identifierSource(typeDescriptor.typeDeclaration.ktSimpleName(asSuperType))
          )
        }
      }
    }
  } else {
    topLevelQualifiedNameSource(typeDescriptor.ktQualifiedName(asSuperType))
  }

/** Type descriptor renderer. */
private data class TypeDescriptorRenderer(
  /** The underlying renderer. */
  val renderer: Renderer,

  /** Set of seen type variables used to detect recursion. */
  val seenTypeVariables: Set<TypeVariable> = setOf(),

  // TODO(b/246842682): Remove when bridge types are materialized as TypeDescriptors
  /** Whether to render a super-type, using bridge name if present. */
  val asSuperType: Boolean = false,

  /** Whether to project RAW types to wildcards, or bounds. */
  val projectRawToWildcards: Boolean = false
) {
  /** Renderer for child type descriptors, including: arguments, bounds, intersections, etc... */
  val child
    get() = copy(asSuperType = false)

  fun source(typeDescriptor: TypeDescriptor): Source =
    when (typeDescriptor) {
      is ArrayTypeDescriptor -> arraySource(typeDescriptor)
      is DeclaredTypeDescriptor -> declaredSource(typeDescriptor)
      is PrimitiveTypeDescriptor -> renderer.qualifiedNameSource(typeDescriptor)
      is TypeVariable -> variableSource(typeDescriptor)
      is IntersectionTypeDescriptor -> intersectionSource(typeDescriptor)
      else -> throw InternalCompilerError("Unexpected ${typeDescriptor::class.java.simpleName}")
    }

  fun arraySource(arrayTypeDescriptor: ArrayTypeDescriptor): Source =
    join(
      renderer.qualifiedNameSource(arrayTypeDescriptor),
      arrayTypeDescriptor.componentTypeDescriptor.let {
        Source.emptyUnless(!it.isPrimitive) { inAngleBrackets(child.source(it)) }
      },
      nullableSuffixSource(arrayTypeDescriptor)
    )

  fun declaredSource(declaredTypeDescriptor: DeclaredTypeDescriptor): Source {
    val typeDeclaration = declaredTypeDescriptor.typeDeclaration
    val enclosingTypeDescriptor = declaredTypeDescriptor.enclosingTypeDescriptor
    val isStatic = !typeDeclaration.isCapturingEnclosingInstance
    return join(
      if (typeDeclaration.isLocal || enclosingTypeDescriptor == null || isStatic) {
        renderer.qualifiedNameSource(declaredTypeDescriptor, asSuperType)
      } else {
        dotSeparated(
          child.declaredSource(enclosingTypeDescriptor.toNonNullable()),
          identifierSource(typeDeclaration.ktSimpleName(asSuperType))
        )
      },
      argumentsSource(declaredTypeDescriptor),
      nullableSuffixSource(declaredTypeDescriptor)
    )
  }

  fun argumentsSource(declaredTypeDescriptor: DeclaredTypeDescriptor): Source =
    declaredTypeDescriptor
      .typeArguments(projectRawToWildcards = projectRawToWildcards)
      .takeIf { it.isNotEmpty() }
      ?.let(::argumentsSource)
      .orEmpty()

  fun argumentsSource(arguments: List<TypeArgument>): Source =
    inAngleBrackets(commaSeparated(arguments.map { source(it) }))

  fun source(typeArgument: TypeArgument): Source = child.source(typeArgument.typeDescriptor)

  fun variableSource(typeVariable: TypeVariable): Source =
    if (didSee(typeVariable)) {
      STAR_OPERATOR
    } else {
      withSeen(typeVariable).run {
        if (typeVariable.isWildcardOrCapture) {
          typeVariable.lowerBoundTypeDescriptor.let { lowerBound ->
            if (lowerBound != null) {
              spaceSeparated(IN_KEYWORD, child.source(lowerBound))
            } else {
              typeVariable.upperBoundTypeDescriptor.let { upperBound ->
                if (upperBound.isImplicitUpperBound) {
                  source("*")
                } else {
                  spaceSeparated(OUT_KEYWORD, child.source(upperBound))
                }
              }
            }
          }
        } else {
          join(renderer.nameSource(typeVariable.toNullable()), nullableSuffixSource(typeVariable))
            .letIf(typeVariable.hasAmpersandAny) {
              infix(it, INTERSECTION_OPERATOR, renderer.topLevelQualifiedNameSource("kotlin.Any"))
            }
        }
      }
    }

  fun intersectionSource(typeDescriptor: IntersectionTypeDescriptor): Source =
    ampersandSeparated(typeDescriptor.intersectionTypeDescriptors.map { source(it) })

  fun nullableSuffixSource(typeDescriptor: TypeDescriptor): Source =
    Source.emptyUnless(typeDescriptor.isNullable) { NULLABLE_OPERATOR }

  fun withSeen(typeVariable: TypeVariable): TypeDescriptorRenderer =
    copy(seenTypeVariables = seenTypeVariables + typeVariable.toNonNullable())

  fun didSee(typeVariable: TypeVariable): Boolean =
    seenTypeVariables.contains(typeVariable.toNonNullable())
}
