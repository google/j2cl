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

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.SourcePosition;
import javax.annotation.Nullable;

/** Field declaration node. */
@Visitable
public class Field extends Member implements HasJsNameInfo {
  @Visitable FieldDescriptor fieldDescriptor;
  @Visitable @Nullable Expression initializer;
  private boolean isEnumField;
  private Variable capturedVariable;

  private Field(
      FieldDescriptor fieldDescriptor,
      Expression initializer,
      boolean isEnumField,
      Variable capturedVariable) {
    this.fieldDescriptor = checkNotNull(fieldDescriptor);
    this.initializer = initializer;
    this.isEnumField = isEnumField;
    this.capturedVariable = capturedVariable;
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

  @Override
  public String getStackTraceMethodName() {
    return null;
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
  public boolean isField() {
    return true;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Field.visit(processor, this);
  }

  @Override
  public String getSimpleJsName() {
    return fieldDescriptor.getSimpleJsName();
  }

  @Override
  public String getJsNamespace() {
    return fieldDescriptor.getJsNamespace();
  }

  /** A Builder for Field. */
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

    public static Builder from(FieldDescriptor fieldDescriptor) {
      Builder builder = new Builder();
      builder.fieldDescriptor = fieldDescriptor;
      return builder;
    }

    public Builder setInitializer(Expression initializer) {
      this.initializer = initializer;
      return this;
    }

    public Builder setEnumField(boolean isEnumField) {
      this.isEnumField = isEnumField;
      return this;
    }

    public Builder setCapturedVariable(Variable capturedVariable) {
      this.capturedVariable = capturedVariable;
      return this;
    }

    public Builder setEnclosingClass(TypeDescriptor enclosingTypeDescriptor) {
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

    public Field build() {
      Field field = new Field(fieldDescriptor, initializer, isEnumField, capturedVariable);
      field.setSourcePosition(sourcePosition);
      return field;
    }
  }
}
