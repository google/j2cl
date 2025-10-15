/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor

/** Represent a for-each loop. */
class IrForLoop(
  override var startOffset: Int,
  override var endOffset: Int,
  override var type: IrType,
  override var origin: IrStatementOrigin?,
) : IrLoop() {
  override lateinit var condition: IrExpression
  override var body: IrExpression? = null
  lateinit var initializers: MutableList<IrVariable>
  lateinit var updates: MutableList<IrExpression>
  override var label: String? = null
  override var attributeOwnerId: IrElement = this

  override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R = visitor.visitLoop(this, data)

  override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
    initializers.forEach { it.accept(visitor, data) }
    condition.accept(visitor, data)
    updates.forEach { it.accept(visitor, data) }
    body?.accept(visitor, data)
  }

  override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
    initializers.transformInPlace(transformer, data)
    condition = condition.transform(transformer, data)
    updates.transformInPlace(transformer, data)
    body = body?.transform(transformer, data)
  }

  companion object {
    fun from(loop: IrLoop) =
      IrForLoop(loop.startOffset, loop.endOffset, loop.type, loop.origin).also {
        it.label = loop.label
      }
  }
}
