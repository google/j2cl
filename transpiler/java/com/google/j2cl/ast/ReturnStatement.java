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
 * Class for return statement.
 */
@Visitable
public class ReturnStatement extends Statement {
  @Visitable @Nullable Expression expression;
  private final TypeDescriptor returnTypeDescriptor;

  public ReturnStatement(Expression expression, TypeDescriptor returnTypeDescriptor) {
    this.expression = expression;
    this.returnTypeDescriptor = checkNotNull(returnTypeDescriptor);
  }

  /**
   * Returns the type descriptor of the type expected to be returned by this statement.
   */
  public TypeDescriptor getTypeDescriptor() {
    return returnTypeDescriptor;
  }

  public Expression getExpression() {
    return expression;
  }

  @Override
  public ReturnStatement clone() {
    ReturnStatement returnStatement =
        new ReturnStatement(AstUtils.clone(expression), returnTypeDescriptor);
    returnStatement.setSourcePosition(this.getSourcePosition());
    return returnStatement;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_ReturnStatement.visit(processor, this);
  }
}
