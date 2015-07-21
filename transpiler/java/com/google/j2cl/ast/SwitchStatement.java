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

import java.util.ArrayList;
import java.util.List;

/**
 * Switch Statement.
 */
@Visitable
public class SwitchStatement extends Statement {
  @Visitable Expression matchExpression;
  @Visitable List<Statement> bodyStatements = new ArrayList<>();

  public SwitchStatement(Expression matchExpression, List<Statement> bodyStatements) {
    Preconditions.checkNotNull(matchExpression);
    Preconditions.checkNotNull(bodyStatements);
    this.matchExpression = matchExpression;
    this.bodyStatements.addAll(bodyStatements);
  }

  public Expression getMatchExpression() {
    return matchExpression;
  }

  public List<Statement> getBodyStatements() {
    return bodyStatements;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_SwitchStatement.visit(processor, this);
  }
}
