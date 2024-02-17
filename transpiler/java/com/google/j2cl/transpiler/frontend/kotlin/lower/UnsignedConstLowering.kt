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
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isUnsigned
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/** Lowers unsigned const values to be instantiations of their boxed value types. */
internal class UnsignedConstLowering : FileLoweringPass, IrElementTransformerVoidWithContext() {
  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid(this)
  }

  override fun visitConst(expression: IrConst<*>): IrExpression {
    if (!expression.type.isUnsigned()) return super.visitConst(expression)
    val classSymbol = expression.type.classifierOrNull as IrClassSymbol
    val constructor = classSymbol.constructors.single { it.owner.isPrimary }
    val argType = constructor.owner.valueParameters.first().type
    val valueExpression =
      when (expression.kind) {
        IrConstKind.Byte ->
          IrConstImpl.Companion.byte(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            argType,
            IrConstKind.Byte.valueOf(expression)
          )
        IrConstKind.Short ->
          IrConstImpl.Companion.short(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            argType,
            IrConstKind.Short.valueOf(expression)
          )
        IrConstKind.Int ->
          IrConstImpl.Companion.int(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            argType,
            IrConstKind.Int.valueOf(expression)
          )
        IrConstKind.Long ->
          IrConstImpl.Companion.long(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            argType,
            IrConstKind.Long.valueOf(expression)
          )
        else -> compilationException("Unexpected unsigned kind: ${expression.kind}", expression)
      }
    return IrConstructorCallImpl.fromSymbolOwner(classSymbol.defaultType, constructor).apply {
      putValueArgument(0, valueExpression)
    }
  }
}
