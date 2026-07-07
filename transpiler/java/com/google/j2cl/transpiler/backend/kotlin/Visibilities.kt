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

import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.TypeDeclaration
import java.lang.Boolean.getBoolean

/** Whether to emit actual visibility, except for those in [EXCLUDED_VISIBILITY_PACKAGES]. */
val emitActualVisibility: Boolean =
  getBoolean("com.google.j2cl.transpiler.backend.kotlin.emitActualVisibility")

/** Set containing packages for which J2KT should translate actual visibility. */
private val ACTUAL_VISIBILITY_PACKAGES: Set<String> =
  setOf(
    "jsconstructor",
    "j2kt",
    "j2ktiosinterop",
    "j2ktjvminterop",
    "j2ktnotpassing",
    "j2ktnotpassing.nullmarked",
    "j2ktobjcweak",
  )

/** List containing package prefixes for which J2KT should translate actual visibility. */
// Keep this list small as lookup time is linear.
private val ACTUAL_VISIBILITY_PACKAGE_PREFIXES: List<String> =
  listOf(
    "java.",
    "javaemul.",
    "javax.",
  )

/**
 * Set containing packages for which J2KT should not translate actual visibility. This list is used
 * to override the ACTUAL_VISIBILITY_PACKAGE_PREFIXES list.
 */
private val EXCLUDED_VISIBILITY_PACKAGES: Set<String> =
  setOf(

  )

/** List containing package prefixes for which J2KT should not translate actual visibility. */
// Keep this list small as lookup time is linear.
private val EXCLUDE_VISIBILITY_PACKAGE_PREFIXES: List<String> =
  listOf(
  )

private val TypeDeclaration.useActualKtVisibilityForPackage: Boolean
  get() = packageName.let { packageName ->
    packageName in ACTUAL_VISIBILITY_PACKAGES ||
      packageName.plus(".").let { packageNamePlusDot ->
        (emitActualVisibility ||
          ACTUAL_VISIBILITY_PACKAGE_PREFIXES.any { packageNamePlusDot.startsWith(it) }) &&
          packageName !in EXCLUDED_VISIBILITY_PACKAGES &&
          EXCLUDE_VISIBILITY_PACKAGE_PREFIXES.none { packageNamePlusDot.startsWith(it) }
      }
  }

/** Returns whether J2KT should translate actual visibility for the given type declaration. */
// TODO(b/206898384): Remove this once the bug is fixed.
internal val TypeDeclaration.useActualKtVisibility: Boolean
  get() =
    useActualKtVisibilityForPackage &&
      (!isAutoConverter || hasAutoValueOrBuilderSuperType) &&
      !isVisibilityWarningSuppressed

internal val MemberDescriptor.useActualKtVisibility: Boolean
  get() =
    enclosingTypeDescriptor.typeDeclaration.useActualKtVisibility && !isVisibilityWarningSuppressed
