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
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import javax.annotation.Nullable;

/** Field declaration node. */
@Visitable
public class Field extends Member {
  private final FieldDescriptor fieldDescriptor;
  @Visitable @Nullable Expression initializer;
  private final Variable capturedVariable;
  // TODO(b/112150736): generalize concept of the source position for names to members.
  private final SourcePosition nameSourcePosition;
  // Only valid for enum fields, where it is >= 0.
  private int enumOrdinal = -1;

  private Field(
      SourcePosition sourcePosition,
      FieldDescriptor fieldDescriptor,
      Expression initializer,
      Variable capturedVariable,
      SourcePosition nameSourcePosition) {
    super(sourcePosition);
    this.fieldDescriptor = checkNotNull(fieldDescriptor);
    this.initializer = initializer;
    this.capturedVariable = capturedVariable;
    this.nameSourcePosition = checkNotNull(nameSourcePosition);
  }

  @Override
  public FieldDescriptor getDescriptor() {
    return fieldDescriptor;
  }

  public Expression getInitializer() {
    return initializer;
  }

  public Variable getCapturedVariable() {
    return this.capturedVariable;
  }

  public SourcePosition getNameSourcePosition() {
    return nameSourcePosition;
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

  public void setEnumOrdinal(int ordinal) {
    checkState(isEnumField());
    this.enumOrdinal = ordinal;
  }

  public int getEnumOrdinal() {
    checkState(isEnumField());
    checkState(enumOrdinal != -1);
    return enumOrdinal;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Field.visit(processor, this);
  }

  /** A Builder for Field. */
  public static class Builder {
    private FieldDescriptor fieldDescriptor;
    private Expression initializer;
    private Variable capturedVariable;
    private SourcePosition sourcePosition;
    private SourcePosition nameSourcePosition = SourcePosition.NONE;

    public static Builder from(Field field) {
      Builder builder = new Builder();
      builder.fieldDescriptor = field.getDescriptor();
      builder.initializer = field.getInitializer();
      builder.capturedVariable = field.getCapturedVariable();
      builder.sourcePosition = field.getSourcePosition();
      builder.nameSourcePosition = field.getNameSourcePosition();
      return builder;
    }

    public static Builder from(FieldDescriptor fieldDescriptor) {
      return new Builder().setDescriptor(fieldDescriptor);
    }

    public Builder setDescriptor(FieldDescriptor fieldDescriptor) {
      this.fieldDescriptor = fieldDescriptor;
      return this;
    }

    public Builder setInitializer(Expression initializer) {
      this.initializer = initializer;
      return this;
    }

    public Builder setCapturedVariable(Variable capturedVariable) {
      this.capturedVariable = capturedVariable;
      return this;
    }

    public Builder setEnclosingClass(DeclaredTypeDescriptor enclosingTypeDescriptor) {
      this.fieldDescriptor =
          FieldDescriptor.Builder.from(fieldDescriptor)
              .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
              .build();
      return this;
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Builder setNameSourcePosition(SourcePosition nameSourcePosition) {
      this.nameSourcePosition = nameSourcePosition;
      return this;
    }

    public Field build() {
      return new Field(
          sourcePosition, fieldDescriptor, initializer, capturedVariable, nameSourcePosition);
    }
  }
}
