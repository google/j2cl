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

/**
 * A Builder for easily and correctly creating modified versions of CastExpressions.
 */
public class CastExpressionBuilder {
  private Expression expression;
  private TypeDescriptor castTypeDescriptor;
  private boolean isRaw;
  private boolean isNullable;

  public static CastExpressionBuilder from(CastExpression cast) {
    CastExpressionBuilder builder = new CastExpressionBuilder();
    builder.expression = cast.getExpression();
    builder.castTypeDescriptor = cast.getCastTypeDescriptor();
    builder.isRaw = cast.isRaw();
    builder.isNullable = cast.isNullable();
    return builder;
  }

  public CastExpressionBuilder expression(Expression expression) {
    this.expression = expression;
    return this;
  }

  public CastExpressionBuilder castTypeDescriptor(TypeDescriptor castTypeDescriptor) {
    this.castTypeDescriptor = castTypeDescriptor;
    return this;
  }

  public CastExpressionBuilder isRaw(boolean isRaw) {
    this.isRaw = isRaw;
    return this;
  }

  public CastExpressionBuilder isNullable(boolean isNullable) {
    this.isNullable = isNullable;
    return this;
  }

  public CastExpression build() {
    return new CastExpression(expression, castTypeDescriptor, isRaw, isNullable);
  }
}
