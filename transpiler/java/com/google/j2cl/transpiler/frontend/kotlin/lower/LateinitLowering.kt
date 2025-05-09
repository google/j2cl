/*
 * Copyright 2010-2017 JetBrains s.r.o.
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.atMostOne

// This is based on org.jetbrains.kotlin.backend.common.lower.LateinitLowering but has been heavily
// modified. Major changes include:
//  - we lower to a checkInitialized(value) helper function that can be have its checking disabled.
//  - private properties are inlined directly to all callsites. This would normally be done by
//    by JvmPropertiesLowering, but we use our checkInitialized(value) instead.
internal class LateinitLowering(private val context: J2clBackendContext) :
  FileLoweringPass, IrElementTransformerVoid() {
  private val visitedLateinitVariables = mutableSetOf<IrVariable>()

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid(this)
  }

  fun lower(irBody: IrBody) {
    irBody.transformChildrenVoid(this)
  }

  override fun visitProperty(declaration: IrProperty): IrStatement {
    if (declaration.isRealLateinit()) {
      val backingField = declaration.backingField!!
      if (!backingField.type.isMarkedNullable()) {
        transformLateinitBackingField(backingField, declaration)
        declaration.getter?.let { transformGetter(backingField, it) }
      }
    }

    declaration.transformChildrenVoid()
    return declaration
  }

  override fun visitVariable(declaration: IrVariable): IrStatement {
    declaration.transformChildrenVoid()

    if (declaration.isLateinit && !declaration.type.isMarkedNullable()) {
      visitedLateinitVariables += declaration
      declaration.type = declaration.type.makeNullable()
      declaration.isVar = true
      declaration.initializer =
        IrConstImpl.constNull(
          declaration.startOffset,
          declaration.endOffset,
          context.irBuiltIns.nothingNType,
        )
    }

    return declaration
  }

  override fun visitGetValue(expression: IrGetValue): IrExpression {
    expression.transformChildrenVoid()

    val irValue = expression.symbol.owner
    if (irValue !is IrVariable || irValue !in visitedLateinitVariables) {
      return expression
    }

    return context
      .createIrBuilder(
        (irValue.parent as IrSymbolOwner).symbol,
        expression.startOffset,
        expression.endOffset,
      )
      .run {
        irCall(checkInitializedSymbol).apply {
          putValueArgument(0, irGet(irValue))
          typeArguments[0] = irValue.type.makeNotNull()
        }
      }
  }

  override fun visitGetField(expression: IrGetField): IrExpression {
    expression.transformChildrenVoid()

    val irField = expression.symbol.owner
    if (irField.isLateinitBackingField()) {
      expression.type = expression.type.makeNullable()
    }
    return expression
  }

  private fun IrField.isLateinitBackingField(): Boolean {
    val property = this.correspondingPropertySymbol?.owner
    return property != null && property.isRealLateinit()
  }

  private fun IrProperty.isRealLateinit() = isLateinit && !isFakeOverride

  override fun visitCall(expression: IrCall): IrExpression {
    expression.transformChildrenVoid()

    return when {
      Symbols.isLateinitIsInitializedPropertyGetter(expression.symbol) ->
        rewriteIsInitialized(expression)
      expression.isPrivateLateinitAccessor() -> rewriteLateinitGetterAccessor(expression)
      else -> expression
    }
  }

  private fun rewriteIsInitialized(expression: IrCall): IrExpression =
    expression.arguments[0]!!.replaceTailExpression {
      val (property, dispatchReceiver) =
        when (it) {
          is IrPropertyReference ->
            it.getter?.owner?.resolveFakeOverride()?.correspondingPropertySymbol?.owner to
              it.dispatchReceiver
          is IrRichPropertyReference ->
            (it.reflectionTargetSymbol as? IrPropertySymbol)?.owner?.resolveFakeOverride() to
              it.boundValues.atMostOne()
          else -> error("Unsupported argument for KProperty::isInitialized call: ${it.render()}")
        }
      require(property?.isLateinit == true) {
        "isInitialized invoked on non-lateinit property ${property?.render()}"
      }
      val backingField =
        property.backingField
          ?: throw AssertionError("Lateinit property is supposed to have a backing field")
      transformLateinitBackingField(backingField, property)
      context.createIrBuilder(property.symbol, expression.startOffset, expression.endOffset).run {
        irNotEquals(irGetField(dispatchReceiver, backingField), irNull())
      }
    }

  private fun rewriteLateinitGetterAccessor(expression: IrCall): IrExpression {
    val backingField = expression.symbol.owner.correspondingPropertySymbol!!.owner.backingField!!
    val type = backingField.type
    return context
      .createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset)
      .run {
        irCall(checkInitializedSymbol, expression.type).apply {
          putValueArgument(0, irGetField(expression.dispatchReceiver!!, backingField, type))
          typeArguments[0] = expression.type
        }
      }
  }

  private fun transformLateinitBackingField(backingField: IrField, property: IrProperty) {
    assert(backingField.initializer == null) {
      "lateinit property backing field should not have an initializer:\n${property.dump()}"
    }
    backingField.type = backingField.type.makeNullable()
    backingField.visibility = property.setter?.visibility ?: property.visibility
  }

  private fun transformGetter(backingField: IrField, getter: IrFunction) {
    val type = backingField.type
    assert(!type.isPrimitiveType()) {
      "'lateinit' property type should not be primitive:\n${backingField.dump()}"
    }
    val startOffset = getter.startOffset
    val endOffset = getter.endOffset

    getter.body =
      context.createIrBuilder(getter.symbol, startOffset, endOffset).run {
        irBlockBody {
          +irReturn(
            irCall(checkInitializedSymbol, type.makeNotNull()).apply {
              putValueArgument(
                0,
                irGetField(getter.dispatchReceiverParameter?.let { irGet(it) }, backingField, type),
              )
              typeArguments[0] = type.makeNotNull()
            }
          )
        }
      }
  }

  private val jvmIntrinsicsClass by lazy {
    context.jvmBackendContext.irPluginContext!!.referenceClass(
      ClassId.topLevel(FqName("kotlin.jvm.internal.Intrinsics"))
    )!!
  }

  private val checkInitializedSymbol by lazy {
    jvmIntrinsicsClass.functionByName("checkInitialized")
  }
}

private inline fun IrExpression.replaceTailExpression(
  crossinline transform: (IrExpression) -> IrExpression
): IrExpression {
  var current = this
  var block: IrContainerExpression? = null
  while (current is IrContainerExpression) {
    block = current
    current = current.statements.last() as IrExpression
  }
  current = transform(current)
  if (block == null) {
    return current
  }
  block.statements[block.statements.size - 1] = current
  return this
}

private fun IrCall.isPrivateLateinitAccessor(): Boolean {
  val accessor = symbol.owner.takeIf { it.isGetter } ?: return false
  val property = accessor.correspondingPropertySymbol?.owner ?: return false
  return property.isLateinit && DescriptorVisibilities.isPrivate(property.visibility)
}
