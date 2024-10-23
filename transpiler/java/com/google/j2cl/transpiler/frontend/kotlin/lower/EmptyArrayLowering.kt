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
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArrayOf
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.toArrayOrPrimitiveArrayType

/**
 * Lower `kotlin.emptyArray()` calls to `arrayOf()` calls.
 *
 * The file where `emptyArray()` is defined is part of the builtins of the standard library and K2
 * loads the definition of the function from the builtins instead of the binaray source. This breaks
 * the inlining of the function.
 *
 * TODO(b/374966022): Check if this pass is still needed once the klib based inliner is implemented.
 */
class EmptyArrayLowering(val context: JvmBackendContext) :
  FileLoweringPass, IrElementTransformerVoidWithContext() {

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid()
  }

  @OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)
  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    expression.transformChildrenVoid()

    val callee = expression.symbol.owner
    val parent = callee.parent

    if (
      callee.name.asString() == "emptyArray" &&
        parent is IrPackageFragment &&
        parent.packageFqName.asString() == "kotlin"
    ) {
      val arrayType = expression.getTypeArgument(0)!!.makeNotNull()
      return context
        .createJvmIrBuilder(currentScope!!)
        .irArrayOf(arrayType.toArrayOrPrimitiveArrayType(context.irBuiltIns))
    }
    return expression
  }
}
