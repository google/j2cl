/*
 * Copyright 2016 Google Inc.
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
import com.google.j2cl.ast.processors.common.Processor;

/** A node that represent a closure field declaration "@public {type}". */
@Visitable
public class JsDocFieldDeclaration extends Expression {
  @Visitable Expression expression;
  private final TypeDescriptor fieldType;
  private final boolean isPublic;
  private final boolean isConst;
  private final boolean isDeprecated;

  private JsDocFieldDeclaration(
      Expression expression,
      TypeDescriptor fieldType,
      boolean isPublic,
      boolean isConst,
      boolean isDeprecated) {
    checkArgument(!(expression instanceof JsDocFieldDeclaration));
    this.expression = checkNotNull(expression);
    this.fieldType = checkNotNull(fieldType);
    this.isPublic = isPublic;
    this.isConst = isConst;
    this.isDeprecated = isDeprecated;
    checkArgument(
        expression instanceof FieldAccess
            || (expression instanceof BinaryExpression
            && ((BinaryExpression) expression).getOperator() == BinaryOperator.ASSIGN),
        "Declaration annotations can only applied to assignments and field references.");
  }

  public Expression getExpression() {
    return expression;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return fieldType;
  }

  @Override
  public Precedence getPrecedence() {
    // These are always emitted directly in a statement, their precedence should never be required.
    throw new UnsupportedOperationException();
  }

  public boolean isPublic() {
    return isPublic;
  }

  public boolean isConst() {
    return isConst;
  }

  public boolean isDeprecated() {
    return isDeprecated;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_JsDocFieldDeclaration.visit(processor, this);
  }

  @Override
  public JsDocFieldDeclaration clone() {
    return new JsDocFieldDeclaration(
        expression.clone(), fieldType, isPublic, isConst, isDeprecated);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for JsDocFieldDeclaration */
  public static class Builder {
    private Expression expression;
    private TypeDescriptor fieldType;
    private boolean isPublic;
    private boolean isConst;
    private Boolean isDeprecated;

    public static Builder from(JsDocFieldDeclaration annotation) {
      Builder builder = new Builder();
      builder.expression = annotation.getExpression();
      builder.fieldType = annotation.getTypeDescriptor();
      builder.isPublic = annotation.isPublic();
      builder.isConst = annotation.isConst();
      builder.isDeprecated = annotation.isDeprecated();
      return builder;
    }

    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    public Builder setFieldType(TypeDescriptor fieldType) {
      this.fieldType = fieldType;
      return this;
    }

    public Builder setPublic(boolean isPublic) {
      this.isPublic = isPublic;
      return this;
    }

    public Builder setConst(boolean isConst) {
      this.isConst = isConst;
      return this;
    }

    public Builder setDeprecated(boolean isDeprecated) {
      this.isDeprecated = isDeprecated;
      return this;
    }

    public JsDocFieldDeclaration build() {
      return new JsDocFieldDeclaration(expression, fieldType, isPublic, isConst, isDeprecated);
    }
  }
}
