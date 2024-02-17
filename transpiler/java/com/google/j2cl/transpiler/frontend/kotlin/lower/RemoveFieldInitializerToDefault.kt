/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/** Drops the field initializer when a property is initialized to its default value. */
internal class RemoveFieldInitializerToDefault : IrElementVisitorVoid, FileLoweringPass {
  override fun lower(irFile: IrFile) = irFile.acceptVoid(this)

  override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

  override fun visitField(declaration: IrField) {
    if (declaration.isInitializedToDefaultValue()) {
      declaration.initializer = null
    }
  }
}

private fun IrField.isInitializedToDefaultValue(): Boolean {
  val constant = initializer?.expression as? IrConst<*> ?: return false

  return if (type.isPrimitiveType(nullable = false)) {
    when (constant.kind) {
      IrConstKind.Boolean -> !IrConstKind.Boolean.valueOf(constant)
      IrConstKind.Char -> IrConstKind.Char.valueOf(constant).code == 0
      IrConstKind.Byte,
      IrConstKind.Short,
      IrConstKind.Int,
      IrConstKind.Long -> (constant.value as Number).toLong() == 0L
      // Must use `equals` to differentiate between +0.0 and -0.0:
      IrConstKind.Float -> IrConstKind.Float.valueOf(constant).equals(0.0f)
      IrConstKind.Double -> IrConstKind.Double.valueOf(constant).equals(0.0)
      else -> false
    }
  } else {
    constant.kind == IrConstKind.Null
  }
}
