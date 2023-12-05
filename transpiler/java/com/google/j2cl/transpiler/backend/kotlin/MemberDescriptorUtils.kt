/*
 * Copyright 2023 Google Inc.
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

import com.google.j2cl.transpiler.ast.AstUtils
import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor

internal val MemberDescriptor.isKtOverride: Boolean
  get() =
    when (this) {
      is MethodDescriptor ->
        isJavaOverride &&
          !directlyOverridesJavaObjectClone &&
          (javaOverriddenMethodDescriptors.any { it.enclosingTypeDescriptor.isInterface } ||
            !AstUtils.needsVisibilityBridge(this))
      else -> false
    }

private val MethodDescriptor.directlyOverridesJavaObjectClone: Boolean
  get() =
    signature == "clone()" &&
      !enclosingTypeDescriptor.isSubtypeOf(typeDescriptors.javaLangCloneable) &&
      javaOverriddenMethodDescriptors.all {
        it.declarationDescriptor.isMemberOf(typeDescriptors.javaLangObject)
      }
