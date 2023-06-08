/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/**
 * Class for expression statement;
 */
@Visitable
public class ExpressionStatement extends Statement {
  @Visitable Expression expression;

  ExpressionStatement(SourcePosition sourcePosition, Expression expression) {
    super(sourcePosition);
    this.expression = checkNotNull(expression);
  }

  public Expression getExpression() {
    return expression;
  }

  @Override
  public boolean isNoop() {
    return !getExpression().hasSideEffects();
  }

  @Override
  public ExpressionStatement clone() {
    return new ExpressionStatement(getSourcePosition(), expression.clone());
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_ExpressionStatement.visit(processor, this);
  }
}
