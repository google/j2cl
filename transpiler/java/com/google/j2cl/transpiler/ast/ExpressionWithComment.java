/*
 * Copyright 2018 Google Inc.
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

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import com.google.j2cl.transpiler.ast.Expression.Precedence;

/** A node that represent an expression that will be emitted with an explaining comment. */
@Visitable
public class ExpressionWithComment extends Expression {
  @Visitable Expression expression;
  private final String comment;

  ExpressionWithComment(Expression expression, String comment) {
    this.expression = checkNotNull(expression);
    this.comment = checkNotNull(comment);
  }

  public Expression getExpression() {
    return expression;
  }

  public String getComment() {
    return comment;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return expression.getTypeDescriptor();
  }

  @Override
  public boolean isEffectivelyInvariant() {
    return expression.isEffectivelyInvariant();
  }

  @Override
  public boolean isCompileTimeConstant() {
    return expression.isCompileTimeConstant();
  }

  @Override
  public Precedence getPrecedence() {
    // The added comment does not affect the precedence of the underlying expression.
    return expression.getPrecedence();
  }

  @Override
  public boolean canBeNull() {
    return expression.canBeNull();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_ExpressionWithComment.visit(processor, this);
  }

  @Override
  public ExpressionWithComment clone() {
    return new ExpressionWithComment(expression.clone(), comment);
  }
}
