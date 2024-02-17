/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.jvm.InlineClassAbi
import org.jetbrains.kotlin.ir.types.IrType

// This is needed for JvmDefaultArgumentStubGenerator.kt since this class was refactored to be
// internal .
// Copied from org.jetbrains.kotlin.backend.jvm.lower.JvmDefaultArgumentFunctionFactory
internal class JvmDefaultArgumentFunctionFactory(context: CommonBackendContext) :
  MaskedDefaultArgumentFunctionFactory(context) {
  override fun IrType.hasNullAsUndefinedValue() =
    (InlineClassAbi.unboxType(this) ?: this) !in context.irBuiltIns.primitiveIrTypes
}
