/*
 * Copyright 2024 Google Inc.
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
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.invokeFun
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Rewrites calls to KFunctionN.invoke() as the equivalent FunctionN.invoke().
 *
 * KFunction{N} is a fictitious type that doesn't exist at runtime, instead it's either intended to
 * be invoked as a Function{N}, or used reflectively via the real KFunction type, and it's the
 * responsibility of the compiler to do this transformation.
 *
 * See:
 * https://github.com/JetBrains/kotlin/blob/master/spec-docs/function-types.md#how-this-will-help-reflection
 */
internal class RewriteKFunctionInvokeLowering(private val context: JvmBackendContext) :
  IrElementTransformerVoid(), BodyLoweringPass {
  override fun lower(irBody: IrBody, container: IrDeclaration) {
    irBody.transformChildrenVoid()
  }

  override fun visitCall(expression: IrCall): IrExpression {
    if (
      expression.origin != IrStatementOrigin.INVOKE ||
        expression.dispatchReceiver?.type?.isKFunction() != true
    ) {
      return super.visitCall(expression)
    }
    // Get the underlying Function reference. If it's already just an implicit cast from FunctionN
    // to KFunctionN, elide the cast.
    val receiver =
      expression.dispatchReceiver?.let {
        if (it.isCastFromFunctionToKFunction()) (it as IrTypeOperatorCall).argument else it
      } ?: return super.visitCall(expression)

    // Lookup the corresponding FunctionN type by using the count of type arguments.
    val arity = (receiver.type as IrSimpleType).arguments.size - 1
    check(arity >= 0) { "KFunctionN should have N >= 0, but found: $arity" }
    val functionNSymbol = context.ir.symbols.getFunction(arity)
    val invokeSymbol = functionNSymbol.owner.invokeFun!!.symbol

    // Rewrite the call as a invoke on FunctionN rather than KFunctionN.
    return context.createJvmIrBuilder(invokeSymbol, expression).run {
      irCall(expression, invokeSymbol).apply { dispatchReceiver = receiver }
    }
  }
}

private fun IrExpression.isCastFromFunctionToKFunction(): Boolean {
  return this is IrTypeOperatorCall &&
    operator == IrTypeOperator.IMPLICIT_CAST &&
    typeOperand.isKFunction() &&
    argument.type.isFunction()
}
