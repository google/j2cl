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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.ast.annotations.Visitable;

import javax.annotation.Nullable;

/**
 * If Statement.
 */
@Visitable
public class IfStatement extends Statement {
  @Visitable Expression conditionExpression;
  @Visitable Statement thenStatement;
  @Visitable @Nullable Statement elseStatement;

  public IfStatement(
      Expression conditionExpression, Statement thenStatement, Statement elseStatement) {
    this.conditionExpression = checkNotNull(conditionExpression);
    this.thenStatement = checkNotNull(thenStatement);
    this.elseStatement = elseStatement;
  }

  public Expression getConditionExpression() {
    return conditionExpression;
  }

  public Statement getThenStatement() {
    return thenStatement;
  }

  public Statement getElseStatement() {
    return elseStatement;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_IfStatement.visit(processor, this);
  }
}
