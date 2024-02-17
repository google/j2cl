/*
 * Copyright 2022 Google Inc.
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
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Lowers calls to functions that return unit into a block of the call and a unit object ref.
 *
 * For example:
 * ```
 * fun foo(): Unit {}
 * val x = foo()
 * ```
 *
 * will lower to:
 *
 * ```
 * fun foo(): Unit {}
 * val x = {
 *   foo()
 *   Unit
 * }
 * ```
 */
internal class KotlinUnitValueLowering(private val context: JvmBackendContext) :
  FileLoweringPass, IrElementTransformerVoidWithContext() {

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid(this)
  }

  override fun visitCall(expression: IrCall): IrExpression {
    if (!expression.type.isUnit()) return super.visitCall(expression)

    return with(context.createJvmIrBuilder(currentScope!!, expression)) {
      irBlock(expression, null, expression.type) {
        +super.visitCall(expression)
        +irGetField(
          null,
          this@KotlinUnitValueLowering.context.cachedDeclarations.getFieldForObjectInstance(
            context.irBuiltIns.unitClass.owner
          )
        )
      }
    }
  }
}
