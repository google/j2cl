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

import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/** Represent a `switch` statement. */
class IrSwitch(
  val switchExpression: IrExpression,
  val cases: List<IrSwitchCase>,
  override val startOffset: Int,
  override val endOffset: Int,
) : IrElementBase(), IrStatement {
  override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
    return visitor.visitElement(this, data)
  }

  override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
    switchExpression.accept(visitor, data)
    cases.forEach { it.accept(visitor, data) }
  }
}

/** Represent a `case` of a `switch` statement. */
class IrSwitchCase(
  var caseExpression: IrExpression?,
  var body: IrExpression?,
  override val startOffset: Int,
  override val endOffset: Int,
) : IrElementBase() {
  override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
    return visitor.visitElement(this, data)
  }

  override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
    caseExpression?.accept(visitor, data)
    body?.accept(visitor, data)
  }
}

/**
 * Represents a `break` statement in a `switch`. We cannot reuse `IrBreak` as it is strongly linked
 * to a `IrForLoop`.
 */
class IrSwitchBreak : IrElementBase(), IrStatement {
  // IrSwitchBreak are synthetic node that does not represent anything in the source code.
  override val endOffset: Int = -1
  override val startOffset: Int = -1

  override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
    return visitor.visitElement(this, data)
  }
}
