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

import com.google.j2cl.transpiler.frontend.kotlin.ir.hasVoidReturn
import com.google.j2cl.transpiler.frontend.kotlin.ir.isLambda
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Adds explicit return to all Unit functions and ensures all calls to them have a Unit result.
 *
 * For example:
 * ```
 * fun foo(): Unit {}
 * val x = foo()
 * ```
 *
 * will lower to:
 * ```
 * fun foo(): Unit {
 *   return Unit
 * }
 * val x: Unit = (foo(), Unit)
 * ```
 *
 * The returns are added in anticipation for the function potentially being polymorphic and thus not
 * ultimately having a `void` return.
 *
 * The adjustment to calls is to ensure that results from functions that are rewritten to be `void`
 * are still `Unit`.
 */
internal class UnitLowering(private val context: J2clBackendContext) :
  FileLoweringPass, IrElementTransformerVoid() {

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid()
  }

  override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
    declaration.transformChildrenVoid()

    val originalBody = declaration.body

    // Add an explicit Unit return for any non-void function with a body. For lambdas we assume they
    // all have a non-void return as the functional type being implement will determine if it's void
    // or not. Those case will get handled and cleaned up later on.
    if (
      declaration.returnType.isUnit() &&
        originalBody != null &&
        (declaration.isLambda || !declaration.hasVoidReturn)
    ) {
      declaration.body =
        context
          .createIrBuilder(
            declaration.symbol,
            startOffset = originalBody.startOffset,
            endOffset = originalBody.endOffset,
          )
          .irBlockBody {
            +originalBody.statements
            +irReturnUnit()
          }
    }
    return declaration
  }

  override fun visitCall(expression: IrCall): IrExpression {
    expression.transformChildrenVoid()
    if (!expression.type.isUnit()) return expression

    return context.createIrBuilder(expression.symbol).irBlock {
      +expression
      +irUnit()
    }
  }

  override fun visitGetField(expression: IrGetField): IrExpression {
    expression.transformChildrenVoid()
    if (!expression.type.isUnit()) return expression

    return context.createIrBuilder(expression.symbol).irBlock {
      +expression
      +irUnit()
    }
  }
}
