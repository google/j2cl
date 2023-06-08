/*
 * Copyright 2021 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.ast;

import com.google.common.base.Preconditions;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** A node that represents a closure annotation "/** @annotation * / (expression)". */
@Visitable
public class JsDocExpression extends Expression {
  @Visitable Expression expression;
  private final String annotation;

  public JsDocExpression(Expression expression, String annotation) {
    this.expression = Preconditions.checkNotNull(expression);
    this.annotation = Preconditions.checkNotNull(annotation);
  }

  public Expression getExpression() {
    return expression;
  }

  public String getAnnotation() {
    return annotation;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return expression.getTypeDescriptor();
  }

  @Override
  public Precedence getPrecedence() {
    return expression.getPrecedence();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_JsDocExpression.visit(processor, this);
  }

  @Override
  public Expression clone() {
    return Builder.from(this).build();
  }

  public static JsDocExpression.Builder newBuilder() {
    return new JsDocExpression.Builder();
  }

  /** Builder for JsDocExpression */
  public static class Builder {
    private Expression expression;
    private String annotation;

    public static Builder from(JsDocExpression annotation) {
      Builder builder = new Builder();
      builder.expression = annotation.getExpression();
      builder.annotation = annotation.getAnnotation();
      return builder;
    }

    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    public Builder setAnnotation(String annotation) {
      this.annotation = annotation;
      return this;
    }

    public JsDocExpression build() {
      return new JsDocExpression(expression, annotation);
    }
  }
}
