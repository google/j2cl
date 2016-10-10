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

import com.google.j2cl.ast.annotations.Visitable;

/** A node that represent a type annotation in the Javascript output. */
@Visitable
public class JsDocAnnotatedExpression extends Expression {
  @Visitable Expression expression;
  @Visitable TypeDescriptor annotationType;
  private boolean isDeclaration;

  private JsDocAnnotatedExpression(
      Expression expression, TypeDescriptor annotationType, boolean isDeclaration) {
    checkArgument(!(expression instanceof JsDocAnnotatedExpression));
    this.expression = expression;
    this.annotationType = annotationType;
    this.isDeclaration = isDeclaration;
  }

  /** Creates a "@type {type}" cast on an expression. */
  public static JsDocAnnotatedExpression createCastAnnotatedExpression(
      Expression expression, TypeDescriptor type) {
    if (expression instanceof JsDocAnnotatedExpression) {
      JsDocAnnotatedExpression annotation = (JsDocAnnotatedExpression) expression;
      return new JsDocAnnotatedExpression(annotation.getExpression(), type, false);
    }
    return new JsDocAnnotatedExpression(expression, type, false);
  }

  /**
   * Creates a "@public {type}" cast on an assignment.
   *
   * @param expression Must be an assignment expression.
   */
  public static JsDocAnnotatedExpression createDeclarationAnnotatedExpression(
      Expression expression, TypeDescriptor type) {
    String message = "Declaration annotations can only applied to assignments.";
    checkArgument(expression instanceof BinaryExpression, message);
    checkArgument(((BinaryExpression) expression).getOperator() == BinaryOperator.ASSIGN, message);
    return new JsDocAnnotatedExpression(expression, type, true);
  }

  public Expression getExpression() {
    return expression;
  }

  public boolean isDeclaration() {
    return isDeclaration;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_JsDocAnnotatedExpression.visit(processor, this);
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return annotationType;
  }

  @Override
  public JsDocAnnotatedExpression clone() {
    return new JsDocAnnotatedExpression(expression.clone(), annotationType, isDeclaration);
  }

  /** A Builder for easily and correctly creating modified versions of CastExpressions. */
  public static class Builder {
    private Expression expression;
    private TypeDescriptor annotationType;
    private boolean isDeclaration;

    public static Builder from(JsDocAnnotatedExpression annotation) {
      Builder builder = new Builder();
      checkArgument(!(annotation.getExpression() instanceof JsDocAnnotatedExpression));
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

    public JsDocAnnotatedExpression build() {
      return new JsDocAnnotatedExpression(expression, annotationType, isDeclaration);
    }
  }
}
