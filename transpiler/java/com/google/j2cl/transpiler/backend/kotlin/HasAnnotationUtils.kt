/*
 * Copyright 2026 Google Inc.
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

import com.google.j2cl.transpiler.ast.HasAnnotations

private val hiddenFromObjCAnnotations =
  setOf(
    "com.google.j2kt.annotations.HiddenFromObjC",
    // TODO(b/527953195): Remove when Dagger KSP is available.
    "dagger.internal.DaggerGenerated",
  )

internal val HasAnnotations.hasInjectAnnotation: Boolean
  get() = hasAnnotation("dagger.Inject") || hasAnnotation("javax.inject.Inject")

internal val HasAnnotations.hasDaggerComponentAnnotation: Boolean
  get() = hasAnnotation("dagger.Component")

internal val HasAnnotations.hasDaggerComponentRegistryCompatibleAnnotation: Boolean
  get() = hasAnnotation("com.google.apps.xplat.dagger.asynccomponent.ComponentRegistryCompatible")

internal val HasAnnotations.isVisibilityWarningSuppressed: Boolean
  get() = isWarningSuppressed("j2kt:visibility")

internal val HasAnnotations.isAnnotatedToHideFromObjC: Boolean
  get() = annotations.any {
    hiddenFromObjCAnnotations.contains(it.typeDescriptor.qualifiedSourceName) ||
      it.typeDescriptor.typeDeclaration.hasAnnotation("com.google.j2kt.annotations.HidesFromObjC")
  }
