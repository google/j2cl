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
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArrayOf
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.toArrayOrPrimitiveArrayType

/**
 * Lower `kotlin.emptyArray()` calls to `arrayOf()` calls.
 *
 * `kotlin.emptyArray()` is an inline function. When inlined, it expands to a call to
 * `arrayOfNulls(0)` creating an array of nullable types (`Array<T?>`). This is problematic for J2CL
 * when `T` is a primitive type, as it would result in an array of boxed type instead of the
 * expected primitive array type (e.g., `Integer[]` instead of `int[]` for `T = Int`).
 *
 * To prevent this type mismatch, we skip the default inlining of `emptyArray()` and instead lower
 * it to a call to `kotlin.arrayOf()` (or the equivalent primitive version of `arrayOf()` like
 * `intArrayOf()`), which correctly produces an empty array of the appropriate type.
 */
class EmptyArrayLowering(val context: J2clBackendContext) :
  FileLoweringPass, IrElementTransformerVoidWithContext() {

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid()
  }

  @OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)
  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    expression.transformChildrenVoid()

    val callee = expression.symbol.owner

    if (context.intrinsics.isEmptyArray(expression.symbol)) {
      val arrayType = expression.typeArguments[0]!!.makeNotNull()
      return context.jvmBackendContext
        .createJvmIrBuilder(currentScope!!)
        .irArrayOf(arrayType.toArrayOrPrimitiveArrayType(context.irBuiltIns))
    }
    return expression
  }
}
