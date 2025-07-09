/*
 * Copyright 2025 Google Inc.
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
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/** Flattens extra blocks that were added from function inlining. */
internal class FlattenInlinedBlocks : FileLoweringPass, IrVisitorVoid() {
  override fun lower(irFile: IrFile) = irFile.acceptVoid(this)

  override fun visitElement(element: IrElement) {
    // Traverse children first so that we apply post-order.
    element.acceptChildrenVoid(this)

    if (element !is IrStatementContainer) return

    // Quickly check if there's anything to even inline.
    if (element.statements.none(IrElement::shouldFlatten)) return

    val originalStatements = element.statements.toList()
    element.statements.clear()

    for (statement in originalStatements) {
      if (statement.shouldFlatten()) {
        element.statements.addAll((statement as IrStatementContainer).statements)
      } else {
        element.statements.add(statement)
      }
    }
  }
}

private fun IrElement.shouldFlatten(): Boolean = this is IrBlock && isFromInlining()

private fun IrBlock.isFromInlining(): Boolean =
  this is IrInlinedFunctionBlock || origin == IrStatementOrigin.INLINE_ARGS_CONTAINER
