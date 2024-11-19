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

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This lowering pass creates `switch` statements from `when` statements with subject when the
 * subject is type of`Enum`, `String`, `Char`, `Byte`, `Short` or `Int`.
 *
 * Ex: The following `When` on `Enum`:
 * ```
 *   when (getMyEnum()) {
 *     MyEnum.ONE, MyEnum.TWO, MyEnum.THREE -> /* Result 1 */ ...
 *     MyEnum.FOUR -> /* Result 2 */ ...
 *   }
 * ```
 *
 * has been transformed into the following:
 * ```
 *   // #1: one temporary variable
 *   val tmp = getMyEnum()
 *
 *   // #2: a `when` without subject
 *   when {
 *     // Multiple entry clause create a nested `when` expressions chain.
 *     when {
 *        tmp == MyEnum.ONE -> true
 *        else -> when {
 *            tmp == MyEnum.TWO -> true
 *            else -> tmp == MyEnum.THREE
 *        }
 *     } -> /* Result 1 */ ...
 *     tmp == MyEnum.FOUR -> /* Result 2 */ ...
 *     else -> throw NoWhenBranchMatchedException()
 *   }
 * ```
 *
 * This lowering pass will transform that to a `switch` construct:
 * ```
 *   val tmp = getMyEnum()
 *   switch (tmp) {
 *     case MyEnum.ONE:
 *     case MyEnum.TWO:
 *     case MyEnum.THREE:
 *       /* Result 1 */ ...
 *       break;
 *     case MyEnum.TWO:
 *       /* Result 2 */ ...
 *       break;
 *       }
 *     default: throw NoWhenBranchMatchedException()
 *  ```
 *
 * This pass is doing its best effort to create the `switch` statement and is deliberately lenient.
 * It will skip unexpected IR structure.
 *
 * This pass does not handle `when` used as expressions as `switch` is a statement.
 *
 * TODO(b/163151103): Handle `when` expressions when `switch` expressions are supported in the J2CL
 *   AST.
 */
internal class CreateSwitchLowering(private val context: CommonBackendContext) : BodyLoweringPass {
  override fun lower(irBody: IrBody, container: IrDeclaration) {
    irBody.transformChildrenVoid(WhenTransformer(context))
  }
}

private class WhenTransformer(private val context: CommonBackendContext) :
  IrElementTransformerVoidWithContext() {
  val optimizableSubjectTypes =
    hashSetOf(
      context.irBuiltIns.stringType,
      context.irBuiltIns.charType,
      context.irBuiltIns.byteType,
      context.irBuiltIns.shortType,
      context.irBuiltIns.intType,
    )

  /**
   * Visit IrBlock created by the Kotlinc frontend in order to try to recover the original `when`
   * statement and transform it to a `switch` statement if possible.
   */
  override fun visitBlock(expression: IrBlock): IrExpression {
    expression.transformChildrenVoid()

    if (!isValidWhenBlock(expression)) {
      return expression
    }

    // Label unlabelled break statements existing in the original `when` statement. Otherwise, they
    // will break the newly created `switch` statement instead of the enclosing loop.
    labelExistingBreakStatement(expression.whenExpression)

    val whenSubject = expression.whenSubject
    val switchExpression = context.createIrBuilder(whenSubject.symbol).irGet(whenSubject)
    val caseStatements = extractCases(expression)

    val irSwitch =
      IrSwitch(switchExpression, caseStatements, expression.startOffset, expression.endOffset)

    // Replace the `when` statement with the new `switch` statement
    expression.statements.removeLast()
    expression.statements.add(irSwitch)

    return expression
  }

  private fun isValidWhenBlock(block: IrBlock): Boolean {
    if (block.origin != IrStatementOrigin.WHEN) {
      return false
    }

    // when-block with subject should have two children: a temporary variable that define the
    // subject and the `when` statement itself.
    if (
      block.statements.size != 2 ||
        block.statements[0] !is IrVariable ||
        block.statements[1] !is IrWhen
    ) {
      return false
    }

    val subject = block.whenSubject
    if (
      subject.type.getClass()?.kind != ClassKind.ENUM_CLASS &&
        subject.type !in optimizableSubjectTypes
    ) {
      return false
    }

    return areBranchesValid(block.whenExpression, subject)
  }

  private fun areBranchesValid(irWhen: IrWhen, subject: IrVariable): Boolean {
    for (branch in irWhen.branches) {
      if (branch is IrElseBranch) {
        // Check the else branch only in the case of a multiple value clause.
        if (
          irWhen.origin == IrStatementOrigin.OROR && !isValidWhenCondition(branch.result, subject)
        ) {
          // Multiple entry clause:
          // ```
          //  when (subject) {
          //      VALUE_1, VALUE_2, ..., VALUE_N -> { /* BODY */ }
          //  }
          // ```
          // is transformed into an `or` sequence:
          // ```
          // if (((subject == VALUE_1) || (subject == VALUE_2)) ... || (subject = VALUE_N)) {
          //   /* BODY */
          // }
          // ```
          // represented as a nested `when` expressions chain:
          // ```
          //  val tmp = subject
          //  when {
          //    when {
          //      ...
          //        when {
          //          when {
          //            tmp == VALUE_1 -> true
          //            else -> tmp == VALUE_2
          //          } -> true
          //          else ->  ...
          //        when {
          //          tmp == VALUE_N-1 -> true
          //          else ->  tmp == VALUE_N-1
          //        } ->true
          //    } ->  { /* BODY */ }
          //  }
          // ```
          // The default branch `else` returns the value of testing the last value.
          // Note that this `when` expression is NOT transformed into a block and hence would not
          // have been converted to a switch,so here we test that the result of the branch is what
          // we expect to extract the values later.
          return false
        }
        continue
      }

      if (!isValidWhenCondition(branch.condition, subject)) {
        return false
      }
    }
    return true
  }

  private fun isValidWhenCondition(condition: IrExpression, subject: IrVariable): Boolean {
    when (condition) {
      is IrCall -> {
        // Single entry clause:
        // ```
        //  when (subject) {
        //      VALUE_1 -> { /* BODY */ }
        //  }
        // ```
        // is transformed to:
        // ```
        //  val tmp = subject
        //  when {
        //    tmp == VALUE_1 -> { /* BODY */ }
        //  }
        //  ```
        // Condition should match the pattern `subject == expression`
        if (condition.symbol != context.irBuiltIns.eqeqSymbol) {
          return false
        }
        var firstArgument = condition.getValueArgument(0)
        // Short and Byte are cast to Int
        if (firstArgument is IrTypeOperatorCall) {
          if (
            firstArgument.operator != IrTypeOperator.CAST &&
              firstArgument.typeOperand != context.irBuiltIns.intType
          ) {
            return false
          }
          firstArgument = firstArgument.argument
        }
        if (firstArgument !is IrGetValue || firstArgument.symbol != subject.symbol) {
          return false
        }
      }
      is IrWhen -> {
        // Multiple entry clause:
        // ```
        //  when (subject) {
        //      VALUE_1, VALUE_2, ..., VALUE_N -> { /* BODY */ }
        //  }
        // ```
        // is transformed into an `or` sequence:
        // ```
        // if (((subject == VALUE_1) || (subject == VALUE_2)) ... || (subject = VALUE_N)) {
        //   /* BODY */
        // }
        // ```
        // represented as a nested `when` expressions chain in the IR tree :
        // ```
        //  val tmp = subject
        //  when {
        //    when {
        //      ...
        //        when {
        //          when {
        //            tmp == VALUE_1 -> true
        //            else -> tmp == VALUE_2
        //          } -> true
        //          else ->  ...
        //        when {
        //          tmp == VALUE_N-1 -> true
        //          else ->  tmp == VALUE_N-1
        //        } ->true
        //    } ->  { /* BODY */ }
        //  }
        // ```
        // Note that this `when` expression is NOT transformed into a block and hence would not have
        // been converted to a switch, so here we check that the form is what we expect to extract
        // the values later.
        // Testing the origin of the `when` ensures it is created by the Kotlin compiler to model a
        // multiple entry clause.
        if (condition.origin != IrStatementOrigin.OROR || !areBranchesValid(condition, subject)) {
          return false
        }
      }
      else -> return false // not optimizable when
    }
    return true
  }

  private fun extractCases(block: IrBlock): List<IrSwitchCase> {
    val caseStatements = mutableListOf<IrSwitchCase>()

    for (branch in block.whenExpression.branches) {
      // `when` does not fallthrough. That's not the case for a switch statement. We are always
      // adding a `break` as the last statement of a case body. If the `break` is unreachable, it
      // will be removed by the `CleanupLowering` pass.
      val caseBody = addBreakStatement(branch.result)
      var cases =
        if (branch is IrElseBranch) {
          // default case.
          listOf()
        } else {
          when (val condition = branch.condition) {
            is IrCall -> {
              // single entry clause is represented by
              // `subject == VALUE -> ...`
              // and will create the following case :
              // `case VALUE: ...`
              listOf(extractCaseExpression(condition))
            }
            is IrWhen -> {
              // A `IrWhen` expression with multiple entry clauses.
              // `VALUE_1, VALUE_2, ..., VALUE_N -> ...`
              extractCasesInMultiEntryWhen(condition)
            }
            else -> throw AssertionError("Unexpected branch:${branch.dump()}")
          }
        }
      caseStatements.add(IrSwitchCase(cases, caseBody, branch.startOffset, branch.endOffset))
    }

    return caseStatements
  }

  fun extractCasesInMultiEntryWhen(irWhen: IrWhen): List<IrExpression> {
    // A `IrWhen` with multiple entry clause
    // `VALUE_1, VALUE_2, ..., VALUE_N -> /* BODY */`
    // is represented in the IR tree as a chain of nested `when`.
    // We will traverse this `when` chain to collect all the case expressions to create the
    // following cases list:
    // ```
    //  case VALUE_1, VALUE_2, ..., VALUE_N: /* Expression or Statement */
    // ```
    // TODO(b/163151103): Convert non-top level `when` to switch expressions.
    val caseExpressions = mutableListOf<IrExpression>()

    for (branch in irWhen.branches) {
      // in a regular branch, the case expression is in the condition. In the else branch, it is in
      // the result:
      // ```
      // subject == VALUE1 -> true
      // else -> subject == VALUE2
      // ```
      val condition = if (branch is IrElseBranch) branch.result else branch.condition

      if (condition is IrWhen) {
        caseExpressions.addAll(extractCasesInMultiEntryWhen(condition))
      } else {
        caseExpressions.add(extractCaseExpression(condition))
      }
    }
    return caseExpressions
  }

  private fun extractCaseExpression(irExpression: IrExpression): IrExpression {
    // At this point we know the irExpression represents the following expression: `subject ==
    // expr`. The case expression is the rhs of the equality.
    check(irExpression is IrCall)
    return checkNotNull(irExpression.getValueArgument(1))
  }

  private fun addBreakStatement(originalBody: IrExpression): IrExpression {
    return IrCompositeImpl(
      originalBody.startOffset,
      originalBody.endOffset,
      originalBody.type,
      origin = null,
      statements = listOf(originalBody, IrSwitchBreak()),
    )
  }

  private fun labelExistingBreakStatement(irWhen: IrWhen) {
    val enclosedLoops = mutableSetOf<IrLoop>()
    val enclosedBreaks = mutableListOf<IrBreak>()
    irWhen.acceptVoid(
      object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

        override fun visitLoop(loop: IrLoop) {
          enclosedLoops.add(loop)
        }

        override fun visitBreak(jump: IrBreak) {
          enclosedBreaks.add(jump)
        }
      }
    )

    for (irBreak in enclosedBreaks) {
      if (irBreak.label != null) {
        continue
      }
      if (irBreak.loop in enclosedLoops) {
        // This `break` is breaking an enclosed loop and will not affect the newly created `switch`.
        continue
      }
      irBreak.label = irBreak.loop.getOrAssignLabel()
    }
  }

  var labelCounter = 0

  fun IrLoop.getOrAssignLabel(): String {
    if (label == null) {
      label = "\$switchLowering\$${labelCounter++}"
    }
    return label!!
  }
}

private val IrBlock.whenSubject
  get() = statements[0] as IrVariable

private val IrBlock.whenExpression
  get() = statements[1] as IrWhen
