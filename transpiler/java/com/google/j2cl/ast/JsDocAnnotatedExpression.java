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

/**
 * A node that represent a closure type cast "@type {type}" or a closure field declaration "@public
 * {type}".
 */
@Visitable
public class JsDocAnnotatedExpression extends Expression {
  @Visitable Expression expression;
  @Visitable TypeDescriptor annotationType;
  private boolean isDeclaration;

  private JsDocAnnotatedExpression(
      Expression expression, TypeDescriptor annotationType, boolean isDeclaration) {
    checkArgument(!(expression instanceof JsDocAnnotatedExpression));
    this.expression = checkNotNull(expression);
    this.annotationType = checkNotNull(annotationType);
    this.isDeclaration = isDeclaration;
  }

  public Expression getExpression() {
    return expression;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return annotationType;
  }

  public boolean isDeclaration() {
    return isDeclaration;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_JsDocAnnotatedExpression.visit(processor, this);
  }

  @Override
  public JsDocAnnotatedExpression clone() {
    return new JsDocAnnotatedExpression(expression.clone(), annotationType, isDeclaration);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for JsDocAnnotatedExpressions */
  public static class Builder {
    private Expression expression;
    private TypeDescriptor annotationType;
    private boolean isDeclaration;

    public static Builder from(JsDocAnnotatedExpression annotation) {
      Builder builder = new Builder();
      builder.expression = annotation.getExpression();
      builder.annotationType = annotation.getTypeDescriptor();
      builder.isDeclaration = annotation.isDeclaration();
      return builder;
    }

    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    public Builder setAnnotationType(TypeDescriptor annotationType) {
      this.annotationType = annotationType;
      return this;
    }

    public Builder setDeclaration(boolean isDeclaration) {
      this.isDeclaration = isDeclaration;
      return this;
    }

    public JsDocAnnotatedExpression build() {
      checkArgument(
          !isDeclaration
              || (expression instanceof BinaryExpression
                  && ((BinaryExpression) expression).getOperator() == BinaryOperator.ASSIGN),
          "Declaration annotations can only applied to assignments.");
      return new JsDocAnnotatedExpression(
          // Avoid pointlessly nesting type annotations.
          AstUtils.removeTypeAnnotationIfPresent(expression), annotationType, isDeclaration);
    }
  }
}
