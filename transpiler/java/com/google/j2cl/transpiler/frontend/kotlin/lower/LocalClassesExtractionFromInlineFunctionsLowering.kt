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

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.LocalClassPopupLowering
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

// Copied and modified from org/jetbrains.kotlin/backend/common/lower/inline/LocalClasses.kt
// TODO(b/452060167): The original `LocalClasses.kt` has been removed in Kotlin 2.2.20. Investigate
// the reason for its removal and identify any potential replacements.

internal class LocalClassesInInlineFunctionsLowering(val context: LoweringContext) :
  BodyLoweringPass {
  override fun lower(irFile: IrFile) {
    runOnFilePostfix(irFile)
  }

  override fun lower(irBody: IrBody, container: IrDeclaration) {
    val function = container as? IrFunction ?: return
    val classesToExtract = mutableSetOf<IrClass>()
    function.collectExtractableLocalClassesInto(classesToExtract)
    if (classesToExtract.isEmpty()) return

    LocalDeclarationsLowering(context).lower(function, function, classesToExtract)
  }
}

internal class LocalClassesExtractionFromInlineFunctionsLowering(context: LoweringContext) :
  LocalClassPopupLowering(context) {

  private val classesToExtract = mutableSetOf<IrClass>()

  override fun lower(irBody: IrBody, container: IrDeclaration) {
    val function = container as? IrFunction ?: return

    function.collectExtractableLocalClassesInto(classesToExtract)
    if (classesToExtract.isEmpty()) return
    println(" Found local classes in inline function: ${function.render()}:")
    classesToExtract.forEach { println("  ${it.render()}") }
    super.lower(irBody, container)

    classesToExtract.clear()
  }

  override fun shouldPopUp(klass: IrClass, currentScope: ScopeWithIr?): Boolean {
    return classesToExtract.contains(klass)
  }
}

private fun IrFunction.collectExtractableLocalClassesInto(classesToExtract: MutableSet<IrClass>) {
  // MODIFIED BY GOOGLE.
  // Back-off hoisting local classes out of functions that contain type parameters. Normally
  // kotlinc backs-off only if they're reified so that they can be resolved/replaced at the
  // call site. However since we're currently not erasing type parameters for inline functions
  // which effectively makes all inline type parameters reified.
  // TODO(b/274670726): Remove this behavior when we properly erase type parameters.
  // original code:
  // if (!isInline) return
  // // Conservatively assume that functions with reified type parameters must be copied.
  // if (typeParameters.any { it.isReified }) return
  if (!isInline || typeParameters.isNotEmpty()) return
  // END OF MODIFICATION.

  val crossinlineParameters = parameters.filter { it.isCrossinline }.toSet()

  acceptChildrenVoid(
    object : IrVisitorVoid() {
      override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
      }

      override fun visitClass(declaration: IrClass) {
        var canExtract = true
        if (crossinlineParameters.isNotEmpty()) {
          declaration.acceptVoid(
            object : IrVisitorVoid() {
              override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
              }

              override fun visitGetValue(expression: IrGetValue) {
                if (expression.symbol.owner in crossinlineParameters) canExtract = false
              }
            }
          )
        }
        if (canExtract) classesToExtract.add(declaration)
      }
    }
  )
}
