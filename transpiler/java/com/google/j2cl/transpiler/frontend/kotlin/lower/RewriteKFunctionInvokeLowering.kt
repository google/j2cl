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

import com.google.j2cl.transpiler.frontend.kotlin.ir.isKFunctionOrKSuspendFunction
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.ir.util.isKSuspendFunction
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Rewrites calls to K(Suspend)FunctionN.invoke() as the equivalent (Suspend)FunctionN.invoke().
 *
 * KFunction{N} and KSuspendFunction{N} are fictitious types that don't exist at runtime, instead
 * it's either intended to be invoked as a Function{N} and SuspendFunction{N}, or used reflectively
 * via the real KFunction type, and it's the responsibility of the compiler to do this
 * transformation.
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
    val callee = expression.symbol.owner
    val dispatchReceiver = expression.dispatchReceiver
    if (
      callee.name != OperatorNameConventions.INVOKE ||
        dispatchReceiver == null ||
        !dispatchReceiver.type.isKFunctionOrKSuspendFunction()
    ) {
      return super.visitCall(expression)
    }

    // Get the underlying Function reference. If it's already just an implicit cast from
    // (Suspend)FunctionN to K(Suspend)FunctionN, elide the cast.
    val receiver =
      if (dispatchReceiver.isCastFromFunctionToKFunction())
        (dispatchReceiver as IrTypeOperatorCall).argument
      else dispatchReceiver

    // Lookup the corresponding (Suspend)FunctionN type by using the count of type arguments.
    val arity = (receiver.type as IrSimpleType).arguments.size - 1
    check(arity >= 0) { "K(Suspend)FunctionN should have N >= 0, but found: $arity" }

    // The single overridden function of `K(Suspend)FunctionN.invoke` must be
    // `(Suspend)Function{n}.invoke`.
    val invokeSymbol = callee.overriddenSymbols.single()

    // Rewrite the call as a invoke on FunctionN rather than KFunctionN.
    return context.createJvmIrBuilder(invokeSymbol, expression).run {
      irCall(expression, invokeSymbol).apply { this.dispatchReceiver = receiver }
    }
  }
}

private fun IrExpression.isCastFromFunctionToKFunction(): Boolean {
  if (this !is IrTypeOperatorCall || operator != IrTypeOperator.IMPLICIT_CAST) {
    return false
  }
  return when {
    typeOperand.isKFunction() -> argument.type.isFunction()
    typeOperand.isKSuspendFunction() -> argument.type.isSuspendFunction()
    else -> false
  }
}
