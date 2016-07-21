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

import com.google.j2cl.ast.annotations.Context;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.common.HasMetadata;
import com.google.j2cl.ast.sourcemap.HasSourcePosition;
import com.google.j2cl.ast.sourcemap.SourcePosition;
import javax.annotation.Nullable;

/** Field declaration node. */
@Visitable
@Context
public class Field extends Member implements HasSourcePosition {
  @Visitable FieldDescriptor fieldDescriptor;
  @Visitable @Nullable Expression initializer;
  private boolean isEnumField;
  private Variable capturedVariable;
  private SourcePosition sourcePosition = SourcePosition.UNKNOWN;

  private Field(
      FieldDescriptor fieldDescriptor,
      Expression initializer,
      boolean isEnumField,
      Variable capturedVariable,
      SourcePosition sourcePosition) {
    this(fieldDescriptor);
    this.initializer = initializer;
    this.isEnumField = isEnumField;
    this.capturedVariable = capturedVariable;
    this.sourcePosition = sourcePosition;
  }

  public Field(FieldDescriptor fieldDescriptor) {
    this.fieldDescriptor = checkNotNull(fieldDescriptor);
  }

  public FieldDescriptor getDescriptor() {
    return fieldDescriptor;
  }

  public Expression getInitializer() {
    return initializer;
  }

  public Variable getCapturedVariable() {
    return this.capturedVariable;
  }

  public boolean hasInitializer() {
    return initializer != null;
  }

  public boolean isCompileTimeConstant() {
    return fieldDescriptor.isCompileTimeConstant();
  }

  public boolean isEnumField() {
    return isEnumField;
  }

  @Override
  public boolean isStatic() {
    return fieldDescriptor.isStatic();
  }

  @Override
  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public void setSourcePosition(SourcePosition sourcePosition) {
    this.sourcePosition = sourcePosition;
  }

  @Override
  public void copyMetadataFrom(HasMetadata<HasSourcePosition> store) {
    setSourcePosition(store.getMetadata().getSourcePosition());
  }

  @Override
  public HasSourcePosition getMetadata() {
    return this;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Field.visit(processor, this);
  }

  /**
   * A Builder for easily and correctly creating modified versions of fields.
   */
  public static class Builder {
    private FieldDescriptor fieldDescriptor;
    private Expression initializer;
    private boolean isEnumField;
    private Variable capturedVariable;
    private SourcePosition sourcePosition = SourcePosition.UNKNOWN;
    public static Builder from(Field field) {
      Builder builder = new Builder();
      builder.fieldDescriptor = field.getDescriptor();
      builder.initializer = field.getInitializer();
      builder.isEnumField = field.isEnumField();
      builder.capturedVariable = field.getCapturedVariable();
      builder.sourcePosition = field.getSourcePosition();
      return builder;
    }

    public static Builder fromDefault(FieldDescriptor fieldDescriptor) {
      Builder builder = new Builder();
      builder.fieldDescriptor = fieldDescriptor;
      return builder;
    }

    public Builder setInitializer(Expression initializer) {
      this.initializer = initializer;
      return this;
    }

    public Builder setIsEnumField(boolean isEnumField) {
      this.isEnumField = isEnumField;
      return this;
    }

    public Builder setCapturedVariable(Variable capturedVariable) {
      this.capturedVariable = capturedVariable;
      return this;
    }

    public Builder setEnclosingClass(TypeDescriptor enclosingClassTypeDescriptor) {
      this.fieldDescriptor =
          FieldDescriptor.Builder.from(fieldDescriptor)
              .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
              .build();
      return this;
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Field build() {
      return new Field(fieldDescriptor, initializer, isEnumField, capturedVariable, sourcePosition);
    }
  }
}
