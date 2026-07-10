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

/** Set containing packages for which J2KT should not translate actual visibility. */
private val EXCLUDED_VISIBILITY_PACKAGES: Set<String> =
  setOf(
    // Packages from excluded readable/integration tests.
    // TODO(b/206898384): Implement j2kt_emit_relaxed_visibility flag to avoid this list.
    "accidentaloverride",
    "autovalue",
    "backwardbridgemethod",
    "bridgemethods",
    "cast",
    "cyclicclinits",
    "genericanddefaultmethods",
    "genericmethod",
    "gwtincompatible",
    "innerclassinheritance",
    "innerclassinitorder",
    "instanceinnerclass",
    "j2kt.relaxedvisibility",
    "jsbridgebackward",
    "jsbridgemultipleaccidental",
    "jsbridgemultipleexposing",
    "multipleroottypes",
    "multipletopclasses",
    "qualifiedsupercall",
    "staticfieldimport.staticimports",
    "subclassgenericclass",
    "subnativejstype",
    "supercallnondefault",
    "supermethodcall",
  )

/** List containing package prefixes for which J2KT should not translate actual visibility. */
// Keep this list small as lookup time is linear.
private val EXCLUDE_VISIBILITY_PACKAGE_PREFIXES: List<String> =
  listOf(
  )

private val TypeDeclaration.useActualKtVisibilityForPackage: Boolean
  get() =
    packageName !in EXCLUDED_VISIBILITY_PACKAGES &&
      packageName.plus(".").let { packageNamePlusDot ->
        EXCLUDE_VISIBILITY_PACKAGE_PREFIXES.none { packageNamePlusDot.startsWith(it) }
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
