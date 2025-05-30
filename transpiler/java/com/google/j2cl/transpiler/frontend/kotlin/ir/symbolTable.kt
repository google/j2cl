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

package com.google.j2cl.transpiler.frontend.kotlin.ir

import java.lang.IllegalArgumentException
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.fir.backend.DelicateDeclarationStorageApi
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler.isExported
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.types.expressions.OperatorConventions

// TODO(b/374966022): Remove this file once we don't rely on IR serialization anymore for inlining.
@OptIn(DelicateDeclarationStorageApi::class, InternalSymbolFinderAPI::class)
fun SymbolTable.populate(irBuiltIns: IrBuiltIns) {
  val irSignaturer = PublicIdSignatureComputer(JvmIrMangler)
  // collects all existing bound symbols in componentStorage.
  val componentsStorage = irBuiltIns.anyClass.owner as Fir2IrComponents
  componentsStorage.classifierStorage.forEachCachedDeclarationSymbol { addSymbol(it, irSignaturer) }
  componentsStorage.declarationStorage.forEachCachedDeclarationSymbol {
    addSymbol(it, irSignaturer)
  }

  // add the builtins so the IrDeserializer does not create new symbol for them.
  with(irBuiltIns) {
    // Add symbols of numeric conversion function (toInt, toDouble...) as they are used in
    // checks in NumericConversionLowering.
    fun getNumericConversionsFunctionSymbols(): List<IrSymbol> {
      val symbols = arrayListOf<IrSymbol>()
      for (type in PrimitiveType.NUMBER_TYPES) {
        val typeClass = symbolFinder.findClass(type.typeName)!!
        for (name in OperatorConventions.NUMBER_CONVERSIONS) {
          symbolFinder.findBuiltInClassMemberFunctions(typeClass, name).singleOrNull()?.let {
            symbols.add(it)
          }
        }
      }
      return symbols
    }

    buildList {
        addAll(lessFunByOperandType.values)
        addAll(lessOrEqualFunByOperandType.values)
        addAll(greaterFunByOperandType.values)
        addAll(greaterOrEqualFunByOperandType.values)
        addAll(ieee754equalsFunByOperandType.values)
        add(eqeqeqSymbol)
        add(eqeqSymbol)
        add(throwCceSymbol)
        add(throwIseSymbol)
        add(andandSymbol)
        add(ororSymbol)
        add(noWhenBranchMatchedExceptionSymbol)
        add(illegalArgumentExceptionSymbol)
        add(dataClassArrayMemberHashCodeSymbol)
        add(dataClassArrayMemberToStringSymbol)
        add(checkNotNullSymbol)
        addAll(getNumericConversionsFunctionSymbols())
      }
      .forEach { addSymbol(it, irSignaturer) }
  }
}

fun SymbolTable.addSymbolsRecursively(symbol: IrSymbol, irSignaturer: PublicIdSignatureComputer) {
  addSymbol(symbol, irSignaturer)

  val children = (symbol.owner as? IrDeclarationContainer)?.declarations ?: emptyList()
  for (declaration in children) {
    addSymbolsRecursively(declaration.symbol, irSignaturer)
  }
}

@Suppress("IMPLICIT_CAST_TO_ANY")
private fun SymbolTable.addSymbol(symbol: IrSymbol, irSignaturer: PublicIdSignatureComputer) {
  if (!symbol.isBound) {
    return
  }
  val declaration =
    symbol.owner as? IrDeclaration ?: throw IllegalArgumentException("Not an IrDeclaration $symbol")
  if (!declaration.isExported(false)) {
    // These declaration are declared in an anonymous or local context and cannot be referenced
    // outside if this context.
    return
  }

  val signature =
    symbol.signature
      ?: irSignaturer.inFile((declaration.fileOrNull)?.symbol) {
        irSignaturer.computeSignature(declaration)
      }
  val unused =
    when (symbol) {
      is IrClassSymbol -> declareClassIfNotExists(signature, { symbol }, { it.owner })
      is IrTypeAliasSymbol -> declareTypeAliasIfNotExists(signature, { symbol }, { it.owner })
      is IrEnumEntrySymbol -> declareEnumEntry(signature, { symbol }, { it.owner })
      is IrSimpleFunctionSymbol ->
        declareSimpleFunctionIfNotExists(signature, { symbol }, { it.owner })
      is IrConstructorSymbol -> declareConstructorIfNotExists(signature, { symbol }, { it.owner })
      is IrPropertySymbol -> declarePropertyIfNotExists(signature, { symbol }, { it.owner })
      is IrFieldSymbol -> declareField(signature, { symbol }, { it.owner })
      else -> throw AssertionError("Unexpected symbol $symbol")
    }
}
