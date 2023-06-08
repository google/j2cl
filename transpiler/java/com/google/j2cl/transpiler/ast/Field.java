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
  // TODO(b/112150736): generalize concept of the source position for names to members.
  private final SourcePosition nameSourcePosition;
  // Only valid for enum fields, where it is >= 0.
  private int enumOrdinal;
  private boolean isImmutable;

  private Field(
      SourcePosition sourcePosition,
      FieldDescriptor fieldDescriptor,
      Expression initializer,
      int enumOrdinal,
      boolean isImmutable,
      SourcePosition nameSourcePosition) {
    super(sourcePosition);
    this.fieldDescriptor = checkNotNull(fieldDescriptor);
    this.initializer = initializer;
    this.nameSourcePosition = checkNotNull(nameSourcePosition);
    this.enumOrdinal = enumOrdinal;
    this.isImmutable = isImmutable;
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
    return descriptor.getKtInfo().isUninitializedWarningSuppressed()
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

  /**
   * Returns true is this field is immutable and its initialization can be hoisted.
   *
   * <p>In some platforms, e.g. WASM, it is beneficial to declare fields as immutable and initialize
   * them at instantiation. Since in Java it is possible to observe uninitialized values even for
   * final fields with initializers, a non-local analysis needs to be performed to decide whether a
   * field can be marked immutable.
   */
  public boolean isImmutable() {
    return isImmutable;
  }

  /** Marks a field as immutable. */
  public void setImmutable(boolean isImmutable) {
    this.isImmutable = isImmutable;
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
  Node acceptInternal(Processor processor) {
    return Visitor_Field.visit(processor, this);
  }

  /** A Builder for Field. */
  public static class Builder {
    private FieldDescriptor fieldDescriptor;
    private Expression initializer;
    private SourcePosition sourcePosition;
    private SourcePosition nameSourcePosition = SourcePosition.NONE;
    private int enumOrdinal = -1;
    private boolean isImmutable = false;

    public static Builder from(Field field) {
      Builder builder = new Builder();
      builder.fieldDescriptor = field.getDescriptor();
      builder.initializer = field.getInitializer();
      builder.sourcePosition = field.getSourcePosition();
      builder.nameSourcePosition = field.getNameSourcePosition();
      builder.enumOrdinal = field.enumOrdinal;
      builder.isImmutable = field.isImmutable;
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

    public Builder setImmutable(boolean isImmutable) {
      this.isImmutable = isImmutable;
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
          enumOrdinal,
          isImmutable,
          nameSourcePosition);
    }
  }
}
