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

import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.backend.kotlin.ast.Member as KtMember
import com.google.j2cl.transpiler.backend.kotlin.ast.Visibility
import com.google.j2cl.transpiler.backend.kotlin.ast.toCompanionObjectOrNull

/** Returns a list of type descriptors declared on this type. */
internal val Type.declaredSuperTypeDescriptors: List<TypeDescriptor>
  get() = listOfNotNull(superTypeDescriptor).plus(superInterfaceTypeDescriptors)

/** Returns whether this type has any constructors. */
internal val Type.hasConstructors: Boolean
  get() = constructors.isNotEmpty()

/** Returns the constructor to render as primary in Kotlin. */
internal val Type.ktPrimaryConstructor: Method?
  get() =
    constructors.singleOrNull()?.takeIf {
      // Render primary constructors for inner classes only, where it's necessary.
      // Don't do it all classes, because Kotlin does not allow using `return` inside `init {}`.
      // It's also necessary because of: https://youtrack.jetbrains.com/issue/KT-65299
      // TODO(b/322331738): Remove special handling of primary constructors when the bug is fixed.
      it.descriptor.enclosingTypeDescriptor.typeDeclaration.isKtInner
    }

/** Returns a list of Kotlin members inside this Java type. */
internal val Type.ktMembers: List<KtMember>
  get() =
    members
      .asSequence()
      .filter { !it.isStatic && (!declaration.isAnonymous || !it.isConstructor) }
      .map { KtMember.WithJavaMember(it) }
      .plus(toCompanionObjectOrNull()?.let { KtMember.WithCompanionObject(it) })
      .plus(types.map { KtMember.WithType(it) })
      .filterNotNull()
      .toList()

// TODO(b/310160330): Remove this restriction once Kotlin allows for that:
// https://github.com/Kotlin/KEEP/blob/master/proposals/jvm-field-annotation-in-interface-companion.md#open-questions
/** Returns whether it's illegal to render [@JvmField] annotations in this type. */
internal val Type.jvmFieldsAreIllegal
  get() =
    isInterface &&
      members.filterIsInstance<Field>().let { fields ->
        fields.any { it.isCompileTimeConstant } && fields.any { !it.isCompileTimeConstant }
      }

internal val Type.needExplicitPrimaryConstructor: Boolean
  get() =
    isClass && !hasConstructors && declaration.visibility.memberKtVisibility != Visibility.PUBLIC
