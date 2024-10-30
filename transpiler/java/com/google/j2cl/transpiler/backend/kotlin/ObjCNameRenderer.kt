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
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotation
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.assignment
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.literal
import com.google.j2cl.transpiler.backend.kotlin.ast.CompanionObject
import com.google.j2cl.transpiler.backend.kotlin.ast.declaration
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

  fun objCNameAnnotationSource(name: String, exact: Boolean? = null): Source =
    annotation(
      nameRenderer.sourceWithOptInQualifiedName("kotlin.experimental.ExperimentalObjCName") {
        topLevelQualifiedNameSource("kotlin.native.ObjCName")
      },
      literal(name),
      exact?.let { parameterSource("exact", literal(it)) }.orEmpty(),
    )

  fun objCAnnotationSource(typeDeclaration: TypeDeclaration): Source =
    Source.emptyUnless(needsObjCNameAnnotation(typeDeclaration)) {
      objCNameAnnotationSource(typeDeclaration.objCName, exact = true)
    }

  fun objCAnnotationSource(companionObject: CompanionObject): Source =
    Source.emptyUnless(needsObjCNameAnnotation(companionObject)) {
      objCNameAnnotationSource(companionObject.declaration.objCName, exact = true)
    }

  fun objCAnnotationSource(
    methodDescriptor: MethodDescriptor,
    methodObjCNames: MethodObjCNames?,
  ): Source =
    Source.emptyUnless(!methodDescriptor.isConstructor) {
      methodObjCNames?.methodName?.let { objCNameAnnotationSource(it) }.orEmpty()
    }

  fun objCAnnotationSource(fieldDescriptor: FieldDescriptor): Source =
    Source.emptyUnless(needsObjCNameAnnotation(fieldDescriptor)) {
      objCNameAnnotationSource(fieldDescriptor.objCName)
    }

  private fun needsObjCNameAnnotation(typeDeclaration: TypeDeclaration): Boolean =
    environment.ktVisibility(typeDeclaration).needsObjCNameAnnotation &&
      !typeDeclaration.isLocal &&
      !typeDeclaration.isAnonymous

  private fun needsObjCNameAnnotation(companionObject: CompanionObject): Boolean =
    needsObjCNameAnnotation(companionObject.enclosingTypeDeclaration)

  private fun needsObjCNameAnnotation(method: Method): Boolean =
    method.descriptor.enclosingTypeDescriptor.typeDeclaration.let { enclosingTypeDeclaration ->
      !enclosingTypeDeclaration.isLocal &&
        !enclosingTypeDeclaration.isAnonymous &&
        environment.ktVisibility(method.descriptor).needsObjCNameAnnotation &&
        !method.isJavaOverride
    }

  private fun needsObjCNameAnnotation(fieldDescriptor: FieldDescriptor): Boolean =
    fieldDescriptor.enclosingTypeDescriptor.typeDeclaration.let { enclosingTypeDeclaration ->
      needsObjCNameAnnotation(enclosingTypeDeclaration) &&
        !enclosingTypeDeclaration.isLocal &&
        !enclosingTypeDeclaration.isAnonymous &&
        environment.ktVisibility(fieldDescriptor).needsObjCNameAnnotation
    }

  internal fun renderedObjCNames(method: Method): MethodObjCNames? =
    when {
      !needsObjCNameAnnotation(method) -> null
      else -> method.toObjCNames()
    }

  companion object {
    private fun parameterSource(name: String, valueSource: Source): Source =
      assignment(source(name), valueSource)
  }
}
