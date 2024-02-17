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

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Place calls that return `Nothing` calls into a synthetic block of `{ foo(); Unit }`.
 *
 * This allows [BlockDecomposerLowering] to come along later and extract this block into its own set
 * of statements. This is important as it will ensure any references to the Nothing type within an
 * expression get hoisted out into their own statements. Statements after the Nothing statement can
 * then be removed as they are unreachable.
 */
internal class KotlinNothingValueCallsLowering(private val context: JvmBackendContext) :
  BodyLoweringPass {
  override fun lower(irBody: IrBody, container: IrDeclaration) {
    irBody.transformChildrenVoid(
      object : IrElementTransformerVoid() {
        override fun visitCall(expression: IrCall): IrExpression {
          if (!expression.type.isNothing()) return super.visitCall(expression)
          return context
            .createJvmIrBuilder(container.symbol, expression.startOffset, expression.endOffset)
            .run {
              irBlock(expression, origin = null, context.irBuiltIns.nothingType) {
                +super.visitCall(expression)
                // We need to > 1 statement in the block for it to be decomposed, otherwise the
                // block will simply be unwrapped in place. It doesn't matter what the next
                // statement is as it is unreachable anyway.
                +irUnit()
              }
            }
        }
      }
    )
  }
}
