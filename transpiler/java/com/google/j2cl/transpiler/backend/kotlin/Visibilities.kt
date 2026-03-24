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

import com.google.j2cl.transpiler.ast.TypeDeclaration

/** List containing package prefixes for which J2KT should translate actual visibility. */
private val ACTUAL_VISIBILITY_PACKAGE_PREFIXES: List<String> =
  listOf(
    "java.",
    "javaemul.",
    "javax.",
    "j2kt.",
    "j2ktiosinterop.",
    "j2ktjvminterop.",
    "j2ktobjcweak.",
    // copybara:strip_begin
    // Packages which should be hidden in Open Source project.
    // copybara:strip_end
  )

/**
 * List containing package prefixes for which J2KT should not translate actual visibility. This list
 * is used to override the ACTUAL_VISIBILITY_PACKAGE_PREFIXES list.
 */
private val EXCLUDED_VISIBILITY_PACKAGE_PREFIXES: List<String> =
  listOf(
    // copybara:strip_begin
    // Packages which should be hidden in Open Source project.
    // copybara:strip_end
  )

/**
 * List containing prefixes for which J2KT should not translate actual visibility. These classes are
 * usually generated as package-private, but extended by public classes.
 */
private val AUTO_TYPE_PREFIXES: List<String> =
  listOf("AutoValue_", "AutoOneOf_", "AutoConverter_", "AutoEnumConverter_")

private val TypeDeclaration.useActualKtVisibilityForPackage: Boolean
  get() =
    packageName.plus(".").let { packageNamePrefix ->
      ACTUAL_VISIBILITY_PACKAGE_PREFIXES.any { packageNamePrefix.startsWith(it) } &&
        EXCLUDED_VISIBILITY_PACKAGE_PREFIXES.none { packageNamePrefix.startsWith(it) }
    }

private val TypeDeclaration.isAutoType: Boolean
  get() = classComponents.any { classComponent ->
    AUTO_TYPE_PREFIXES.any { classComponent.startsWith(it) }
  }

/** Returns whether J2KT should translate actual visibility for the given type declaration. */
// TODO(b/206898384): Remove this once the bug is fixed.
internal val TypeDeclaration.useActualKtVisibility: Boolean
  get() = useActualKtVisibilityForPackage && !isAutoType
