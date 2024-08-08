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
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.findEnumValuesFunction
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.typeArguments
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Implement enumEntries() and Enum.entries by rewriting all callers at the call site.
 *
 * In both cases the references will be replaced with `enumEntries<SomeEnum>(SomeEnum.values)`.
 */
internal class EnumEntriesIntrinsicLowering(private val context: JvmBackendContext) :
  FileLoweringPass, IrElementTransformerVoid() {
  private val createEnumEntriesSymbol = context.ir.symbols.createEnumEntries

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid()
  }

  override fun visitCall(expression: IrCall): IrExpression {
    val enumType =
      when {
        expression.isEnumEntries -> requireNotNull(expression.typeArguments[0])
        expression.isEnumGetEntries -> expression.symbol.owner.parentAsClass.defaultType
        else -> return super.visitCall(expression)
      }
    val enumValuesFun = enumType.classOrFail.owner.findEnumValuesFunction(context)
    val returnType = context.ir.symbols.enumEntries.typeWith(enumType)

    // enumEntries<SomeEnum>(SomeEnum.values)
    return context.createJvmIrBuilder(createEnumEntriesSymbol, expression).run {
      irCall(this@EnumEntriesIntrinsicLowering.createEnumEntriesSymbol, returnType).apply {
        putTypeArgument(0, enumType)
        putValueArgument(0, irCall(enumValuesFun))
      }
    }
  }

  override fun visitClass(declaration: IrClass): IrStatement {
    if (!declaration.isEnumClass) return super.visitClass(declaration)
    // Remove the <get-entries> special function from Enum classes. All references should be
    // rewritten at the call site.
    declaration.declarations.removeIf {
      it is IrFunction &&
        it.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER &&
        (it.body as? IrSyntheticBody)?.kind == IrSyntheticBodyKind.ENUM_ENTRIES
    }
    return declaration
  }
}

private val IrCall.isEnumEntries: Boolean
  get() =
    with(symbol.owner) {
      kotlinFqName == enumEntriesFqName &&
        valueParameters.isEmpty() &&
        typeArgumentsCount == 1 &&
        dispatchReceiverParameter == null &&
        extensionReceiverParameter == null
    }

private val enumEntriesFqName = FqName("kotlin.enums.enumEntriesIntrinsic")

private val IrCall.isEnumGetEntries: Boolean
  get() =
    with(symbol.owner) {
      name == enumGetEntriesName &&
        parentClassOrNull?.isEnumClass == true &&
        dispatchReceiverParameter == null &&
        extensionReceiverParameter == null &&
        valueParameters.isEmpty()
    }

private val enumGetEntriesName = Name.special("<get-entries>")
