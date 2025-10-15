/*
 * Copyright 2025w Google Inc.
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
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * This pass transforms calls to the `kotlin.coroutines.coroutineContext` getter into an access to
 * the `context` property of the current `Continuation`. The current `Continuation` is obtained via
 * a call to the `getContinuationSymbol` intrinsic.
 */
internal class CoroutineContextGetterLowering(private val context: J2clBackendContext) :
  FileLoweringPass, IrElementTransformerVoidWithContext() {
  @OptIn(InternalSymbolFinderAPI::class)
  private val continuationContextGetterSymbol: IrSimpleFunctionSymbol by lazy {
    context.irBuiltIns.symbolFinder
      .findProperties(CallableId(StandardClassIds.Continuation, Name.identifier("context")))
      .single()
      .owner
      .getter!!
      .symbol
  }

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid()
  }

  override fun visitCall(expression: IrCall): IrExpression {
    expression.transformChildrenVoid()

    if (!expression.isCoroutineContextGetterCall()) {
      return expression
    }

    val irBuilder = context.jvmBackendContext.createJvmIrBuilder(currentScope!!, expression)

    return irBuilder.irCall(continuationContextGetterSymbol).apply {
      dispatchReceiver =
        irBuilder.irCall(context.intrinsics.getContinuationSymbol).apply {
          typeArguments[0] = context.irBuiltIns.anyNType
        }
    }
  }

  private fun IrCall.isCoroutineContextGetterCall(): Boolean =
    context.intrinsics.isCoroutineContextGetterCall(symbol)
}
