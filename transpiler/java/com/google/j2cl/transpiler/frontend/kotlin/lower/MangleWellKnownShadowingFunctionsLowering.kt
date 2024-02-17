/*
 * Copyright 2022 Google Inc.
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
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils

/**
 * Mangles the names of functions that are known to be shadowing functions in the super types.
 *
 * Pure user-code cannot run into this situation on its own, however, there are cases where kotlinc
 * adds functions to types that can then be shadowed by user code. The primary example of this is
 * with the readonly collection types, which at compile time get mutable stub functions added on to
 * comply with the java.util collection contracts. User code that subtypes these can then
 * unintentionally shadow these stubs. See: https://youtrack.jetbrains.com/issue/KT-55562
 *
 * The "fix" here is to mangle the functions that are shadowing. In practice this means that the
 * functions will only be callable from Kotlin callers. Java callers will not have access. However,
 * the JVM behavior here was already quite strange so this is unlikely going to cause a problem in
 * practice.
 */
internal class MangleWellKnownShadowingFunctionsLowering :
  FileLoweringPass, IrElementTransformerVoid() {
  private val wellKnownShadowingFunctions =
    setOf(
      FqName(
        "kotlin.collections.RingBuffer.add"
      ), // See: https://youtrack.jetbrains.com/issue/KT-55562
      FqName("shadowing.Foo.add"), // See: b/262794329
    )

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid(this)
  }

  override fun visitFunction(declaration: IrFunction): IrStatement {
    declaration.mangleIfShadowing()
    return super.visitFunction(declaration)
  }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    expression.symbol.owner.mangleIfShadowing()
    return super.visitFunctionAccess(expression)
  }

  private fun IrFunction.mangleIfShadowing() {
    val isAlreadyMangled = name.asString().contains('$')
    if (isAlreadyMangled) return

    if (kotlinFqName in wellKnownShadowingFunctions) {
      name =
        Name.identifier(
          name.asString() + "$" + NameUtils.sanitizeAsJavaIdentifier(kotlinFqName.asString())
        )
    }
  }
}
