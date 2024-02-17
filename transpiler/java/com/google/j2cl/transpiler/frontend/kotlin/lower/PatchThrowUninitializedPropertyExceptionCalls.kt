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
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * With the JVM backend, the `throwUninitializedPropertyAccessException()` function is defined with
 * a return type set to void instead of Nothing (the function is defined in Java) that does not
 * match our own definition of this method. In order to make the final generated JavaScript valid,
 * `throwUninitializedPropertyAccessException()` needs to return Nothing.
 *
 * This pass will patch each call to this function to change the return type to Nothing.
 */
internal class PatchThrowUninitializedPropertyExceptionCalls(
  private val context: CommonBackendContext
) : BodyLoweringPass {

  override fun lower(irBody: IrBody, container: IrDeclaration) {
    irBody.acceptChildrenVoid(
      object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
          element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
          super.visitCall(expression)

          if (expression.symbol == context.ir.symbols.throwUninitializedPropertyAccessException) {
            expression.symbol.owner.returnType = context.irBuiltIns.nothingType
          }
        }
      }
    )
  }
}
