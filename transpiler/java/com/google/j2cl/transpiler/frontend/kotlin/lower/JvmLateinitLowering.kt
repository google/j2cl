/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.LateinitLowering
import org.jetbrains.kotlin.backend.common.lower.UninitializedPropertyAccessExceptionThrower
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrExpression

// Copied from org.jetbrains.kotlin.backend.jvm.lower.JvmLateinitLowering to change the visibility.
internal class JvmLateinitLowering(context: CommonBackendContext) :
  LateinitLowering(context, JvmUninitializedPropertyAccessExceptionThrower(context.ir.symbols)) {
  override fun transformLateinitBackingField(backingField: IrField, property: IrProperty) {
    super.transformLateinitBackingField(backingField, property)
    backingField.visibility = property.setter?.visibility ?: property.visibility
  }
}

open class JvmUninitializedPropertyAccessExceptionThrower(symbols: Symbols) :
  UninitializedPropertyAccessExceptionThrower(symbols) {
  override fun build(builder: IrBuilderWithScope, name: String): IrExpression =
    builder.irBlock { +super.build(builder, name) }
}
