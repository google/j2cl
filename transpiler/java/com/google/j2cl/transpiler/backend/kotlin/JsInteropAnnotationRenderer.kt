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
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.emptySource
import com.google.j2cl.transpiler.backend.kotlin.source.ifNotNullSource
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.sourceIf

internal fun Renderer.jsInteropAnnotationsSource(fieldDescriptor: FieldDescriptor): Source =
  atMostOneSource(
    jsPropertyAnnotationSource(fieldDescriptor),
    jsIgnoreAnnotationSource(fieldDescriptor),
    jsOverlayAnnotationSource(fieldDescriptor)
  )

internal fun Renderer.jsInteropAnnotationsSource(methodDescriptor: MethodDescriptor): Source =
  atMostOneSource(
    jsPropertyAnnotationSource(methodDescriptor),
    jsMethodAnnotationSource(methodDescriptor),
    jsConstructorAnnotationSource(methodDescriptor),
    jsIgnoreAnnotationSource(methodDescriptor),
    jsOverlayAnnotationSource(methodDescriptor)
  )

private fun Renderer.jsPropertyAnnotationSource(fieldDescriptor: FieldDescriptor): Source =
  sourceIf(fieldDescriptor.hasJsPropertyAnnotation) {
    jsPropertyAnnotationSource(fieldDescriptor.jsInfo.jsName, fieldDescriptor.jsInfo.jsNamespace)
  }

private fun Renderer.jsPropertyAnnotationSource(methodDescriptor: MethodDescriptor): Source =
  // Emit an annotation only if the original java method had a `JsProperty` annotation defined on
  // it.
  sourceIf(methodDescriptor.hasJsPropertyAnnotation) {
    jsPropertyAnnotationSource(
      methodDescriptor.originalJsInfo.jsName,
      methodDescriptor.originalJsInfo.jsNamespace
    )
  }

private fun Renderer.jsPropertyAnnotationSource(jsName: String?, jsNamespace: String?): Source =
  annotation(
    topLevelQualifiedNameSource("jsinterop.annotations.JsProperty"),
    nameParameterSource(jsName),
    namespaceParameterSource(jsNamespace)
  )

private fun Renderer.jsIgnoreAnnotationSource(memberDescriptor: MemberDescriptor): Source =
  sourceIf(memberDescriptor.hasJsIgnoreAnnotation) {
    annotation(topLevelQualifiedNameSource("jsinterop.annotations.JsIgnore"))
  }

private fun Renderer.jsOverlayAnnotationSource(memberDescriptor: MemberDescriptor): Source =
  sourceIf(memberDescriptor.isJsOverlay) {
    annotation(topLevelQualifiedNameSource("jsinterop.annotations.JsOverlay"))
  }

private fun Renderer.jsConstructorAnnotationSource(methodDescriptor: MethodDescriptor): Source =
  sourceIf(methodDescriptor.hasJsConstructorAnnotation) {
    annotation(topLevelQualifiedNameSource("jsinterop.annotations.JsConstructor"))
  }

private fun Renderer.jsMethodAnnotationSource(methodDescriptor: MethodDescriptor): Source =
  // Emit an annotation only if the original java method had a `JsMethod` annotation defined on it.
  sourceIf(methodDescriptor.hasJsMethodAnnotation) {
    annotation(
      topLevelQualifiedNameSource("jsinterop.annotations.JsMethod"),
      nameParameterSource(methodDescriptor.jsInfo.jsName),
      namespaceParameterSource(methodDescriptor.jsInfo.jsNamespace)
    )
  }

internal fun Renderer.jsInteropAnnotationsSource(typeDeclaration: TypeDeclaration): Source =
  atMostOneSource(
    jsFunctionAnnotationSource(typeDeclaration),
    jsTypeAnnotationSource(typeDeclaration),
    jsEnumAnnotationSource(typeDeclaration)
  )

private fun Renderer.jsFunctionAnnotationSource(typeDeclaration: TypeDeclaration): Source =
  sourceIf(typeDeclaration.isJsFunctionInterface) {
    annotation(topLevelQualifiedNameSource("jsinterop.annotations.JsFunction"))
  }

private fun Renderer.jsEnumAnnotationSource(typeDeclaration: TypeDeclaration): Source =
  sourceIf(typeDeclaration.isJsEnum) {
    annotation(
      topLevelQualifiedNameSource("jsinterop.annotations.JsEnum"),
      nameParameterSource(typeDeclaration),
      namespaceParameterSource(typeDeclaration),
      isNativeParameterSource(typeDeclaration.isNative),
      booleanParameterSource(
        "hasCustomValue",
        checkNotNull(typeDeclaration.jsEnumInfo).hasCustomValue(),
        false
      )
    )
  }

private fun Renderer.jsTypeAnnotationSource(typeDeclaration: TypeDeclaration): Source =
  sourceIf(typeDeclaration.isJsType) {
    annotation(
      topLevelQualifiedNameSource("jsinterop.annotations.JsType"),
      nameParameterSource(typeDeclaration),
      namespaceParameterSource(typeDeclaration),
      isNativeParameterSource(typeDeclaration.isNative)
    )
  }

private fun nameParameterSource(typeDeclaration: TypeDeclaration): Source =
  sourceIf(typeDeclaration.simpleJsName != typeDeclaration.simpleSourceName) {
    nameParameterSource(typeDeclaration.simpleJsName)
  }

private fun nameParameterSource(value: String?): Source =
  value.ifNotNullSource { assignment(source("name"), literalSource(it)) }

private fun Renderer.namespaceParameterSource(typeDeclaration: TypeDeclaration): Source =
  sourceIf(typeDeclaration.hasCustomizedJsNamespace()) {
    namespaceParameterSource(typeDeclaration.jsNamespace)
  }

private fun Renderer.namespaceParameterSource(value: String?): Source =
  value.ifNotNullSource { namespace ->
    val namespaceSource =
      if (JsUtils.isGlobal(namespace))
        dotSeparated(
          topLevelQualifiedNameSource("jsinterop.annotations.JsPackage"),
          identifierSource("GLOBAL")
        )
      else literalSource(namespace)
    assignment(source("namespace"), namespaceSource)
  }

private fun isNativeParameterSource(value: Boolean): Source =
  booleanParameterSource("isNative", value, false)

private fun booleanParameterSource(name: String, value: Boolean, defaultValue: Boolean): Source =
  sourceIf(value != defaultValue) { assignment(source(name), literalSource(value)) }

private val MethodDescriptor.hasJsPropertyAnnotation
  get() =
    originalJsInfo.hasJsMemberAnnotation &&
      (originalJsInfo.jsMemberType == JsMemberType.GETTER ||
        originalJsInfo.jsMemberType == JsMemberType.SETTER)

private val MethodDescriptor.hasJsMethodAnnotation
  get() = originalJsInfo.hasJsMemberAnnotation && originalJsInfo.jsMemberType == JsMemberType.METHOD

private val MethodDescriptor.hasJsConstructorAnnotation
  get() =
    originalJsInfo.hasJsMemberAnnotation && originalJsInfo.jsMemberType == JsMemberType.CONSTRUCTOR

private val MemberDescriptor.hasJsIgnoreAnnotation
  get() =
    // We need to reverse-engineering  here because JsInfo does not carry over the information about
    // JsIgnore annotation.
    // TODO(b/266614719):  Rely on annotation presence instead when JsInterop annotations are part
    //  of the J2CL ast.
    enclosingTypeDescriptor.isJsType &&
      visibility.isPublic &&
      originalJsInfo.jsMemberType == JsMemberType.NONE &&
      !isJsOverlay

private val FieldDescriptor.hasJsPropertyAnnotation
  get() = originalJsInfo.hasJsMemberAnnotation && isJsProperty

private fun atMostOneSource(source: Source, vararg sources: Source): Source {
  val nonEmptySources = listOf(source, *sources).filter { !it.isEmpty }

  return when (nonEmptySources.size) {
    0 -> emptySource
    1 -> nonEmptySources[0]
    else -> throw IllegalArgumentException("There is more than one non-empty source.")
  }
}
