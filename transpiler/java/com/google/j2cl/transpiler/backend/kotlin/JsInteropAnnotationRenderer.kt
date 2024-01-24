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
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotation
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.assignment
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.literal
import com.google.j2cl.transpiler.backend.kotlin.ast.Visibility as KtVisibility
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

/**
 * Js interop annotation renderer.
 *
 * @property nameRenderer underlying name renderer
 */
internal data class JsInteropAnnotationRenderer(val nameRenderer: NameRenderer) {

  fun jsInteropAnnotationsSource(typeDeclaration: TypeDeclaration): Source =
    jsFunctionAnnotationSource(typeDeclaration)
      .ifEmpty { jsTypeAnnotationSource(typeDeclaration) }
      .ifEmpty { jsEnumAnnotationSource(typeDeclaration) }

  fun jsInteropAnnotationsSource(fieldDescriptor: FieldDescriptor): Source =
    jsPropertyAnnotationSource(fieldDescriptor)
      .ifEmpty { jsIgnoreAnnotationSource(fieldDescriptor) }
      .ifEmpty { jsOverlayAnnotationSource(fieldDescriptor) }

  fun jsInteropAnnotationsSource(methodDescriptor: MethodDescriptor): Source =
    jsPropertyAnnotationSource(methodDescriptor)
      .ifEmpty { jsMethodAnnotationSource(methodDescriptor) }
      .ifEmpty { jsConstructorAnnotationSource(methodDescriptor) }
      .ifEmpty { jsIgnoreAnnotationSource(methodDescriptor) }
      .ifEmpty { jsOverlayAnnotationSource(methodDescriptor) }

  fun jsInteropAnnotationsSource(parameterDescriptor: ParameterDescriptor): Source =
    parameterDescriptor
      .takeIf { it.isJsOptional }
      ?.let {
        annotation(nameRenderer.topLevelQualifiedNameSource("jsinterop.annotations.JsOptional"))
      }
      .orEmpty()

  private fun jsPropertyAnnotationSource(memberDescriptor: MemberDescriptor): Source =
    memberDescriptor
      .takeIf { it.isJsProperty }
      ?.let { jsInteropAnnotationSource(memberDescriptor, "jsinterop.annotations.JsProperty") }
      .orEmpty()

  private fun jsIgnoreAnnotationSource(memberDescriptor: MemberDescriptor): Source =
    memberDescriptor
      .takeIf { it.hasJsIgnoreAnnotation }
      ?.let {
        annotation(nameRenderer.topLevelQualifiedNameSource("jsinterop.annotations.JsIgnore"))
      }
      .orEmpty()

  private fun jsOverlayAnnotationSource(memberDescriptor: MemberDescriptor): Source =
    memberDescriptor
      .takeIf { it.isJsOverlay }
      ?.let {
        annotation(nameRenderer.topLevelQualifiedNameSource("jsinterop.annotations.JsOverlay"))
      }
      .orEmpty()

  private fun jsConstructorAnnotationSource(methodDescriptor: MethodDescriptor): Source =
    methodDescriptor
      .takeIf { it.hasJsConstructorAnnotation }
      ?.let {
        annotation(nameRenderer.topLevelQualifiedNameSource("jsinterop.annotations.JsConstructor"))
      }
      .orEmpty()

  private fun jsMethodAnnotationSource(methodDescriptor: MethodDescriptor): Source =
    methodDescriptor
      .takeIf { it.isJsMethod }
      ?.let { jsInteropAnnotationSource(methodDescriptor, "jsinterop.annotations.JsMethod") }
      .orEmpty()

  /**
   * Render the `annotationQualifiedName` annotation if the member had an annotation in the source
   * or if it requires one to restore its jsname.
   */
  private fun jsInteropAnnotationSource(
    memberDescriptor: MemberDescriptor,
    annotationQualifiedName: String,
  ): Source =
    memberDescriptor
      .takeIf {
        it.originalJsInfo.hasJsMemberAnnotation ||
          // If the name is mangled but it overrides a member (which means that one was already
          // mangled) then the annotation is already emitted in the overridden member.
          (it.isKtNameMangled && !it.isKtOverride)
      }
      ?.let {
        val nameParameterValue =
          it.originalJsInfo.jsName
            // if there is no name specified in the original annotation but the name is mangled in
            // Kotlin, use the simpleJsName otherwise do not emit any name.
            ?: if (it.isKtNameMangled) it.simpleJsName else null

        annotation(
          nameRenderer.topLevelQualifiedNameSource(annotationQualifiedName),
          nameParameterSource(nameParameterValue),
          namespaceParameterSource(it.originalJsInfo.jsNamespace),
        )
      }
      .orEmpty()

  private fun jsFunctionAnnotationSource(typeDeclaration: TypeDeclaration): Source =
    typeDeclaration
      .takeIf { it.isJsFunctionInterface }
      ?.let {
        annotation(nameRenderer.topLevelQualifiedNameSource("jsinterop.annotations.JsFunction"))
      }
      .orEmpty()

  private fun jsEnumAnnotationSource(typeDeclaration: TypeDeclaration): Source =
    typeDeclaration
      .takeIf { it.isJsEnum }
      ?.let {
        annotation(
          nameRenderer.topLevelQualifiedNameSource("jsinterop.annotations.JsEnum"),
          nameParameterSource(it),
          namespaceParameterSource(it),
          isNativeParameterSource(it.isNative),
          booleanParameterSource(
            "hasCustomValue",
            checkNotNull(it.jsEnumInfo).hasCustomValue(),
            false,
          ),
        )
      }
      .orEmpty()

  private fun jsTypeAnnotationSource(typeDeclaration: TypeDeclaration): Source =
    typeDeclaration
      .takeIf { it.isJsType }
      ?.let {
        annotation(
          nameRenderer.topLevelQualifiedNameSource("jsinterop.annotations.JsType"),
          nameParameterSource(it),
          namespaceParameterSource(it),
          isNativeParameterSource(it.isNative),
        )
      }
      .orEmpty()

  private fun namespaceParameterSource(typeDeclaration: TypeDeclaration): Source =
    typeDeclaration
      .takeIf { it.hasCustomizedJsNamespace() }
      ?.let { namespaceParameterSource(it.jsNamespace) }
      .orEmpty()

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
      nameRenderer.topLevelQualifiedNameSource("jsinterop.annotations.JsPackage"),
      identifierSource("GLOBAL"),
    )

  companion object {
    private fun nameParameterSource(typeDeclaration: TypeDeclaration): Source =
      typeDeclaration
        .takeIf { it.simpleJsName != it.simpleSourceName }
        ?.let { nameParameterSource(it.simpleJsName) }
        .orEmpty()

    private fun nameParameterSource(value: String?): Source =
      value?.let { assignment(source("name"), literal(it)) }.orEmpty()

    private fun isNativeParameterSource(value: Boolean): Source =
      booleanParameterSource("isNative", value, false)

    private fun booleanParameterSource(
      name: String,
      value: Boolean,
      defaultValue: Boolean,
    ): Source =
      value.takeIf { it != defaultValue }?.let { assignment(source(name), literal(it)) }.orEmpty()

    private val MethodDescriptor.hasJsConstructorAnnotation
      get() = originalJsInfo.hasJsMemberAnnotation && isJsConstructor

    private val MemberDescriptor.hasJsIgnoreAnnotation
      get() =
        // We need to reverse-engineering  here because JsInfo does not carry over the information
        // about JsIgnore annotation.
        // TODO(b/266614719): Rely on annotation presence instead when JsInterop annotations are
        // part  of the J2CL ast.
        enclosingTypeDescriptor.isJsType &&
          !enclosingTypeDescriptor.isNative &&
          ktVisibility == KtVisibility.PUBLIC &&
          originalJsInfo.jsMemberType == JsMemberType.NONE

    private val MemberDescriptor.isKtNameMangled: Boolean
      get() = name != ktMangledName
  }
}
