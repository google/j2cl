/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.InitializersCleanupLowering
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor

/** Removes the initializers lowered by [J2clInitializersLowering]. */
internal class J2clInitializersCleanupLowering(
  context: CommonBackendContext,
  shouldEraseFieldInitializer: (IrField) -> Boolean,
) : InitializersCleanupLowering(context, shouldEraseFieldInitializer) {
  override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
    if (
      declaration is IrAnonymousInitializer &&
        !declaration.isStatic &&
        declaration.parentAsClass.primaryConstructor == null
    ) {
      // Keep the init blocks of classes without primary constructor for J2CL.
      return null
    }
    return super.transformFlat(declaration)
  }
}
