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

  private val hiddenFromObjCMapping: HiddenFromObjCMapping
    get() = environment.hiddenFromObjCMapping

  private val isJ2ObjCInteropEnabled: Boolean
    get() = nameRenderer.environment.isJ2ObjCInteropEnabled

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

  fun objCAnnotationSource(typeDeclaration: TypeDeclaration): Source =
    when {
      !isJ2ObjCInteropEnabled -> Source.EMPTY
      hiddenFromObjCMapping.contains(typeDeclaration) -> hiddenFromObjCAnnotationSource()
      needsObjCNameAnnotation(typeDeclaration) ->
        objCNameAnnotationSource(
          typeDeclaration.objCName(nameRenderer.objCNamePrefix),
          swiftName = typeDeclaration.objCNameWithoutPrefix,
          exact = true,
        )
      else -> Source.EMPTY
    }

  fun objCAnnotationSource(companionObject: CompanionObject): Source =
    Source.emptyUnless(isJ2ObjCInteropEnabled && needsObjCNameAnnotation(companionObject)) {
      objCNameAnnotationSource(
        companionObject.declaration.objCName(nameRenderer.objCNamePrefix),
        swiftName = companionObject.declaration.objCNameWithoutPrefix,
        exact = true,
      )
    }

  fun objCAnnotationSource(
    methodDescriptor: MethodDescriptor,
    methodObjCNames: MethodObjCNames?,
  ): Source =
    Source.emptyIf(
      !isJ2ObjCInteropEnabled ||
        methodDescriptor.isConstructor ||
        hiddenFromObjCMapping.contains(methodDescriptor.enclosingTypeDescriptor)
    ) {
      when {
        hiddenFromObjCMapping.contains(methodDescriptor) -> hiddenFromObjCAnnotationSource()
        else -> objCNameAnnotationSource(methodObjCNames)
      }
    }

  fun objCNameAnnotationSource(methodObjCNames: MethodObjCNames?): Source =
    methodObjCNames
      ?.objCName
      ?.let { objCNameAnnotationSource(it.string, swiftName = it.swiftString) }
      .orEmpty()

  fun objCAnnotationSource(fieldDescriptor: FieldDescriptor): Source =
    Source.emptyIf(
      !isJ2ObjCInteropEnabled ||
        hiddenFromObjCMapping.contains(fieldDescriptor.enclosingTypeDescriptor)
    ) {
      when {
        hiddenFromObjCMapping.contains(fieldDescriptor) -> hiddenFromObjCAnnotationSource()
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
    !hiddenFromObjCMapping.contains(method.descriptor) &&
      method.descriptor.enclosingTypeDescriptor.typeDeclaration.let { enclosingTypeDeclaration ->
        !enclosingTypeDeclaration.isLocal &&
          !enclosingTypeDeclaration.isAnonymous &&
          environment.ktVisibility(method.descriptor).needsObjCNameAnnotation &&
          !method.isJavaOverride &&
          method.descriptor.objectiveCName != null
      }

  private fun needsObjCNameAnnotation(fieldDescriptor: FieldDescriptor): Boolean =
    !hiddenFromObjCMapping.contains(fieldDescriptor) &&
      fieldDescriptor.enclosingTypeDescriptor.typeDeclaration.let { enclosingTypeDeclaration ->
        needsObjCNameAnnotation(enclosingTypeDeclaration, forceObjCNameAnnotation = true) &&
          environment.ktVisibility(fieldDescriptor).needsObjCNameAnnotation
      }

  internal fun renderedObjCNames(method: Method): MethodObjCNames? =
    when {
      !isJ2ObjCInteropEnabled -> null
      !needsObjCNameAnnotation(method) -> null
      else -> method.toObjCNames()
    }

  companion object {
    private fun parameterSource(name: String, valueSource: Source): Source =
      assignment(source(name), valueSource)
  }
}
