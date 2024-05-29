/*
 * Copyright 2023 Google Inc.
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

import com.google.j2cl.transpiler.frontend.kotlin.ir.isJsEnum
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Removes super enum constructor calls and effectively empty constructors from enums.
 *
 * This will normalize the AST to better match the Java pattern, where the JLS does not permit an
 * explicit super enum constructor call (it's purely implicit).
 */
internal class EnumClassConstructorLowering(private val context: JvmBackendContext) :
  FileLoweringPass, IrElementVisitorVoid {
  override fun lower(irFile: IrFile) {
    visitElement(irFile)
  }

  override fun visitElement(element: IrElement) {
    return element.acceptChildrenVoid(this)
  }

  override fun visitClass(declaration: IrClass) {
    super.visitClass(declaration)

    if (declaration.isEnumClass) {
      declaration.constructors.forEach(this::cleanupSuperEnumConstructorCall)
      // Remove the now empty implicit constructors from JsEnum.
      if (declaration.isJsEnum) {
        declaration.declarations.removeIf { it is IrConstructor && it.isEffectivelyEmpty() }
      }
    }
  }

  private fun cleanupSuperEnumConstructorCall(irConstructor: IrConstructor) {
    val body = irConstructor.body
    if (
      body == null ||
        body.statements.isEmpty() ||
        !body.statements.first().isSuperEnumConstructorCall()
    ) {
      return
    }
    irConstructor.body =
      context.irFactory.createBlockBody(
        body.startOffset,
        body.endOffset,
        body.statements.slice(1..<body.statements.size),
      )
  }

  private fun IrStatement.isSuperEnumConstructorCall(): Boolean {
    return this is IrEnumConstructorCall &&
      symbol.owner.parentAsClass.symbol == context.irBuiltIns.enumClass
  }
}

private fun IrConstructor.isEffectivelyEmpty() =
  valueParameters.isEmpty() && annotations.isEmpty() && body.isEmpty()

private fun IrBody?.isEmpty() = this == null || statements.all { it is IrBlock && it.isEmpty() }

private fun IrBlock?.isEmpty() = this == null || statements.isEmpty()
