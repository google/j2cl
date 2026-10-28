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
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.google.j2cl.transpiler.frontend.kotlin.ir.isImplicitConstructor
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Removes implicit constructors from enums. This will normalize the AST to better match the Java
 * frontend, where implicit constructors are not part of the AST and are synthesized later by a
 * normalization pass.
 *
 * Remove the <get-entries> special function from Enum classes. All references are rewritten at the
 * call site.
 */
internal class EnumClassConstructorLowering(
  @Suppress("UNUSED_PARAMETER") context: JvmBackendContext
) : FileLoweringPass, IrVisitorVoid() {
  override fun lower(irFile: IrFile) {
    visitElement(irFile)
  }

  override fun visitElement(element: IrElement) {
    return element.acceptChildrenVoid(this)
  }

  override fun visitClass(declaration: IrClass) {
    super.visitClass(declaration)

    if (declaration.isEnumClass) {
      // Remove the <get-entries> special function from Enum classes.
      declaration.declarations.removeIf {
        it is IrFunction &&
          it.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER &&
          (it.body as? IrSyntheticBody)?.kind == IrSyntheticBodyKind.ENUM_ENTRIES
      }
      // Remove implicit constructors.
      declaration.declarations.removeIf { it.isImplicitConstructor }
    }
  }
}
