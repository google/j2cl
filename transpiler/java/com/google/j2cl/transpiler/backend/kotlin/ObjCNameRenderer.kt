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

import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotation
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.assignment
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.literal
import com.google.j2cl.transpiler.backend.kotlin.ast.CompanionObject
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

/**
 * ObjC annotation renderer.
 *
 * @property nameRenderer underlying name renderer
 */
internal class ObjCNameRenderer(val nameRenderer: NameRenderer) {

  private val environment: Environment
    get() = nameRenderer.environment

  private val isJ2ObjCInteropEnabled: Boolean
    get() = nameRenderer.environment.isJ2ObjCInteropEnabled

  fun objectiveCNameAnnotationSource(name: String): Source =
    annotation(
      nameRenderer.topLevelQualifiedNameSource("com.google.j2objc.annotations.ObjectiveCName"),
      literal(name),
    )

  fun objectiveCAnnotationSource(typeDeclaration: TypeDeclaration): Source =
    if (!isJ2ObjCInteropEnabled) {
      Source.EMPTY
    } else if (needsObjCNameAnnotation(typeDeclaration)) {
      objectiveCNameAnnotationSource(typeDeclaration.objCNameWithoutPrefix)
    } else {
      Source.EMPTY
    }

  fun hiddenFromObjCAnnotationSource(): Source =
    annotation(
      nameRenderer.sourceWithOptInQualifiedName("kotlin.experimental.ExperimentalObjCRefinement") {
        topLevelQualifiedNameSource("kotlin.native.HiddenFromObjC")
      }
    )

  fun objCNameAnnotationSource(
    name: String,
    swiftName: String? = null,
    exact: Boolean? = null,
  ): Source =
    annotation(
      nameRenderer.sourceWithOptInQualifiedName("kotlin.experimental.ExperimentalObjCName") {
        topLevelQualifiedNameSource("kotlin.native.ObjCName")
      },
      literal(name),
      swiftName?.let { parameterSource("swiftName", literal(it)) }.orEmpty(),
      exact?.let { parameterSource("exact", literal(it)) }.orEmpty(),
    )

  fun objCAnnotationSource(methodDescriptor: MethodDescriptor): Source =
    Source.emptyIf(!isJ2ObjCInteropEnabled || methodDescriptor.isConstructor) {
      when {
        isHiddenFromObjC(methodDescriptor) -> hiddenFromObjCAnnotationSource()
        else -> Source.EMPTY
      }
    }

  fun objCAnnotationSource(fieldDescriptor: FieldDescriptor): Source =
    Source.emptyIf(!isJ2ObjCInteropEnabled) {
      when {
        isHiddenFromObjC(fieldDescriptor) -> hiddenFromObjCAnnotationSource()
        needsObjCNameAnnotation(fieldDescriptor) ->
          objCNameAnnotationSource(fieldDescriptor.objCName)
        else -> Source.EMPTY
      }
    }

  private fun needsObjCNameAnnotation(
    typeDeclaration: TypeDeclaration,
    forceObjCNameAnnotation: Boolean = false,
  ): Boolean =
    environment.ktVisibility(typeDeclaration).needsObjCNameAnnotation &&
      !typeDeclaration.isLocal &&
      !typeDeclaration.isAnonymous &&
      (forceObjCNameAnnotation ||
        typeDeclaration.objectiveCName != null ||
        typeDeclaration.objectiveCNamePrefix != null)

  private fun needsObjCNameAnnotation(companionObject: CompanionObject): Boolean =
    needsObjCNameAnnotation(companionObject.enclosingTypeDeclaration)

  private fun needsObjCNameAnnotation(method: Method): Boolean =
    method.descriptor.enclosingTypeDescriptor.typeDeclaration.let { enclosingTypeDeclaration ->
      !enclosingTypeDeclaration.isLocal &&
        !enclosingTypeDeclaration.isAnonymous &&
        environment.ktVisibility(method.descriptor).needsObjCNameAnnotation &&
        !method.isJavaOverride &&
        method.descriptor.objectiveCName != null
    }

  private fun needsObjCNameAnnotation(fieldDescriptor: FieldDescriptor): Boolean =
    fieldDescriptor.enclosingTypeDescriptor.typeDeclaration.let { enclosingTypeDeclaration ->
      needsObjCNameAnnotation(enclosingTypeDeclaration, forceObjCNameAnnotation = true) &&
        environment.ktVisibility(fieldDescriptor).needsObjCNameAnnotation
    }

  private fun isHiddenFromObjC(methodDescriptor: MethodDescriptor): Boolean =
    hasHiddenFromObjCAnnotation(methodDescriptor)

  private fun isHiddenFromObjC(fieldDescriptor: FieldDescriptor): Boolean =
    hasHiddenFromObjCAnnotation(fieldDescriptor)

  companion object {
    private fun parameterSource(name: String, valueSource: Source): Source =
      assignment(source(name), valueSource)

    private fun hasHiddenFromObjCAnnotation(memberDescriptor: MemberDescriptor): Boolean =
      memberDescriptor.hasAnnotation("com.google.j2kt.annotations.HiddenFromObjC")
  }
}
