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
 * While Statement.
 */
@Visitable
public class WhileStatement extends Statement {
  @Visitable Expression conditionExpression;
  @Visitable Statement body;

  public WhileStatement(Expression conditionExpression, Statement body) {
    Preconditions.checkNotNull(conditionExpression);
    Preconditions.checkNotNull(body);
    this.conditionExpression = conditionExpression;
    this.body = body;
  }

  public Expression getConditionExpression() {
    return conditionExpression;
  }

  public Statement getBody() {
    return body;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_WhileStatement.visit(processor, this);
  }
}
