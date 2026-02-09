/*
 * Copyright 2026 Google Inc.
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
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irBreak
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irDoWhile
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irWhile
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrBreakImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Lower do...while loops that define variables in their body and use them in the condition.
 *
 * Such loops are not directly representable in JavaScript if variables are block-scoped (i.e.
 * declared with 'let'), as variables declared in the 'do' block are not visible in the 'while'
 * condition. This lowering pass transforms such loops into a structure where variables are declared
 * in an outer scope, making them accessible to the condition check.
 *
 * This transformation also ensures that 'continue' statements in the original loop behave correctly
 * by skipping the remainder of the loop body and proceeding to the condition check.
 *
 * Specifically, a loop like:
 * ```
 *   label@ do {
 *            var x = y++
 *            ...
 *            continue@label
 *            ...
 *          } while (x < 10)
 * ```
 *
 * is transformed into:
 * ```
 *   label@ while(true) {
 *     var x: Int
 *     bodyLabel@ do {
 *        x = y++
 *        ...
 *        break@bodyLabel
 *        ...
 *     } while(false)
 *     if (!(x < 10)) break@label
 *   }
 * ```
 */
class DoWhileLoopLowering(val context: J2clBackendContext) : FileLoweringPass {

  override fun lower(irFile: IrFile) {
    val originalToInnerLoopMap = mutableMapOf<IrDoWhileLoop, IrDoWhileLoop>()
    irFile.transformChildrenVoid(DoWhileReplacementTransformer(originalToInnerLoopMap))

    if (originalToInnerLoopMap.isNotEmpty()) {
      irFile.transformChildrenVoid(ContinueToBreakTransformer(originalToInnerLoopMap))
    }
  }

  private inner class DoWhileReplacementTransformer(
    val originalToInnerLoopMap: MutableMap<IrDoWhileLoop, IrDoWhileLoop>
  ) : IrBuildingTransformer(context.jvmBackendContext) {
    private var labelCounter: Int = 0

    override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrExpression {
      loop.transformChildrenVoid(this)

      val conditionVars = getBodyVariablesUsedInCondition(loop)

      // TODO(dramaix): Consider only rewriting the loop when variables used in the condition are
      // also captured (e.g., by a lambda).
      if (conditionVars.isEmpty()) {
        // We don't have any variable scoping issues so we don't need to rewrite the loop.
        return loop
      }

      // Remove variable declarations that are used in the while condition. These variables will be
      // declared at the beginning of the outer loop to ensure they are in scope for the condition
      // check.
      removeVariableDeclarationsFromBody(loop, conditionVars, builder)

      with(builder.at(loop)) {
        val innerLoop = createInnerLoop(loop)
        originalToInnerLoopMap[loop] = innerLoop

        val outerLoop = irWhile(loop.origin)
        outerLoop.label = loop.label
        outerLoop.condition = irBoolean(true)

        outerLoop.body =
          irBlock(
            startOffset = loop.body?.startOffset ?: loop.condition.startOffset,
            endOffset = loop.condition.endOffset,
          ) {
            // Declare variables used in the condition at the top of the outer loop's body. Their
            // initialization will occur at the original declaration site within the inner loop.
            for (variable in conditionVars) {
              +variable.apply { initializer = null }
            }
            // do { /* body loop */ } while(false)
            +innerLoop
            // if (!condition) break@outerLoop
            +irIfThen(
              context.irBuiltIns.unitType,
              irCallOp(
                context.irBuiltIns.booleanNotSymbol,
                context.irBuiltIns.booleanType,
                loop.condition,
              ),
              irBreak(outerLoop),
            )
          }

        return outerLoop
      }
    }

    /**
     * Finds variables declared within the [IrDoWhileLoop]'s body that are referenced in the loop's
     * condition.
     */
    private fun getBodyVariablesUsedInCondition(loop: IrDoWhileLoop): Set<IrVariable> {
      val body = loop.body ?: return emptySet()

      val variables = mutableSetOf<IrVariableSymbol>()
      body.acceptVoid(
        object : IrVisitorVoid() {
          override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
          }

          override fun visitVariable(declaration: IrVariable) {
            variables.add(declaration.symbol)
          }
        }
      )
      if (variables.isEmpty()) {
        return emptySet()
      }

      val conditionVars = mutableSetOf<IrVariable>()

      loop.condition.acceptVoid(
        object : IrVisitorVoid() {
          override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
          }

          override fun visitGetValue(expression: IrGetValue) {
            val symbol = expression.symbol
            if (symbol in variables) {
              conditionVars.add(symbol.owner as IrVariable)
            }
          }
        }
      )

      return conditionVars
    }

    private fun removeVariableDeclarationsFromBody(
      loop: IrDoWhileLoop,
      conditionVars: Set<IrVariable>,
      builder: IrBuilderWithScope,
    ) {
      loop.body?.transformChildrenVoid(
        object : IrElementTransformerVoid() {
          override fun visitVariable(declaration: IrVariable): IrStatement {
            if (declaration !in conditionVars) {
              return super.visitVariable(declaration)
            }

            return with(builder.at(declaration)) {
              if (declaration.initializer == null) {
                irBlock {}
              } else {
                irSet(declaration.symbol, declaration.initializer!!)
              }
            }
          }
        }
      )
    }

    private fun createInnerLoop(originalLoop: IrDoWhileLoop): IrDoWhileLoop =
      with(builder) {
        irDoWhile(originalLoop.origin).apply {
          label = "loop_body\$${labelCounter++}"
          condition = irBoolean(false)
          body = originalLoop.body
        }
      }
  }

  private inner class ContinueToBreakTransformer(
    val originalToInnerLoopMap: Map<IrDoWhileLoop, IrDoWhileLoop>
  ) : IrBuildingTransformer(context.jvmBackendContext) {
    override fun visitContinue(jump: IrContinue): IrExpression {
      val bodyLoop = originalToInnerLoopMap[jump.loop]

      if (bodyLoop == null) {
        return jump
      }

      return IrBreakImpl(jump.startOffset, jump.endOffset, context.irBuiltIns.nothingType, bodyLoop)
    }
  }
}
