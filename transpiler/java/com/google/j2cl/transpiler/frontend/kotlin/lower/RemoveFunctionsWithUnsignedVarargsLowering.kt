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
package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.isUnsignedArray
import org.jetbrains.kotlin.ir.util.isVararg

/** Remove functions that contain unsigned varargs in the signature. */
// TODO(b/242573966): Remove this when we can handle unsigned vararg types.
class RemoveFunctionsWithUnsignedVarargsLowering : DeclarationTransformer {
  override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
    if (
      declaration is IrFunction &&
        declaration.valueParameters.any { it.type.isUnsignedArray() && it.isVararg }
    ) {
      return emptyList()
    }
    // 'null' means keep existing element
    return null
  }
}
