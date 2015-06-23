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

import com.google.j2cl.ast.processors.Visitable;

/**
 * Class for instanceof Expression.
 */
@Visitable
public class InstanceOfExpression extends Expression {
  @Visitable Expression expression;
  @Visitable TypeReference testTypeRef;

  public InstanceOfExpression(Expression leftOperand, TypeReference rightOperand) {
    this.expression = leftOperand;
    this.testTypeRef = rightOperand;
  }

  public Expression getExpression() {
    return expression;
  }

  public TypeReference getTestTypeRef() {
    return testTypeRef;
  }

  public void setExpression(Expression leftOperand) {
    this.expression = leftOperand;
  }

  public void setTestTypeRef(TypeReference rightOperand) {
    this.testTypeRef = rightOperand;
  }

  @Override
  public InstanceOfExpression accept(Processor processor) {
    return Visitor_InstanceOfExpression.visit(processor, this);
  }
}
