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
package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody

internal class LocalClassesExtractionFromInlineFunctionsLowering(context: JvmBackendContext) :
  BodyLoweringPass {
  private val delegate =
    org.jetbrains.kotlin.backend.common.lower.inline
      .LocalClassesExtractionFromInlineFunctionsLowering(context)
  override fun lower(irBody: IrBody, container: IrDeclaration) {
    val function = container as? IrFunction ?: return
    // Back-off hoisting local classes out of functions that contain type parameters. Normally
    // kotlinc backs-off only if they're reified so that they can be resolved/replaced at the
    // call site. However since we're currently not erasing type parameters for inline functions
    // which effectively makes all inline type parameters reified.
    // TODO(b/274670726): Remove this behavior when we properly erase type parameters.
    if (!function.isInline || function.typeParameters.isNotEmpty()) return
    delegate.lower(irBody, container)
  }
}
