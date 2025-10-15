/*
 * Copyright 2025 Google Inc.
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
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.visitors.*

/**
 * Lowers `IrLocalDelegatedProperty` nodes by extracting the delegate, getter, and setter.
 *
 * A local delegated property is a variable inside a function, where read/write access is delegated
 * to another object. For example:
 * ```
 * fun foo() {
 *   var localDelegatedProperty: String by PropertyDelegate()
 * }
 * ```
 *
 * The `IrLocalDelegatedProperty` node for `localDelegatedProperty` contains the definition of the
 * delegate instance (`PropertyDelegate()`), and the getter and setter that forward calls to that
 * delegate. Any read/write access to `localDelegatedProperty` is done through these getter and
 * setter.
 *
 * This pass rewrites the `IrLocalDelegatedProperty` node into a block of three separate statements:
 * 1. The initialization of the delegate instance.
 * 2. The getter definition.
 * 3. The setter definition.
 *
 * As a result of this lowering, the getter and setter become local functions:
 * ```
 * fun foo() {
 *   var localDelegatedProperty_delegate = PropertyDelegate()
 *   fun localDelegatedProperty_getter() = localDelegatedProperty_delegate.getValue()
 *   fun localDelegatedProperty_setter(value: String) =
 *     localDelegatedProperty_delegate.setValue(value)
 * }
 * ```
 */
internal class LocalDelegatedPropertiesLowering : IrElementTransformerVoid(), BodyLoweringPass {
  override fun lower(irBody: IrBody, container: IrDeclaration) {
    val unused = irBody.accept(this, null)
  }

  override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
    declaration.transformChildrenVoid(this)
    return IrBlockImpl(
      declaration.startOffset,
      declaration.endOffset,
      declaration.type,
      null,
      listOfNotNull(declaration.delegate, declaration.getter, declaration.setter),
    )
  }
}
