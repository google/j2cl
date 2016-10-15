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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.ast.annotations.Visitable;

/**
 * Class for an array access expression.
 */
@Visitable
public class ArrayAccess extends Expression {
  @Visitable Expression arrayExpression;
  @Visitable Expression indexExpression;

  public ArrayAccess(Expression arrayExpression, Expression indexExpression) {
    checkArgument(arrayExpression.getTypeDescriptor().isArray());

    this.arrayExpression = checkNotNull(arrayExpression);
    this.indexExpression = checkNotNull(indexExpression);
  }

  public Expression getArrayExpression() {
    return arrayExpression;
  }

  public Expression getIndexExpression() {
    return indexExpression;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return arrayExpression.getTypeDescriptor().getComponentTypeDescriptor();
  }

  @Override
  public ArrayAccess clone() {
    return new ArrayAccess(arrayExpression.clone(), indexExpression.clone());
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_ArrayAccess.visit(processor, this);
  }
}
