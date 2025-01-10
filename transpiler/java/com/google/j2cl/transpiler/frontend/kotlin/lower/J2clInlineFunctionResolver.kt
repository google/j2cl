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

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal class J2clInlineFunctionResolver(private val context: CommonBackendContext) :
  InlineFunctionResolver(InlineMode.ALL_INLINE_FUNCTIONS) {

  override fun shouldExcludeFunctionFromInlining(symbol: IrFunctionSymbol): Boolean {
    return super.shouldExcludeFunctionFromInlining(symbol) ||
      // arrayOf functions are inline intrinsic functions in the jvm stdlib and should not be
      // inlined.
      // The calls to these functions are directly handled by our CompilationUnitBuilder.
      // TODO(b/256856926): Remove this code when arrayOf() functions are no longer inlineable.
      symbol.owner.isArrayOf() ||
      // String?.plus() functions are inline intrinsic functions in the stdlib and should not be
      // inlined. The calls to these functions are directly handled by our CompilationUnitBuilder.
      // TODO(b/256856926): Remove this code when String?.plus() function is no longer inlineable.
      symbol.isExtensionStringPlus
  }

  private val IrFunctionSymbol.isExtensionStringPlus: Boolean
    get() = this == context.irBuiltIns.extensionStringPlus
}

private val PRIMITIVE_ARRAY_OF_NAMES: Set<String> =
  (PrimitiveType.values().map { type -> type.name } +
      UnsignedType.values().map { type -> type.typeName.asString() })
    .map { name -> name.toLowerCaseAsciiOnly() + "ArrayOf" }
    .toSet()

private const val ARRAY_OF_NAME = "arrayOf"

private fun IrFunction.isArrayOf(): Boolean {
  val parent =
    when (val directParent = parent) {
      is IrClass -> directParent.getPackageFragment() ?: return false
      is IrPackageFragment -> directParent
      else -> return false
    }
  return parent.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME &&
    name.asString().let { it in PRIMITIVE_ARRAY_OF_NAMES || it == ARRAY_OF_NAME } &&
    extensionReceiverParameter == null &&
    dispatchReceiverParameter == null &&
    valueParameters.size == 1 &&
    valueParameters[0].isVararg
}
