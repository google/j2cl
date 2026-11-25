/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.transformFlat

/**
 * Moves the field initializers of classes without primary constructor into init blocks right after
 * the fields.
 *
 * The init blocks of these classes are not lowered into their constructors but become initializer
 * blocks in the J2CL AST, which J2CL implements like for Java. Field initializers are not kept as
 * is because `BlockDecomposerLowering` extracts the ones that need to be decomposed into functions
 * that cannot access the instance.
 */
internal class J2clFieldInitializersLowering(private val context: CommonBackendContext) :
  ClassLoweringPass {
  override fun lower(irClass: IrClass) {
    if (irClass.primaryConstructor != null) {
      return
    }
    irClass.declarations.transformFlat(::moveFieldInitializerToInitBlock)
  }

  /** Returns the declaration followed by the init block its field initializer was moved to. */
  private fun moveFieldInitializerToInitBlock(declaration: IrDeclaration): List<IrDeclaration>? {
    val field = (declaration as? IrProperty)?.backingField ?: declaration as? IrField ?: return null
    val initializer = field.initializer?.takeUnless { field.isStatic } ?: return null
    field.initializer = null
    return listOf(declaration, createInitBlock(field, initializer.expression))
  }

  private fun createInitBlock(field: IrField, value: IrExpression) =
    context.irFactory
      .createAnonymousInitializer(
        value.startOffset,
        value.endOffset,
        IrDeclarationOrigin.DEFINED,
        IrAnonymousInitializerSymbolImpl(),
      )
      .apply {
        val irClass = field.parentAsClass
        body =
          context.irFactory.createBlockBody(startOffset, endOffset).apply {
            statements +=
              context.createIrBuilder(symbol, startOffset, endOffset).run {
                irSetField(irGet(irClass.thisReceiver!!), field, value)
              }
          }
        patchDeclarationParents(irClass)
      }
}
