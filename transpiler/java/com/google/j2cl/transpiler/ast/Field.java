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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.Objects;
import javax.annotation.Nullable;

/** Field declaration node. */
@Visitable
public class Field extends Member {

  @Visitable FieldDescriptor fieldDescriptor;
  @Visitable @Nullable Expression initializer;
  // TODO(b/112150736): generalize concept of the source position for names to members.
  private final SourcePosition nameSourcePosition;
 
  private Field(
      SourcePosition sourcePosition,
      FieldDescriptor fieldDescriptor,
      Expression initializer,
      SourcePosition nameSourcePosition) {
    super(sourcePosition);
    this.fieldDescriptor = checkNotNull(fieldDescriptor);
    this.initializer = initializer;
    this.nameSourcePosition = checkNotNull(nameSourcePosition);
  }

  @Override
  public FieldDescriptor getDescriptor() {
    return fieldDescriptor;
  }

  public Expression getInitializer() {
    return initializer;
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

  public boolean isKtLateInit() {
    FieldDescriptor descriptor = getDescriptor();
    boolean isTestProperty =
        descriptor.getEnclosingTypeDescriptor().getTypeDeclaration().isTestClass()
            || descriptor
                .getEnclosingTypeDescriptor()
                .getTypeDeclaration()
                .getAllSuperTypesIncludingSelf()
                .stream()
                .anyMatch(
                    type ->
                        Objects.equals(type.getQualifiedSourceName(), "junit.framework.TestCase")
                            || Objects.equals(type.getQualifiedSourceName(), "org.junit.TestCase"));

    return (descriptor.getKtInfo().isUninitializedWarningSuppressed() || isTestProperty)
        && !descriptor.isFinal()
        && !descriptor.getTypeDescriptor().isNullable()
        && !hasInitializer();
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

  /** A Builder for Field. */
  public static class Builder {
    private FieldDescriptor fieldDescriptor;
    private Expression initializer;
    private SourcePosition sourcePosition;
    private SourcePosition nameSourcePosition = SourcePosition.NONE;

    public static Builder from(Field field) {
      Builder builder = new Builder();
      builder.fieldDescriptor = field.getDescriptor();
      builder.initializer = field.getInitializer();
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
          sourcePosition,
          fieldDescriptor,
          initializer,
          nameSourcePosition);
    }
  }
}
