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
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This lowering pass recreates `for` loops from some `while` or `do while` loops:
 * 1. Iteration over an array (e.g., `for (i in array)`) has been transformed into the following:
 * ```
 *   // #1: 3 temporary variables
 *   val indexedObject = array
 *   var inductionVar = 0
 *   val last = indexedObject.size
 *
 *   // #2: The inner while loop
 *   while (inductionVar < last) {
 *     val i = indexedObject[inductionVar]
 *     val inductionVar = inductionVar + 1
 *     {
 *       // Loop body
 *     }
 *   }
 * ```
 *
 * This lowering pass will transform that to for loop:
 * ```
 *   for (
 *       var indexedObject = array, inductionVar = 0, last = indexedObject.size;
 *       inductionVar < last;
 *       inductionVar = inductionVar + 1) {
 *     val loopVar = indexedObject[inductionVar]
 *     {
 *       // Loop body
 *     }
 *   }
 * ```
 * 2. Iteration over non-overflowing ranges (e.g., `for (i in 1 .. 3)`):
 * ```
 *   // #1: 1 temporary
 *   variable var inductionVar = 0
 *
 *   // #2: a if-guarded `do while` loop where the `if` condition is the same than the `while`
 *   //    condition
 *   if (condition) {
 *     do {
 *       val loopVar = inductionVar
 *       inductionVar = inductionVar + 1
 *       {
 *         // loop body
 *       }
 *     } while (condition)
 *   }
 * ```
 *
 * will be transformed to:
 * ```
 *   for (var inductionVar = 0; condition; inductionVar = inductionVar + 1) {
 *     val i = inductionVar
 *     {
 *       // Loop body
 *     }
 *   }
 * ```
 *
 * A note about overflowing ranges: A range is considered as overflowing when you cannot determine
 * at compile time that the end of the range is less than max value of the range type.
 *
 * ex: `for(i in 0 .. aVariable)`, `for(i in 0 .. array.size)`
 *
 * Unfortunately, ranges with custom step fall into that category even when the range boundary is
 * known at compile time.
 *
 * ex: `for (i in 0 .. 5 step 3)` is not optimized by this pass.
 *
 * This pass is doing its best effort to recreate the `for` loop and is deliberately lenient. It
 * will skip unexpected IR structure.
 */
internal class CreateForLoopLowering : BodyLoweringPass {
  override fun lower(irBody: IrBody, container: IrDeclaration) {
    val oldLoopToNewLoop = mutableMapOf<IrLoop, IrLoop>()
    val transformer = LoopTransformer(oldLoopToNewLoop)
    irBody.transformChildrenVoid(transformer)

    // Update references in break/continue.
    irBody.transformChildrenVoid(
      object : IrElementTransformerVoid() {
        override fun visitBreakContinue(jump: IrBreakContinue): IrExpression {
          oldLoopToNewLoop[jump.loop]?.let { jump.loop = it }
          return jump
        }
      }
    )
  }
}

private class LoopTransformer(val oldLoopToNewLoop: MutableMap<IrLoop, IrLoop>) :
  IrElementTransformerVoidWithContext() {

  override fun visitBlock(expression: IrBlock): IrExpression {
    // The psi2ir transformer wraps all `for` loop into an `IrBlock` with origin `FOR_LOOP`.
    // After this check, we are sure that we are manipulating `while` and `do while` loop that has
    // been created by the Kotlin compiler to represent a for loop. We can make assumption on them
    // because we will never match a `while` or `do while` loop written by the user.
    if (expression.origin != IrStatementOrigin.FOR_LOOP) {
      return super.visitBlock(expression)
    }

    // Extract the different component of the for loop. If we are unable to extract one of the
    // component, it means that the current loop is not supported.
    val initializers = extractInitializers(expression) ?: return super.visitBlock(expression)
    val condition = extractCondition(expression) ?: return super.visitBlock(expression)
    val innerLoopBody =
      extractAndSplitInnerLoopBody(expression) ?: return super.visitBlock(expression)

    val oldLoop = getInnerLoop(expression)
    val newLoop =
      IrForLoop.from(oldLoop).apply {
        this.condition = condition
        this.initializers = initializers
        updates = mutableListOf(innerLoopBody.inductionVariableUpdate)
        body =
          IrCompositeImpl(
            innerLoopBody.originalLoopBody.startOffset,
            innerLoopBody.originalLoopBody.endOffset,
            innerLoopBody.originalLoopBody.type,
            innerLoopBody.originalLoopBody.origin,
            innerLoopBody.loopVariablesDefinitions + innerLoopBody.originalLoopBody.statements
          )
      }
    // Update mapping from old to new loop, so we can later update references in break/continue.
    oldLoopToNewLoop[oldLoop] = newLoop

    return visitLoop(newLoop)
  }

  private fun extractInitializers(enclosingBlock: IrBlock): MutableList<IrVariable>? {
    val blockStatements = enclosingBlock.statements
    // Supported loops have at least the definition of the induction variable and the loop itself.
    if (blockStatements.size < 2) {
      return null
    }

    val initializers = mutableListOf<IrVariable>()
    // all block statements but the last one should be temporary variable definitions.
    for (i in 0 until blockStatements.size - 1) {
      val s = blockStatements[i]
      if (s !is IrVariable || s.origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE) {
        // unexpected statement
        return null
      }
      initializers += s
    }

    return initializers
  }

  private fun extractCondition(irBlock: IrBlock): IrExpression? =
    when (val innerLoop = getInnerLoop(irBlock)) {
      is IrWhileLoop -> innerLoop.condition
      is IrDoWhileLoop -> {
        // We only transform if-guarded do while loop if the condition of the `if` and the `while`
        // conditions are the same.
        val ifCondition = (irBlock.statements.last() as IrWhen).branches.single().condition
        if (compareConditionExpressions(innerLoop.condition, ifCondition)) {
          innerLoop.condition
        } else {
          null
        }
      }
      else -> null
    }

  private fun compareConditionExpressions(first: IrExpression, second: IrExpression): Boolean {
    // The supported loops are always using comparison operators. They are modeled as function call.
    // If we found another type of condition expression, it means we do not support this type of
    // loop.
    if (
      first !is IrCall ||
        second !is IrCall ||
        first.symbol != second.symbol ||
        first.valueArgumentsCount != second.valueArgumentsCount
    ) {
      return false
    }

    // The conditions created by Kotlin compiler are function calls that take either a variable
    // reference (induction variable, last item of the progression) or a constant as arguments.
    for (i in 0 until first.valueArgumentsCount) {
      val firstCallArg = first.getValueArgument(i)!!
      val secondCallArg = second.getValueArgument(i)!!

      when (firstCallArg) {
        is IrGetValue -> {
          if (firstCallArg.symbol != (secondCallArg as? IrGetValue)?.symbol) {
            return false
          }
        }
        is IrConst<*> -> {
          if (firstCallArg.value != (secondCallArg as? IrConst<*>)?.value) {
            return false
          }
        }
        else -> return false
      }
    }
    return true
  }

  private data class InnerLoopBody(
    val loopVariablesDefinitions: MutableList<IrVariable>,
    val inductionVariableUpdate: IrSetValue,
    val originalLoopBody: IrContainerExpression
  )

  private fun extractAndSplitInnerLoopBody(enclosingBlock: IrBlock): InnerLoopBody? {
    val innerLoopBody = getInnerLoop(enclosingBlock).body as IrContainerExpression
    // We expect the loop body to have at least 3 statements:
    //  #1: The declarations of the variables used in the `for` loop. We can have multiple variables
    //      in case of destructured declaration: `for ((key, value) in pairs)`
    //  #2: The update of the induction variable
    //  #3: A block containing the body of the initial `for` loop
    // Note that the induction variable update happens before the body of the original loop runs.
    // After the transformation to a for loop, the update of the induction variable will be done
    // after the execution of the original loop body. In theory, to be safe, we should check that
    // the induction variable is not referenced in the original body loop. But in practice, the
    // induction variable is introduced by the compiler to only get the iteration object and the
    // user code cannot refer to it. There is currently no compiler optimization that introduce a
    // reference to the induction variable into the body loop, and it's most unlikely they will have
    // such optimization in the future.
    val statements = innerLoopBody.statements

    if (statements.size < 3) {
      return null
    }

    val loopVariablesDefinitions = mutableListOf<IrVariable>()
    var inductionVariableUpdate: IrSetValue? = null

    for (i in 0 until statements.size - 1) {
      when (val s = statements[i]) {
        is IrVariable -> loopVariablesDefinitions.add(s)
        is IrSetValue -> {
          if (inductionVariableUpdate != null) {
            // we expect only one variable update
            return null
          }
          inductionVariableUpdate = s
        }
        else -> return null // unexpected statement
      }
    }

    if (inductionVariableUpdate == null) {
      return null
    }

    val originalLoopBody = statements.last() as? IrContainerExpression ?: return null

    return InnerLoopBody(loopVariablesDefinitions, inductionVariableUpdate, originalLoopBody)
  }

  private fun getInnerLoop(enclosingBlock: IrBlock): IrLoop {
    val lastStatement = enclosingBlock.statements.last()
    // `for` loops have been transformed either in a `while` loop or an if-guarded `do while` loop.
    // Any other structure is an error.
    return checkInnerLoop(
      when (lastStatement) {
        is IrWhileLoop -> lastStatement
        is IrWhen -> lastStatement.branches.single().result as? IrDoWhileLoop
        else -> null
      }
    )
  }

  private fun checkInnerLoop(loop: IrLoop?): IrLoop {
    if (loop == null) {
      throw IllegalStateException("Malformed for loop block")
    }
    if (loop.origin != IrStatementOrigin.FOR_LOOP_INNER_WHILE) {
      throw IllegalStateException("Unexpected loop origin ${loop.origin}")
    }
    return loop
  }
}
