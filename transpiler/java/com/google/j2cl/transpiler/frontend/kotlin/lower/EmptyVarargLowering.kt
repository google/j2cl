/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArrayOf
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.* // ktlint-disable
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.* // ktlint-disable

// Replaces empty vararg references with empty array instantiations.
// Based on org.jetbrains.kotlin.backend.jvm.lower.VarargLowering
internal class EmptyVarargLowering(val context: JvmBackendContext) :
  FileLoweringPass, IrElementTransformerVoidWithContext() {
  override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

  // Ignore annotations
  override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
    val constructor = expression.symbol.owner
    if (constructor.constructedClass.isAnnotationClass) return expression
    return super.visitConstructorCall(expression)
  }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    expression.transformChildrenVoid()
    val function = expression.symbol

    // Replace empty varargs with empty arrays
    for (i in 0 until expression.valueArgumentsCount) {
      if (expression.getValueArgument(i) != null) continue

      val parameter = function.owner.valueParameters[i]
      if (parameter.varargElementType != null && !parameter.hasDefaultValue()) {
        // Compute the correct type for the array argument.
        val arrayType = parameter.type.substitute(expression.typeSubstitutionMap).makeNotNull()
        expression.putValueArgument(i, createBuilder().irArrayOf(arrayType))
      }
    }

    return expression
  }

  private fun createBuilder(
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
  ) = context.createJvmIrBuilder(currentScope!!, startOffset, endOffset)
}
