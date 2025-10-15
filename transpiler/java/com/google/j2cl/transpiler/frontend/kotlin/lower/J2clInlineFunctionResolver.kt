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

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal class J2clInlineFunctionResolver(private val context: J2clBackendContext) :
  InlineFunctionResolver() {

  override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
    return symbol.owner.resolveFakeOverrideOrSelf().takeIf { it.isInline }
  }

  override fun shouldSkipBecauseOfCallSite(expression: IrFunctionAccessExpression): Boolean {
    return super.shouldSkipBecauseOfCallSite(expression) ||
      // arrayOf functions are inline intrinsic functions in the jvm stdlib and should not be
      // inlined. The calls to these functions are directly handled by our CompilationUnitBuilder.
      context.intrinsics.isArrayOf(expression.symbol) ||
      // emptyArray is an inline builtin function in stdlib and should not be inlined.
      // The calls to this function are directly handled by our EmptyArrayLowering pass.
      context.intrinsics.isEmptyArray(expression.symbol) ||
      // The `coroutineContext` getter is an inline intrinsic function.
      // In Kotlin/JVM, this is replaced at code generation time.
      // In Kotlin/JS, calls to this inline function are mapped to a top-level inline suspend
      // function `getCoroutineContext`, which is then inlined.
      // J2CL cannot easily adopt the Kotlin/JS approach. The top-level replacement function would
      // need to be attached to a FileClass,
      // which would require copying logic from the ExternalPackageParentPatcherLowering pass.
      // Thus, we opt to skip inlining the getter here and instead lower the call later.
      expression.symbol.isCoroutineContextGetter()
  }

  private val IrFunctionSymbol.isExtensionStringPlus: Boolean
    get() = this == context.irBuiltIns.extensionStringPlus

  private fun IrFunctionSymbol.isCoroutineContextGetter(): Boolean =
    context.intrinsics.isCoroutineContextGetterCall(this)
}

private val PRIMITIVE_ARRAY_OF_NAMES: Set<String> =
  (PrimitiveType.values().map { type -> type.name } +
      UnsignedType.values().map { type -> type.typeName.asString() })
    .map { name -> name.toLowerCaseAsciiOnly() + "ArrayOf" }
    .toSet()

private const val ARRAY_OF_NAME = "arrayOf"
