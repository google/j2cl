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

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/** An AST represention of `for (v in iterable)`. */
class IrForInLoop(
  override val startOffset: Int,
  override val endOffset: Int,
  override var type: IrType,
  val variable: IrVariable,
  override var condition: IrExpression,
) : IrLoop() {
  override var origin: IrStatementOrigin? = IrStatementOrigin.FOR_LOOP
  override var body: IrExpression? = null
  override var attributeOwnerId: IrAttributeContainer = this
  override var originalBeforeInline: IrAttributeContainer? = null
  override var label: String? = null

  override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
    visitor.visitLoop(this, data)

  override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
    variable.accept(visitor, data)
    condition.accept(visitor, data)
    body?.accept(visitor, data)
  }

  override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
    variable.transformChildren(transformer, data)
    condition.transformChildren(transformer, data)
    body?.transformChildren(transformer, data)
  }
}
