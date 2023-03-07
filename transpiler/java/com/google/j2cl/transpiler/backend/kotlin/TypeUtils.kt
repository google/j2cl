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

import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDescriptor

internal val Type.declaredSuperTypeDescriptors: List<TypeDescriptor>
  get() = listOfNotNull(superTypeDescriptor).plus(superInterfaceTypeDescriptors)

internal val Type.hasConstructors: Boolean
  get() = constructors.isNotEmpty()

// Returns the constructor to render as primary in Kotlin.
internal val Type.ktPrimaryConstructor: Method?
  get() =
    constructors.singleOrNull()?.takeIf {
      // Render primary constructors for inner classes only, where it's necessary.
      // Don't do it all classes, because Kotlin does not allow using `return` inside `init {}`.
      it.descriptor.enclosingTypeDescriptor.typeDeclaration.isKtInner
    }
