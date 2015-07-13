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
package com.google.j2cl.ast;

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.processors.Visitable;

/**
 * Ternary expression.
 */
@Visitable
public class TernaryExpression extends Expression {
  @Visitable Expression conditionExpression;
  @Visitable Expression trueExpression;
  @Visitable Expression falseExpression;

  public TernaryExpression(
      Expression conditionExpression, Expression trueExpression, Expression falseExpression) {
    Preconditions.checkNotNull(conditionExpression);
    Preconditions.checkNotNull(trueExpression);
    Preconditions.checkNotNull(falseExpression);
    this.conditionExpression = conditionExpression;
    this.trueExpression = trueExpression;
    this.falseExpression = falseExpression;
  }

  public Expression getConditionExpression() {
    return conditionExpression;
  }

  public Expression getTrueExpression() {
    return trueExpression;
  }

  public Expression getFalseExpression() {
    return falseExpression;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_TernaryExpression.visit(processor, this);
  }
}
