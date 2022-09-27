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

internal fun Renderer.renderTypeDescriptor(
  typeDescriptor: TypeDescriptor,
  asSimple: Boolean = false,
  asSuperType: Boolean = false,
  projectRawToWildcards: Boolean = false
) =
  TypeDescriptorRenderer(
      this,
      asSimple = asSimple,
      asSuperType = asSuperType,
      projectRawToWildcards = projectRawToWildcards
    )
    .render(typeDescriptor)

internal fun Renderer.renderTypeArguments(typeArguments: List<TypeArgument>) =
  TypeDescriptorRenderer(this).renderArguments(typeArguments)

/** Type descriptor renderer. */
private data class TypeDescriptorRenderer(
  /** The underlying renderer. */
  val renderer: Renderer,

  /** Set of seen type descriptors used to detect recursion. */
  val seenTypeDescriptors: Set<TypeDescriptor> = setOf(),

  /** Whether to render simple name. */
  val asSimple: Boolean = false,

  // TODO(b/246842682): Remove when bridge types are materialized as TypeDescriptors
  /** Whether to render a super-type, using bridge name if present. */
  val asSuperType: Boolean = false,

  /** Whether to project RAW types to wildcards, or bounds. */
  val projectRawToWildcards: Boolean = false
) {
  /** Renderer for child type descriptors, including: arguments, bounds, intersections, etc... */
  val child
    get() = copy(asSimple = false, asSuperType = false)

  fun render(typeDescriptor: TypeDescriptor) {
    copy(seenTypeDescriptors = seenTypeDescriptors + typeDescriptor.toNonNullable()).run {
      when (typeDescriptor) {
        is ArrayTypeDescriptor -> renderArray(typeDescriptor)
        is DeclaredTypeDescriptor -> renderDeclared(typeDescriptor)
        is PrimitiveTypeDescriptor -> renderer.renderQualifiedName(typeDescriptor)
        is TypeVariable -> renderVariable(typeDescriptor)
        is IntersectionTypeDescriptor -> renderIntersection(typeDescriptor)
        else -> throw InternalCompilerError("Unexpected ${typeDescriptor::class.java.simpleName}")
      }
    }
  }

  fun renderArray(arrayTypeDescriptor: ArrayTypeDescriptor) {
    renderer.renderQualifiedName(arrayTypeDescriptor)
    val componentTypeDescriptor = arrayTypeDescriptor.componentTypeDescriptor
    if (!componentTypeDescriptor.isPrimitive) {
      renderer.renderInAngleBrackets { child.render(componentTypeDescriptor) }
    }
    renderNullableSuffix(arrayTypeDescriptor)
  }

  fun renderDeclared(declaredTypeDescriptor: DeclaredTypeDescriptor) {
    val typeDeclaration = declaredTypeDescriptor.typeDeclaration
    val enclosingTypeDescriptor = declaredTypeDescriptor.enclosingTypeDescriptor
    if (typeDeclaration.isLocal || asSimple) {
      // Skip rendering package name or enclosing type.
      renderer.renderQualifiedName(
        declaredTypeDescriptor,
        asSimple = true,
        asSuperType = asSuperType
      )
    } else if (enclosingTypeDescriptor != null) {
      // Render the enclosing type if present.
      if (!typeDeclaration.isCapturingEnclosingInstance) {
        renderer.renderQualifiedName(enclosingTypeDescriptor)
      } else {
        child.renderDeclared(enclosingTypeDescriptor.toNonNullable())
      }
      renderer.render(".")
      renderer.renderIdentifier(typeDeclaration.ktSimpleName)
    } else {
      renderer.renderQualifiedName(declaredTypeDescriptor, asSuperType = asSuperType)
    }
    renderArguments(declaredTypeDescriptor)
    renderNullableSuffix(declaredTypeDescriptor)
  }

  fun renderArguments(declaredTypeDescriptor: DeclaredTypeDescriptor) {
    val arguments =
      declaredTypeDescriptor.typeArguments(projectRawToWildcards = projectRawToWildcards)
    if (arguments.isNotEmpty()) {
      if (!asSuperType || arguments.all { it.isDenotable }) {
        renderArguments(arguments)
      }
    }
  }

  fun renderArguments(arguments: List<TypeArgument>) {
    renderer.renderInAngleBrackets {
      renderer.renderCommaSeparated(arguments) { child.render(it.typeDescriptor) }
    }
  }

  fun renderVariable(typeVariable: TypeVariable) {
    if (typeVariable.isWildcardOrCapture) {
      val lowerBoundTypeDescriptor = typeVariable.lowerBoundTypeDescriptor
      if (lowerBoundTypeDescriptor != null) {
        renderer.render("in ")
        child.render(lowerBoundTypeDescriptor)
      } else {
        val boundTypeDescriptor = typeVariable.upperBoundTypeDescriptor
        val isRecursive = seenTypeDescriptors.contains(boundTypeDescriptor)
        if (isRecursive || boundTypeDescriptor.isImplicitUpperBound) {
          renderer.render("*")
        } else {
          renderer.render("out ")
          child.render(boundTypeDescriptor)
        }
      }
    } else {
      renderer.renderName(typeVariable.toNullable())
      renderNullableSuffix(typeVariable)
    }
  }

  fun renderIntersection(typeDescriptor: IntersectionTypeDescriptor) {
    // Render only the first type from the intersection and comment out others, as they are not
    // supported in Kotlin.
    // TODO(b/205367162): Support intersection types.
    val typeDescriptors = typeDescriptor.intersectionTypeDescriptors
    child.render(typeDescriptors.first())
    renderer.render(" ")
    renderer.renderInCommentBrackets {
      renderer.render("& ")
      renderer.renderSeparatedWith(typeDescriptors.drop(1), " & ") { child.render(it) }
    }
  }

  fun renderNullableSuffix(typeDescriptor: TypeDescriptor) {
    if (typeDescriptor.isNullable) renderer.render("?")
  }
}
