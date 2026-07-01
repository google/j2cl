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

import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.backend.kotlin.ast.Visibility as KtVisibility
import com.google.j2cl.transpiler.backend.kotlin.ast.narrowDown
import com.google.j2cl.transpiler.backend.kotlin.common.runIfNotNull

// TODO(b/216796920): Remove when the bug is fixed.
internal val TypeDeclaration.directlyDeclaredTypeParameterDescriptors: List<TypeVariable>
  get() = typeParameterDescriptors.take(directlyDeclaredTypeParameterCount)

// TODO(b/216796920): Remove when the bug is fixed.
internal val TypeDeclaration.directlyDeclaredTypeParameterCount: Int
  get() {
    val enclosingInstanceTypeParameterCount =
      enclosingTypeDeclaration
        ?.takeIf { isCapturingEnclosingInstance }
        ?.typeParameterDescriptors
        ?.size ?: 0

    val enclosingMethodTypeParameterCount =
      enclosingMethodDescriptor?.typeParameterTypeDescriptors?.size ?: 0

    return typeParameterDescriptors.size
      .minus(enclosingInstanceTypeParameterCount)
      .minus(enclosingMethodTypeParameterCount)
  }

internal val TypeDeclaration.canBeNullableAsBound: Boolean
  get() =
    !hasRecursiveTypeBounds() ||
      typeParameterDescriptors.all { it.upperBoundTypeDescriptor.isNullable }

internal val TypeDeclaration.isKtInner: Boolean
  get() =
    enclosingTypeDeclaration != null &&
      kind == TypeDeclaration.Kind.CLASS &&
      isCapturingEnclosingInstance &&
      !isLocal

internal val TypeDeclaration.isOpen: Boolean
  get() = !isFinal && !isAnonymous

/**
 * Returns whether the described type is a test class, i.e. has the JUnit `@RunWith` annotation or
 * `@RunParameterized` annotation.
 */
internal val TypeDeclaration.isTestClass: Boolean
  get() =
    hasAnnotation("org.junit.runner.RunWith") ||
      hasAnnotation("com.google.apps.xplat.testing.parameterized.RunParameterized")

internal val TypeDeclaration.defaultKtVisibility: KtVisibility
  get() = KtVisibility.PUBLIC

internal val TypeDeclaration.ktVisibility: KtVisibility
  get() =
    when {
      hasJ2ktPublicAnnotation -> KtVisibility.PUBLIC
      // TODO(b/483489173): Remove when visibility problem in Dagger-generated Factory classes is
      // solved differently.
      hasInjectAnnotatedMethod -> KtVisibility.PUBLIC
      !useActualKtVisibility -> KtVisibility.PUBLIC
      else -> KtVisibility.from(visibility)
    }

internal val TypeDeclaration.inferredKtVisibility: KtVisibility
  get() =
    ktVisibility.runIfNotNull(enclosingTypeDeclaration) {
      ktVisibility.narrowDown(it.inferredKtVisibility)
    }

internal val TypeDeclaration.declaredKtVisibility: KtVisibility?
  get() = ktVisibility.takeIf { !isAnonymous && !isLocal && it != defaultKtVisibility }

internal fun TypeDeclaration.equalsOrEnclosedIn(other: TypeDeclaration): Boolean =
  this == other || enclosingTypeDeclaration?.equalsOrEnclosedIn(other) ?: false

internal fun TypeDeclaration.isFromJRE(): Boolean =
  packageName.startsWith("javaemul.") ||
    packageName.startsWith("java.") ||
    packageName.startsWith("javax.") ||
    packageName.startsWith("kotlin.")

private val AUTO_CONVERTER_PREFIXES: List<String> = listOf("AutoConverter_", "AutoEnumConverter_")

internal val TypeDeclaration.isAutoConverter: Boolean
  get() = classComponents.any { classComponent ->
    AUTO_CONVERTER_PREFIXES.any { classComponent.startsWith(it) }
  }

internal val TypeDeclaration.isAutoValueOrBuilder: Boolean
  get() =
    hasAnnotation("com.google.auto.value.AutoValue") ||
      hasAnnotation("com.google.auto.value.AutoValue.Builder")

internal val TypeDeclaration.hasAutoValueOrBuilderSuperType: Boolean
  get() =
    toDescriptor().superTypesStream.map { it.typeDeclaration }.anyMatch { it.isAutoValueOrBuilder }

internal val TypeDeclaration.isEnumWithNonEmptyValues: Boolean
  get() = isEnum && declaredFieldDescriptors.any { it.isEnumConstant }

internal val TypeDeclaration.isKtNative: Boolean
  get() =
    hasAnnotation("javaemul.internal.annotations.KtNative") ||
      hasAnnotation("com.google.j2kt.annotations.KtNative")

internal val TypeDeclaration.ktNativeQualifiedName: String?
  get() = getAnnotation("javaemul.internal.annotations.KtNative")?.getStringValue("name")

internal val TypeDeclaration.ktBridgeQualifiedName: String?
  get() = getAnnotation("javaemul.internal.annotations.KtNative")?.getStringValue("bridgeName")

internal val TypeDeclaration.ktCompanionQualifiedName: String?
  get() = getAnnotation("javaemul.internal.annotations.KtNative")?.getStringValue("companionName")

internal val TypeDeclaration.ktMutableQualifiedName: String?
  get() = getAnnotation("javaemul.internal.annotations.KtNative")?.getStringValue("mutableName")

internal val TypeDeclaration.hasInjectAnnotatedMethod: Boolean
  get() = declaredMethodDescriptors.any { it.hasInjectAnnotation }

internal val TypeDeclaration.hasJ2ktPublicAnnotation: Boolean
  get() = hasAnnotation("com.google.common.annotations.J2ktPublic")
