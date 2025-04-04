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
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.findEnumValuesFunction
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.getPrimitiveType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/** Replace calls to intrinsic function with the implementation specific to the platform. */
class IntrinsicFunctionCallsLowering(j2clBackendContext: J2clBackendContext) :
  FileLoweringPass, IrElementTransformerVoid() {
  private val context = j2clBackendContext.jvmBackendContext
  private val intrinsics = j2clBackendContext.intrinsics

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid()
  }

  override fun visitCall(expression: IrCall): IrExpression {
    return when {
      expression.isEnumEntries || expression.isEnumGetEntries -> lowerEnumEntries(expression)
      expression.isRangeUntilCall -> lowerRangeUntilCall(expression)
      expression.isArrayIteratorCall -> lowerArrayIteratorCall(expression)
      expression.isNoWhenBranchMatchedException -> lowerNoWhenBranchMatchedException(expression)
      expression.isCompareTo -> lowerCompareToCall(expression)
      else -> super.visitCall(expression)
    }
  }

  /**
   * Implement enumEntries() and Enum.entries by rewriting all callers at the call site.
   *
   * In both cases the references will be replaced with `enumEntries<SomeEnum>(SomeEnum.values)`
   */
  private fun lowerEnumEntries(expression: IrCall): IrExpression {
    val enumType =
      when {
        expression.isEnumEntries -> requireNotNull(expression.typeArguments[0])
        expression.isEnumGetEntries -> expression.symbol.owner.parentAsClass.defaultType
        else -> throw AssertionError("Unexpected call")
      }
    val enumValuesFun = enumType.classOrFail.owner.findEnumValuesFunction(context)
    val returnType = context.ir.symbols.enumEntries.typeWith(enumType)
    val createEnumEntriesSymbol = context.ir.symbols.createEnumEntries

    // enumEntries<SomeEnum>(SomeEnum.values)
    return context.createJvmIrBuilder(createEnumEntriesSymbol, expression).run {
      irCall(createEnumEntriesSymbol, returnType).apply {
        putTypeArgument(0, enumType)
        putValueArgument(0, irCall(enumValuesFun))
      }
    }
  }

  private val kotlinRangesUntilCallableId =
    CallableId(FqName("kotlin.ranges"), null, Name.identifier("until"))

  /** Rewrites call to rangeUntil operator to `kotlin.ranges.until` function call. */
  private fun lowerRangeUntilCall(irCall: IrCall): IrExpression {
    require(irCall.valueArgumentsCount == 1) { "invalid number of arguments" }
    val argument = irCall.valueArguments[0]!!
    val dispatchReceiver = requireNotNull(irCall.dispatchReceiver)

    val kotlinRangesUntilSymbol: IrSimpleFunctionSymbol =
      context.irPluginContext!!.referenceFunctions(kotlinRangesUntilCallableId).first {
        it.owner.extensionReceiverParameter!!.type == dispatchReceiver.type &&
          it.owner.valueParameters.size == 1 &&
          it.owner.valueParameters[0].type == argument.type
      }

    return context.createJvmIrBuilder(kotlinRangesUntilSymbol, irCall).run {
      irCall(kotlinRangesUntilSymbol, irCall.type).apply {
        extensionReceiver = dispatchReceiver
        putValueArgument(0, argument)
      }
    }
  }

  private val kotlinJvmInternalIteratorCallableId =
    CallableId(FqName("kotlin.jvm.internal"), null, Name.identifier("iterator"))

  /** Rewrite a call in the form of `arr.iterator()` into `kotlin.jvm.internal.iterator(arr)` */
  private fun lowerArrayIteratorCall(irCall: IrCall): IrExpression {
    require(irCall.valueArgumentsCount == 0) { "invalid number of arguments" }
    val dispatchReceiver = requireNotNull(irCall.dispatchReceiver)
    val dispatchReceiverType = dispatchReceiver.type as IrSimpleType
    val isPrimitive = dispatchReceiverType.isPrimitiveArray()
    val kotlinJvmInternalIterator =
      context.irPluginContext!!.referenceFunctions(kotlinJvmInternalIteratorCallableId).single {
        function ->
        function.owner.valueParameters.size == 1 &&
          with(function.owner.valueParameters[0]) {
            if (isPrimitive) {
              type == dispatchReceiverType
            } else {
              // for non-primitive cases, match the array function taking Array<T> as parameters
              type.isArray()
            }
          }
      }

    return context.createJvmIrBuilder(kotlinJvmInternalIterator, irCall).run {
      irCall(kotlinJvmInternalIterator, irCall.type).apply {
        putValueArgument(0, dispatchReceiver)
        if (!isPrimitive) {
          require(dispatchReceiverType.arguments.size == 1)
          putTypeArgument(0, dispatchReceiverType.arguments[0].typeOrFail)
        }
      }
    }
  }

  private fun lowerCompareToCall(expression: IrCall): IrExpression {
    val callee = expression.symbol.owner
    val calleeReceiver =
      checkNotNull(callee.dispatchReceiverParameter ?: callee.extensionReceiverParameter)
    val calleeParameter = callee.valueParameters.single()

    val replacementSymbol =
      when (comparisonType(calleeReceiver.type, calleeParameter.type)) {
        PrimitiveType.DOUBLE -> javaLangDoubleClass
        PrimitiveType.FLOAT -> javaLangFloatClass
        PrimitiveType.LONG -> javaLangLongClass
        PrimitiveType.CHAR -> javaLangCharacterClass
        PrimitiveType.INT -> javaLangIntegerClass
        PrimitiveType.BOOLEAN -> javaLangBooleanClass
        PrimitiveType.BYTE -> javaLangByteClass
        PrimitiveType.SHORT -> javaLangShortClass
      }.functionByName("compare")

    return context.createJvmIrBuilder(replacementSymbol, expression).run {
      irCall(replacementSymbol).apply {
        putValueArgument(
          0,
          checkNotNull(expression.dispatchReceiver ?: expression.extensionReceiver),
        )
        putValueArgument(1, expression.getValueArgument(0)!!)
      }
    }
  }

  private fun comparisonType(left: IrType, right: IrType): PrimitiveType {
    check(left.isPrimitiveType() && right.isPrimitiveType())
    return when {
      left == right -> left.getPrimitiveType()!!
      left.isDouble() || right.isDouble() -> PrimitiveType.DOUBLE
      left.isFloat() || right.isFloat() -> PrimitiveType.FLOAT
      left.isLong() || right.isLong() -> PrimitiveType.LONG
      left.isChar() || right.isChar() -> PrimitiveType.CHAR
      else -> PrimitiveType.INT
    }
  }

  private val javaLangFqName = FqName("java.lang")

  private val javaLangIntegerClass by lazy {
    context.irPluginContext!!.referenceClass(ClassId(javaLangFqName, Name.identifier("Integer")))!!
  }

  private val javaLangLongClass by lazy {
    context.irPluginContext!!.referenceClass(ClassId(javaLangFqName, Name.identifier("Long")))!!
  }

  private val javaLangDoubleClass by lazy {
    context.irPluginContext!!.referenceClass(ClassId(javaLangFqName, Name.identifier("Double")))!!
  }

  private val javaLangFloatClass by lazy {
    context.irPluginContext!!.referenceClass(ClassId(javaLangFqName, Name.identifier("Float")))!!
  }

  private val javaLangByteClass by lazy {
    context.irPluginContext!!.referenceClass(ClassId(javaLangFqName, Name.identifier("Byte")))!!
  }

  private val javaLangShortClass by lazy {
    context.irPluginContext!!.referenceClass(ClassId(javaLangFqName, Name.identifier("Short")))!!
  }

  private val javaLangBooleanClass by lazy {
    context.irPluginContext!!.referenceClass(ClassId(javaLangFqName, Name.identifier("Boolean")))!!
  }

  private val javaLangCharacterClass by lazy {
    context.irPluginContext!!.referenceClass(
      ClassId(javaLangFqName, Name.identifier("Character"))
    )!!
  }

  private val jvmIntrinsicsClass by lazy {
    context.irPluginContext!!.referenceClass(
      ClassId.topLevel(FqName("kotlin.jvm.internal.Intrinsics"))
    )!!
  }

  private val throwNoWhenBranchMatchedException by lazy {
    jvmIntrinsicsClass.functionByName("throwNoWhenBranchMatchedException")
  }

  private fun lowerNoWhenBranchMatchedException(irCall: IrCall) =
    context.createJvmIrBuilder(throwNoWhenBranchMatchedException, irCall).run {
      irCall(throwNoWhenBranchMatchedException)
    }

  private val IrCall.isEnumEntries: Boolean
    get() = intrinsics.isEnumEntries(this)

  private val IrCall.isArrayIteratorCall: Boolean
    get() = intrinsics.isArrayIterator(this)

  private val IrCall.isRangeUntilCall: Boolean
    get() = intrinsics.isRangeUntil(this)

  private val IrCall.isEnumGetEntries: Boolean
    get() = intrinsics.isEnumGetEntries(this)

  private val IrCall.isNoWhenBranchMatchedException: Boolean
    get() = intrinsics.isNoWhenBranchMatchedException(this)

  private val IrCall.isCompareTo: Boolean
    get() = intrinsics.isCompareTo(this)
}
