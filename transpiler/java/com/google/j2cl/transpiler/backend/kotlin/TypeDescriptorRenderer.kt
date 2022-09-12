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
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor

internal fun Renderer.renderTypeDescriptor(
  typeDescriptor: TypeDescriptor,
  asSimple: Boolean = false,
  asSuperType: Boolean = false
) {
  when (typeDescriptor) {
    is ArrayTypeDescriptor -> renderArrayTypeDescriptor(typeDescriptor)
    is DeclaredTypeDescriptor ->
      renderDeclaredTypeDescriptor(typeDescriptor, asSimple = asSimple, asSuperType = asSuperType)
    is PrimitiveTypeDescriptor -> renderQualifiedName(typeDescriptor)
    is TypeVariable -> renderTypeVariable(typeDescriptor)
    is IntersectionTypeDescriptor -> renderIntersectionTypeDescriptor(typeDescriptor)
    else -> throw InternalCompilerError("Unexpected ${typeDescriptor::class.java.simpleName}")
  }
}

private fun Renderer.renderNullableSuffix(typeDescriptor: TypeDescriptor) {
  if (typeDescriptor.isNullable) render("?")
}

private fun Renderer.renderArrayTypeDescriptor(arrayTypeDescriptor: ArrayTypeDescriptor) {
  renderQualifiedName(arrayTypeDescriptor)
  val componentTypeDescriptor = arrayTypeDescriptor.componentTypeDescriptor!!
  if (!componentTypeDescriptor.isPrimitive) {
    renderInAngleBrackets { renderTypeDescriptor(componentTypeDescriptor) }
  }
  renderNullableSuffix(arrayTypeDescriptor)
}

private fun Renderer.renderDeclaredTypeDescriptor(
  declaredTypeDescriptor: DeclaredTypeDescriptor,
  asSimple: Boolean = false,
  asSuperType: Boolean = false
) {
  val typeDeclaration = declaredTypeDescriptor.typeDeclaration
  val enclosingTypeDescriptor = declaredTypeDescriptor.enclosingTypeDescriptor
  if (typeDeclaration.isLocal || asSimple) {
    // Skip rendering package name or enclosing type.
    renderIdentifier(typeDeclaration.ktSimpleName)
  } else if (enclosingTypeDescriptor != null) {
    // Render the enclosing type if present.
    if (!typeDeclaration.isCapturingEnclosingInstance) {
      renderQualifiedName(enclosingTypeDescriptor)
    } else {
      renderDeclaredTypeDescriptor(enclosingTypeDescriptor.toNonNullable())
    }
    render(".")
    renderIdentifier(typeDeclaration.ktSimpleName)
  } else {
    // Render the qualified or bridge name for this top-level type.
    var qualifiedName = typeDeclaration.ktQualifiedName
    if (asSuperType && typeDeclaration.ktBridgeQualifiedName != null) {
      qualifiedName = typeDeclaration.ktBridgeQualifiedName
    }
    renderQualifiedName(qualifiedName)
  }

  renderTypeArguments(declaredTypeDescriptor, asSuperType = asSuperType)
  renderNullableSuffix(declaredTypeDescriptor)
}

private fun Renderer.renderTypeArguments(
  declaredTypeDescriptor: DeclaredTypeDescriptor,
  asSuperType: Boolean = false
) {
  val parameters = declaredTypeDescriptor.typeDeclaration.directlyDeclaredTypeParameterDescriptors
  val arguments = declaredTypeDescriptor.directlyDeclaredTypeArgumentDescriptors
  if (arguments.isNotEmpty()) {
    if (!asSuperType || arguments.all { it.isDenotable }) {
      renderTypeArguments(parameters, arguments)
    }
  }
}

internal fun Renderer.renderTypeArguments(
  typeParameters: List<TypeVariable>,
  typeArguments: List<TypeDescriptor>
) {
  renderInAngleBrackets {
    renderCommaSeparated(inferNonNullableBounds(typeParameters, typeArguments)) {
      renderTypeDescriptor(it)
    }
  }
}

private fun inferNonNullableBounds(
  typeParameters: List<TypeVariable>,
  typeArguments: List<TypeDescriptor>
): List<TypeDescriptor> = typeParameters.zip(typeArguments, ::inferNonNullableBounds)

private fun inferNonNullableBounds(
  typeParameter: TypeVariable,
  typeArgument: TypeDescriptor
): TypeDescriptor =
  if (!typeParameter.upperBoundTypeDescriptor.isNullable) typeArgument.toNonNullable()
  else typeArgument

private fun Renderer.renderTypeVariable(typeVariable: TypeVariable) {
  if (typeVariable.isWildcardOrCapture) {
    val lowerBoundTypeDescriptor = typeVariable.lowerBoundTypeDescriptor
    if (lowerBoundTypeDescriptor != null) {
      render("in ")
      renderTypeDescriptor(lowerBoundTypeDescriptor)
    } else {
      val boundTypeDescriptor = typeVariable.upperBoundTypeDescriptor
      if (boundTypeDescriptor.isImplicitUpperBound) {
        render("*")
      } else {
        render("out ")
        renderTypeDescriptor(boundTypeDescriptor)
      }
    }
  } else {
    renderName(typeVariable.toNullable())
    renderNullableSuffix(typeVariable)
  }
}

private fun Renderer.renderIntersectionTypeDescriptor(
  intersectionTypeDescriptor: IntersectionTypeDescriptor
) {
  // Render only the first type from the intersection and comment out others, as they are not
  // supported in Kotlin.
  // TODO(b/205367162): Support intersection types.
  val typeDescriptors = intersectionTypeDescriptor.intersectionTypeDescriptors
  renderTypeDescriptor(typeDescriptors.first())
  render(" ")
  renderInCommentBrackets {
    render("& ")
    renderSeparatedWith(typeDescriptors.drop(1), " & ") { renderTypeDescriptor(it) }
  }
}

/** Returns whether this type is denotable as a top-level type of type argument. */
internal val TypeDescriptor.isDenotable
  get() =
    when (this) {
      is DeclaredTypeDescriptor -> !typeDeclaration.isAnonymous
      is TypeVariable -> !isWildcardOrCapture
      is IntersectionTypeDescriptor -> false
      is UnionTypeDescriptor -> false
      is PrimitiveTypeDescriptor -> true
      is ArrayTypeDescriptor -> true
      else -> error("Unhandled $this")
    }
