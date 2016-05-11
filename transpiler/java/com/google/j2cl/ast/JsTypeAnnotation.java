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

import com.google.j2cl.ast.processors.Visitable;

/**
 * A node that represent a type annotation in the Javascript output.
 */
@Visitable
public class JsTypeAnnotation extends Expression {
  @Visitable Expression expression;
  @Visitable TypeDescriptor annotationType;
  private boolean isDeclaration;

  private JsTypeAnnotation(
      Expression expression, TypeDescriptor annotationType, boolean isDeclaration) {
    checkArgument(!(expression instanceof JsTypeAnnotation));
    this.expression = expression;
    this.annotationType = annotationType;
    this.isDeclaration = isDeclaration;
  }

  /**
   * Creates a "@type {type}" cast on an expression.
   */
  public static JsTypeAnnotation createTypeAnnotation(Expression expression, TypeDescriptor type) {
    if (expression instanceof JsTypeAnnotation) {
      JsTypeAnnotation annotation = (JsTypeAnnotation) expression;
      return new JsTypeAnnotation(annotation.getExpression(), type, false);
    }
    return new JsTypeAnnotation(expression, type, false);
  }

  /**
   * Creates a "@public {type}" cast on an assignment.
   * @param expression Must be an assignment expression.
   */
  public static JsTypeAnnotation createDeclarationAnnotation(
      Expression expression, TypeDescriptor type) {
    String message = "Declaration annotations can only applied to assignments.";
    checkArgument(expression instanceof BinaryExpression, message);
    checkArgument(((BinaryExpression) expression).getOperator() == BinaryOperator.ASSIGN, message);
    return new JsTypeAnnotation(expression, type, true);
  }

  public Expression getExpression() {
    return expression;
  }

  public boolean isDeclaration() {
    return isDeclaration;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_JsTypeAnnotation.visit(processor, this);
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return annotationType;
  }

  /**
   * A Builder for easily and correctly creating modified versions of CastExpressions.
   */
  public static class Builder {
    private Expression expression;
    private TypeDescriptor annotationType;
    private boolean isDeclaration;

    public static Builder from(JsTypeAnnotation annotation) {
      Builder builder = new Builder();
      checkArgument(!(annotation.getExpression() instanceof JsTypeAnnotation));
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

    public JsTypeAnnotation build() {
      return new JsTypeAnnotation(expression, annotationType, isDeclaration);
    }
  }
}
