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

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * This class removes implicit casts to parent types that do not promote nullability. It replaces
 * such casts with implicit casts to the original non-nulable type if they do promote nullability.
 *
 * Implicit casts involved in member access dispatch receivers are not modified, as they may be used
 * to target specific members on the parent class when generics are involved.
 */
// TODO(b/377502016): Remove this pass once smart cast's bug is fixed.
internal class SmartCastCleaner : FileLoweringPass, IrElementTransformerVoid() {

  var dispatchReceivers: MutableSet<IrExpression> = hashSetOf()

  override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

  override fun visitMemberAccess(expression: IrMemberAccessExpression<*>): IrExpression {
    if (expression.dispatchReceiver != null) {
      dispatchReceivers.add(expression.dispatchReceiver!!)
    }
    val visitedExpression = super.visitMemberAccess(expression)
    if (expression.dispatchReceiver != null) {
      dispatchReceivers.remove(expression.dispatchReceiver!!)
    }
    return visitedExpression
  }

  private fun IrExpression.isDispatchReceiver() = dispatchReceivers.contains(this)

  override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
    if (!expression.isDispatchReceiver() && expression.operator == IrTypeOperator.IMPLICIT_CAST) {
      val argument = expression.argument
      val originalType = argument.type
      val targetType = expression.typeOperand
      if (targetType.classOrNull != null && originalType.isSubtypeOfClass(targetType.classOrFail)) {
        if (!originalType.isNullable() || targetType.isNullable()) {
          // Implicit cast to a parent class that does not promote the nullability can be
          // removed.
          return argument
        } else {
          // Nullability promotion: replace the original implicit cast to an implcit cast to the
          // original non-nullable type.
          return IrTypeOperatorCallImpl(
            expression.startOffset,
            expression.endOffset,
            originalType.makeNotNull(),
            IrTypeOperator.IMPLICIT_CAST,
            originalType.makeNotNull(),
            argument,
          )
        }
      }
    }

    return super.visitTypeOperator(expression)
  }
}
