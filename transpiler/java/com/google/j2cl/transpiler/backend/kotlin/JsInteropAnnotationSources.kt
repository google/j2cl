/*
 * Copyright 2023 Google Inc.
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

import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.JsMemberType
import com.google.j2cl.transpiler.ast.JsUtils
import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.Visibility
import com.google.j2cl.transpiler.backend.kotlin.AnnotationSources.Companion.annotationTargetSource
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotation
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotationName
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.assignment
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.literal
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyIf
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyUnless
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

/**
 * Js interop annotation sources.
 *
 * @property nameSources underlying name sources
 */
internal data class JsInteropAnnotationSources(val nameSources: NameSources) {

  private val environment: Environment
    get() = nameSources.environment

  fun jsInteropAnnotationsSource(typeDeclaration: TypeDeclaration): Source =
    jsFunctionAnnotationSource(typeDeclaration)
      .ifEmpty { jsTypeAnnotationSource(typeDeclaration) }
      .ifEmpty { jsEnumAnnotationSource(typeDeclaration) }

  fun jsInteropAnnotationsSource(memberDescriptor: MemberDescriptor): Source =
    when (memberDescriptor) {
      is FieldDescriptor -> jsInteropAnnotationsSource(memberDescriptor)
      is MethodDescriptor -> jsInteropAnnotationsSource(memberDescriptor)
      else -> Source.EMPTY
    }

  fun jsInteropAnnotationsSource(fieldDescriptor: FieldDescriptor): Source =
    jsMemberAnnotationSource(fieldDescriptor)
      .ifEmpty { jsIgnoreAnnotationSource(fieldDescriptor) }
      .ifEmpty { jsOverlayAnnotationSource(fieldDescriptor) }

  fun jsInteropAnnotationsSource(methodDescriptor: MethodDescriptor): Source =
    newLineSeparated(
      jsAsyncAnnotationSource(methodDescriptor),
      jsMemberAnnotationSource(methodDescriptor)
        .ifEmpty { jsIgnoreAnnotationSource(methodDescriptor) }
        .ifEmpty { jsOverlayAnnotationSource(methodDescriptor) },
    )

  fun jsInteropAnnotationsSource(parameterDescriptor: ParameterDescriptor): Source =
    emptyUnless(parameterDescriptor.isJsOptional) {
      annotation(nameSources.topLevelQualifiedNameSource("jsinterop.annotations.JsOptional"))
    }

  private fun jsIgnoreAnnotationSource(memberDescriptor: MemberDescriptor): Source =
    emptyUnless(
      memberDescriptor.hasJsIgnoreAnnotation ||
        memberDescriptor.needsJsIgnoreAnnotationForVisibilityMismatch
    ) {
      annotation(nameSources.topLevelQualifiedNameSource("jsinterop.annotations.JsIgnore"))
    }

  private fun jsOverlayAnnotationSource(memberDescriptor: MemberDescriptor): Source =
    emptyUnless(memberDescriptor.isJsOverlay) {
      annotation(nameSources.topLevelQualifiedNameSource("jsinterop.annotations.JsOverlay"))
    }

  private fun jsAsyncAnnotationSource(methodDescriptor: MethodDescriptor): Source =
    emptyUnless(methodDescriptor.isJsAsync) {
      annotation(nameSources.topLevelQualifiedNameSource("jsinterop.annotations.JsAsync"))
    }

  /**
   * Include the `annotationQualifiedName` annotation if the member had an annotation in the source
   * or if it requires one to restore its jsname.
   */
  private fun jsMemberAnnotationSource(memberDescriptor: MemberDescriptor): Source =
    emptyUnless(hasJsInteropAnnotation(memberDescriptor)) {
      annotation(
        annotationName(
          annotationTargetSource(memberDescriptor),
          nameSources.topLevelQualifiedNameSource(
            memberDescriptor.declarationJsInfo.jsMemberType.annotationName()
          ),
        ),
        nameParameterSource(jsAnnotationNameParameterValue(memberDescriptor)),
        namespaceParameterSource(memberDescriptor.declarationJsInfo.jsNamespace),
      )
    }

  private fun JsMemberType.annotationName(): String =
    when (this) {
      JsMemberType.CONSTRUCTOR -> "jsinterop.annotations.JsConstructor"
      JsMemberType.METHOD -> "jsinterop.annotations.JsMethod"
      JsMemberType.PROPERTY,
      JsMemberType.GETTER,
      JsMemberType.SETTER -> "jsinterop.annotations.JsProperty"
      else -> throw IllegalStateException("Unexpected JsMemberType: ${this}")
    }

  private fun hasJsInteropAnnotation(memberDescriptor: MemberDescriptor): Boolean =
    memberDescriptor.declarationJsInfo.hasJsMemberAnnotation ||
      // If the name is mangled but it overrides a member (which means that one was already
      // mangled) then the annotation is already emitted in the overridden member.
      (memberDescriptor.isJsMember &&
        environment.isKtNameMangled(memberDescriptor) &&
        (memberDescriptor !is MethodDescriptor || !memberDescriptor.isJavaOverride))

  private fun jsAnnotationNameParameterValue(memberDescriptor: MemberDescriptor): String? =
    memberDescriptor.declarationJsInfo.jsName
      // if there is no name specified in the original annotation but the name is mangled in
      // Kotlin, use the simpleJsName otherwise do not emit any name.
      ?: memberDescriptor.simpleJsName.takeIf { environment.isKtNameMangled(memberDescriptor) }

  private fun jsFunctionAnnotationSource(typeDeclaration: TypeDeclaration): Source =
    emptyUnless(typeDeclaration.isJsFunctionInterface) {
      annotation(nameSources.topLevelQualifiedNameSource("jsinterop.annotations.JsFunction"))
    }

  private fun jsEnumAnnotationSource(typeDeclaration: TypeDeclaration): Source =
    emptyUnless(typeDeclaration.isJsEnum) {
      annotation(
        nameSources.topLevelQualifiedNameSource("jsinterop.annotations.JsEnum"),
        nameParameterSource(typeDeclaration),
        namespaceParameterSource(typeDeclaration),
        isNativeParameterSource(typeDeclaration.isNative),
        booleanParameterSource(
          "hasCustomValue",
          checkNotNull(typeDeclaration.jsEnumInfo).hasCustomValue(),
          false,
        ),
      )
    }

  private fun jsTypeAnnotationSource(typeDeclaration: TypeDeclaration): Source =
    emptyUnless(typeDeclaration.isJsType) {
      annotation(
        nameSources.topLevelQualifiedNameSource("jsinterop.annotations.JsType"),
        nameParameterSource(typeDeclaration),
        namespaceParameterSource(typeDeclaration),
        isNativeParameterSource(typeDeclaration.isNative),
      )
    }

  private fun namespaceParameterSource(typeDeclaration: TypeDeclaration): Source =
    emptyUnless(typeDeclaration.hasCustomizedJsNamespace()) {
      namespaceParameterSource(typeDeclaration.jsNamespace)
    }

  private fun namespaceParameterSource(namespace: String?): Source =
    namespace?.let { assignment(source("namespace"), namespaceSource(it)) }.orEmpty()

  private fun namespaceSource(namespace: String): Source =
    if (JsUtils.isGlobal(namespace)) {
      globalNamespaceSource()
    } else {
      literal(namespace)
    }

  private fun globalNamespaceSource(): Source =
    dotSeparated(
      nameSources.topLevelQualifiedNameSource("jsinterop.annotations.JsPackage"),
      identifierSource("GLOBAL"),
    )

  private val MemberDescriptor.hasJsIgnoreAnnotation
    get() = hasAnnotation("jsinterop.annotations.JsIgnore")

  private val MemberDescriptor.needsJsIgnoreAnnotationForVisibilityMismatch
    get() =
      // If we're using relaxed visibility semantics we may need to add JsIgnore annotation if a
      // member was promoted to be public but the original member was _not_ a JsMember.
      // TODO(b/b/206898384): we can remove this when we always bring over original visibilities.
      enclosingTypeDescriptor.isJsType &&
        !enclosingTypeDescriptor.isNative &&
        environment.ktVisibility(this).isPublic &&
        visibility != Visibility.PUBLIC &&
        declarationJsInfo.jsMemberType == JsMemberType.NONE

  companion object {
    private fun nameParameterSource(typeDeclaration: TypeDeclaration): Source =
      emptyIf(typeDeclaration.simpleJsName == typeDeclaration.simpleSourceName) {
        nameParameterSource(typeDeclaration.simpleJsName)
      }

    private fun nameParameterSource(value: String?): Source =
      value?.let { assignment(source("name"), literal(it)) }.orEmpty()

    private fun isNativeParameterSource(value: Boolean): Source =
      booleanParameterSource("isNative", value, false)

    private fun booleanParameterSource(
      name: String,
      value: Boolean,
      defaultValue: Boolean,
    ): Source = emptyIf(value == defaultValue) { assignment(source(name), literal(value)) }
  }
}
