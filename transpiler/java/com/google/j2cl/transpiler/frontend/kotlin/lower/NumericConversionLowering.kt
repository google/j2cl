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

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getPrimitiveType
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.expressions.OperatorConventions

/**
 * Rewrites numeric conversion calls (toInt(), toShort(), etc) to be simple casts instead.
 *
 * The J2CL AST already understands widening/narrowing casts without having to box and call the
 * relevant conversion methods. By lowering to a simple cast we can leverage this existing behavior.
 *
 * Note: this should run after `ConstEvaluationLowering` in order to not interfere with its
 * expectation that the calls exist. It should also run after `TypeOperatorLowering` so that the
 * cast doesn't get rewritten.
 */
internal class NumericConversionLowering(private val context: JvmBackendContext) :
  FileLoweringPass, IrElementTransformerVoidWithContext() {

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid(this)
  }

  override fun visitCall(expression: IrCall): IrExpression {
    if (!expression.isNumericConversion()) return super.visitCall(expression)
    return visitExpression(
      context.createJvmIrBuilder(currentScope!!).run {
        irAs(expression.dispatchReceiver!!, expression.symbol.owner.returnType)
      }
    )
  }

  private fun IrCall.isNumericConversion(): Boolean {
    val fnName = symbol.owner.name
    if (fnName !in OperatorConventions.NUMBER_CONVERSIONS) return false
    if (dispatchReceiver?.type?.getPrimitiveType() == null) return false
    // Just confirm that we're actually calling the function defined on the primitive type.
    check(dispatchReceiver!!.type.classOrNull?.functionByName(fnName.asString()) == symbol)
    return true
  }
}
