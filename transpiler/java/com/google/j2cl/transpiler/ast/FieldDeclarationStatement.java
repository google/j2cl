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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** A node that represent a closure field declaration "@public {type}". */
@Visitable
public class FieldDeclarationStatement extends Statement {
  @Visitable Expression expression;
  @Visitable FieldDescriptor fieldDescriptor;
  private final boolean isPublic;

  private FieldDeclarationStatement(
      SourcePosition sourcePosition,
      Expression expression,
      FieldDescriptor fieldDescriptor,
      boolean isPublic) {
    super(sourcePosition);
    this.expression = checkNotNull(expression);
    this.fieldDescriptor = checkNotNull(fieldDescriptor);
    this.isPublic = isPublic;
    checkArgument(
        expression instanceof FieldAccess || expression.isSimpleAssignment(),
        "Declaration annotations can only applied to assignments and field references.");
  }

  public Expression getExpression() {
    return expression;
  }

  public FieldDescriptor getFieldDescriptor() {
    return fieldDescriptor;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public boolean isConst() {
    return fieldDescriptor.isCompileTimeConstant();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_FieldDeclarationStatement.visit(processor, this);
  }

  @Override
  public FieldDeclarationStatement clone() {
    return new FieldDeclarationStatement(
        getSourcePosition(), expression.clone(), fieldDescriptor, isPublic);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for JsDocFieldDeclaration */
  public static class Builder {
    private Expression expression;
    private FieldDescriptor fieldDescriptor;
    private boolean isPublic;
    private SourcePosition sourcePosition;

    public static Builder from(FieldDeclarationStatement fieldDeclaration) {
      Builder builder = new Builder();
      builder.expression = fieldDeclaration.getExpression();
      builder.fieldDescriptor = fieldDeclaration.getFieldDescriptor();
      builder.isPublic = fieldDeclaration.isPublic();
      builder.sourcePosition = fieldDeclaration.getSourcePosition();

      return builder;
    }

    @CanIgnoreReturnValue
    public Builder setExpression(Expression initializer) {
      this.expression = initializer;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setFieldDescriptor(FieldDescriptor fieldDescriptor) {
      this.fieldDescriptor = fieldDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setPublic(boolean isPublic) {
      this.isPublic = isPublic;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public FieldDeclarationStatement build() {
      return new FieldDeclarationStatement(sourcePosition, expression, fieldDescriptor, isPublic);
    }
  }
}
