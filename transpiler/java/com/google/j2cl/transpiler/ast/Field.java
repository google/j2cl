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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import javax.annotation.Nullable;

/** Field declaration node. */
@Visitable
public class Field extends Member {

  @Visitable FieldDescriptor fieldDescriptor;
  @Visitable @Nullable Expression initializer;

  private Field(
      SourcePosition sourcePosition, FieldDescriptor fieldDescriptor, Expression initializer) {
    super(sourcePosition);
    this.fieldDescriptor = checkNotNull(fieldDescriptor);
    this.initializer = initializer;
  }

  @Override
  public FieldDescriptor getDescriptor() {
    return fieldDescriptor;
  }

  public Expression getInitializer() {
    return initializer;
  }

  public boolean hasInitializer() {
    return initializer != null;
  }

  public boolean isCompileTimeConstant() {
    return fieldDescriptor.isCompileTimeConstant();
  }

  @Override
  public boolean isField() {
    return true;
  }

  @Override
  public boolean isEnumField() {
    return getDescriptor().isEnumConstant();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_Field.visit(processor, this);
  }

  public Builder toBuilder() {
    return builder()
        .setDescriptor(this.getDescriptor())
        .setInitializer(this.getInitializer())
        .setSourcePosition(this.getSourcePosition());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder builderFrom(FieldDescriptor fieldDescriptor) {
    return builder().setDescriptor(fieldDescriptor);
  }

  /** A Builder for Field. */
  public static class Builder {
    private FieldDescriptor fieldDescriptor;
    private Expression initializer;
    private SourcePosition sourcePosition;

    @CanIgnoreReturnValue
    public Builder setDescriptor(FieldDescriptor fieldDescriptor) {
      this.fieldDescriptor = fieldDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setInitializer(Expression initializer) {
      this.initializer = initializer;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setEnclosingClass(DeclaredTypeDescriptor enclosingTypeDescriptor) {
      this.fieldDescriptor =
          fieldDescriptor.toBuilder().setEnclosingTypeDescriptor(enclosingTypeDescriptor).build();
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Field build() {
      return new Field(sourcePosition, fieldDescriptor, initializer);
    }
  }
}
