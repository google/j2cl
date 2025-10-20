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
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Removes implicit casts that derive from smart casts that are unnecessary.
 *
 * These casts are being removed as they can cause bloat in the AST and output, or in the case of
 * primitives, cause extra boxing/unboxing operations to occur.
 */
internal class SmartCastCleaner : FileLoweringPass, IrElementTransformerVoid() {

  override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

  override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
    expression.transformChildrenVoid()

    if (expression.operator != IrTypeOperator.IMPLICIT_CAST) {
      return expression
    }

    val argument = expression.argument
    val originalType = argument.type
    val targetType = expression.typeOperand

    // Implicit cast that promotes the nullability of a primitive type is not needed for J2CL and
    // lead to extra boxing/unboxing operations.
    if (targetType.isPrimitiveType() && targetType.classOrFail == originalType.classOrNull) {
      return expression.argument
    }

    return expression
  }
}
