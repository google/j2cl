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
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Removes the temporary variables introduced by function inlining whose initializer is pure, and
 * replaces their reads with a copy of the initializer. Those temporaries are not always removed by
 * the JS optimizer, which increases code size.
 *
 * The Kotlin inliner stores every argument that is not an inlinable lambda in a temporary variable.
 * For example, with:
 * ```
 * inline fun sum(a: Int, b: Int = a + 1): Int = a + b
 * ```
 *
 * The pseudo IR of a call like `sum(10)` after inlining looks like:
 * ```
 * BLOCK INLINE_ARGS_CONTAINER
 *   VAR IR_TEMPORARY_VARIABLE tmp0 = 10
 *   RETURNABLE_BLOCK
 *     INLINED_BLOCK
 *       VAR IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER a = tmp0
 *       VAR IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER b = a + 1
 *       RETURN a + b
 * ```
 *
 * A non-default argument is evaluated at the call site into a temporary of the
 * `INLINE_ARGS_CONTAINER` (`tmp0`), and read by the parameter variable (`a`). A default argument
 * goes directly into the parameter variable (`b`).
 *
 * After this lowering, the pure temporaries are removed and their reads are replaced with their
 * value. `b` is kept, as `10 + 1` is a call:
 * ```
 * BLOCK INLINE_ARGS_CONTAINER
 *   RETURNABLE_BLOCK
 *     INLINED_BLOCK
 *       VAR IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER b = 10 + 1
 *       RETURN 10 + b
 * ```
 *
 * The lowering runs in two phases: [PureVariableCollector] finds the temporaries holding a pure
 * value, and [PureVariableSubstitutor] removes them and substitutes their reads.
 */
internal class InlinedPureArgumentsLowering : FileLoweringPass {
  override fun lower(irFile: IrFile) {
    val pureExpressionByVariable =
      PureVariableCollector().also { irFile.acceptVoid(it) }.pureExpressionByVariable
    irFile.transformChildrenVoid(PureVariableSubstitutor(pureExpressionByVariable))
  }
}

/**
 * Collects the inlining temporaries that hold a pure value, mapped to that value:
 * - the `IR_TEMPORARY_VARIABLE`s of an `INLINE_ARGS_CONTAINER` whose initializer is pure;
 * - the parameter variables of an `IrInlinedFunctionBlock` reading a collected variable, mapped to
 *   its value;
 * - the other parameter variables (default arguments) whose initializer is pure.
 *
 * A parameter variable reading a container temporary that is not pure is not collected. The
 * temporaries of a container must be collected before its children are visited, so before the
 * parameter variables reading them.
 */
private class PureVariableCollector : IrVisitorVoid() {
  val pureExpressionByVariable = mutableMapOf<IrValueSymbol, IrExpression>()
  private val argumentTemporaries = mutableSetOf<IrValueSymbol>()

  override fun visitElement(element: IrElement) {
    element.acceptChildrenVoid(this)
  }

  override fun visitBlock(expression: IrBlock) {
    if (expression.isArgumentContainer) {
      for (temporary in expression.statements.filterIsInstance<IrVariable>()) {
        check(temporary.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE && !temporary.isVar) {
          "Unexpected variable in an INLINE_ARGS_CONTAINER: ${temporary.render()}"
        }
        argumentTemporaries += temporary.symbol
        collectIfPure(temporary)
      }
    }
    expression.acceptChildrenVoid(this)
  }

  override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock) {
    for (parameterVariable in
      inlinedBlock.statements.filterIsInstance<IrVariable>().filter { it.isParameterVariable() }) {
      check(!parameterVariable.isVar) {
        "Unexpected var parameter variable: ${parameterVariable.render()}"
      }
      collectParameterVariable(parameterVariable)
    }
    inlinedBlock.acceptChildrenVoid(this)
  }

  private fun collectParameterVariable(variable: IrVariable) {
    val readSymbol = (variable.initializer as? IrGetValue)?.symbol
    val pureExpression = pureExpressionByVariable[readSymbol]
    if (pureExpression != null) {
      // The parameter variable reads another variable set to a pure expression. Map it to the pure
      // expression too, so that its reads are also replaced by the pure expression.
      pureExpressionByVariable[variable.symbol] = pureExpression
    } else if (readSymbol !in argumentTemporaries) {
      // A default argument using the default value, collect it if the default value is pure.
      collectIfPure(variable)
    }
  }

  private fun collectIfPure(variable: IrVariable) {
    val initializer = variable.initializer ?: return
    if (initializer.isSubstitutablePureExpression()) {
      pureExpressionByVariable[variable.symbol] = initializer
    }
  }
}

/**
 * Removes the declarations of the variables in [pureExpressionByVariable] from the
 * `INLINE_ARGS_CONTAINER` and `IrInlinedFunctionBlock` blocks, and replaces their reads with a copy
 * of their value.
 */
private class PureVariableSubstitutor(
  private val pureExpressionByVariable: Map<IrValueSymbol, IrExpression>
) : IrElementTransformerVoid() {
  override fun visitBlock(expression: IrBlock): IrExpression {
    expression.transformChildrenVoid(this)

    if (expression.isArgumentContainer || expression is IrInlinedFunctionBlock) {
      expression.statements.removeAll { it is IrVariable && it.symbol in pureExpressionByVariable }
    }
    return expression
  }

  override fun visitGetValue(expression: IrGetValue): IrExpression {
    val pureExpression =
      pureExpressionByVariable[expression.symbol] ?: return super.visitGetValue(expression)
    // Copy the expression, as the variable can be read more than once, and transform the copy, as
    // the expression can read other removed variables.
    return pureExpression.deepCopyWithSymbols().transform(this, null)
  }
}

private val IrBlock.isArgumentContainer
  get() = origin == IrStatementOrigin.INLINE_ARGS_CONTAINER

private fun IrVariable.isParameterVariable() =
  origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER ||
    origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER

/**
 * Returns whether the expression is a pure expression that can be substituted for a variable.
 *
 * After unwrapping the top-level implicit casts, we only consider an expression pure if it is:
 * - an `IrConst`;
 * - a read of a `val` or a parameter (cannot be reassigned);
 * - an `INSTANCEOF`, `NOT_INSTANCEOF` or `REINTERPRET_CAST` whose argument is pure;
 * - a reference to the `Unit` object;
 * - a read of a final field, either static or on a pure receiver.
 */
private fun IrExpression?.isSubstitutablePureExpression(): Boolean {
  // The inliner unwraps the implicit casts of an argument before checking whether it is pure, and
  // adds an implicit cast to the parameter type if needed.
  val unwrapped = this?.unwrapImplicitCasts() ?: return false
  // Varargs are excluded: `isPure` accepts a vararg whose elements are pure, but substituting it
  // would create a new array at each use of the parameter.
  return unwrapped !is IrVararg &&
    unwrapped.isPure(
      anyVariable = false,
      // Deliberately passing no symbols so that every call is considered not pure.
      symbols = null,
    )
}

private tailrec fun IrExpression.unwrapImplicitCasts(): IrExpression =
  if (this is IrTypeOperatorCall && operator == IrTypeOperator.IMPLICIT_CAST) {
    argument.unwrapImplicitCasts()
  } else {
    this
  }
