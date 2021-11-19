/*
 * Copyright 2021 Google Inc.
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

import com.google.j2cl.transpiler.ast.Visibility

internal val Visibility.sourceStringOrNull: String?
  get() =
    when (this) {
      Visibility.PUBLIC -> null
      Visibility.PROTECTED -> "protected"
      Visibility.PACKAGE_PRIVATE -> "internal"
      // TODO(b/206898384): For now, render Java "private" as Kotlin "internal", since in Kotlin
      // private members are not visible from other classes in the same file.
      Visibility.PRIVATE -> "internal"
    }
