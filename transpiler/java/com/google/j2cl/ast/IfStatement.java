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

import javax.annotation.Nullable;

/**
 * If Statement.
 */
@Visitable
public class IfStatement extends Statement {
  @Visitable Expression conditionExpression;
  @Visitable Block trueBlock;
  @Visitable @Nullable Block falseBlock;

  public IfStatement(Expression conditionExpression, Block trueBlock, Block falseBlock) {
    Preconditions.checkNotNull(conditionExpression);
    Preconditions.checkNotNull(trueBlock);
    this.conditionExpression = conditionExpression;
    this.trueBlock = trueBlock;
    this.falseBlock = falseBlock;
  }

  public Expression getConditionExpression() {
    return conditionExpression;
  }

  public Block getTrueBlock() {
    return trueBlock;
  }

  public Block getFalseBlock() {
    return falseBlock;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_IfStatement.visit(processor, this);
  }
}
