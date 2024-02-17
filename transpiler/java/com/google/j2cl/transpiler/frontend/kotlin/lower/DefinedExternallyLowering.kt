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

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName

/** Removes the initializer for any property initialized with definedExternally. */
internal class DefinedExternallyLowering(private val context: JvmBackendContext) :
  FileLoweringPass, IrElementVisitorVoid {

  override fun lower(irFile: IrFile) {
    irFile.acceptVoid(this)
  }

  override fun visitElement(element: IrElement) {
    element.acceptChildrenVoid(this)
  }

  override fun visitField(declaration: IrField) {
    super.visitField(declaration)
    if (declaration.isInitializedToDefinedExternally()) {
      declaration.initializer = null
    }
  }
}

private val definedExternallyPropertyFqName = FqName("kotlin.js.definedExternally")

private fun IrField.isInitializedToDefinedExternally(): Boolean {
  val irCall = initializer?.expression as? IrCall ?: return false
  val correspondingProperty = irCall.symbol.owner.correspondingPropertySymbol?.owner ?: return false
  return correspondingProperty.fqName == definedExternallyPropertyFqName
}

private val IrProperty.fqName: FqName
  get() = parent.kotlinFqName.child(name)
