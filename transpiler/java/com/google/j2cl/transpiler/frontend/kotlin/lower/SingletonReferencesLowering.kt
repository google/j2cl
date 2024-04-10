/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.* // ktlint-disable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * A pass that replaces references to object classess by references to static fields holding the
 * unique instance of the class.
 *
 * Based on org.jetbrains.kotlin.ir.backend.jvm.lower.SingletonReferencesLowering.kt
 */
internal class SingletonReferencesLowering(val context: JvmBackendContext) :
  FileLoweringPass, IrElementTransformerVoidWithContext() {

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid(this)
  }

  override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
    val instanceField =
      context.cachedDeclarations.getFieldForObjectInstance(expression.symbol.owner)
    return IrGetFieldImpl(
      expression.startOffset,
      expression.endOffset,
      instanceField.symbol,
      expression.type,
    )
  }
}
