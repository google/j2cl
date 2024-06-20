/*
 * Copyright 2022 Google Inc.
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

/** Class for a reference to an array creation. */
@Visitable
public class ArrayCreationReference extends Expression {
  @Visitable ArrayTypeDescriptor targetTypeDescriptor;
  @Visitable MethodDescriptor interfaceMethodDescriptor;
  private final SourcePosition sourcePosition;

  private ArrayCreationReference(
      SourcePosition sourcePosition,
      ArrayTypeDescriptor targetTypeDescriptor,
      MethodDescriptor interfaceMethodDescriptor) {
    this.targetTypeDescriptor = checkNotNull(targetTypeDescriptor);
    this.interfaceMethodDescriptor = checkNotNull(interfaceMethodDescriptor);
    this.sourcePosition = checkNotNull(sourcePosition);
    checkArgument(interfaceMethodDescriptor.getEnclosingTypeDescriptor().isFunctionalInterface());
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return interfaceMethodDescriptor.getEnclosingTypeDescriptor();
  }

  public ArrayTypeDescriptor getTargetTypeDescriptor() {
    return targetTypeDescriptor;
  }

  public MethodDescriptor getInterfacedMethodDescriptor() {
    return interfaceMethodDescriptor;
  }

  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public Precedence getPrecedence() {
    return Precedence.FUNCTION;
  }

  @Override
  public boolean canBeNull() {
    return false;
  }

  @Override
  public ArrayCreationReference clone() {
    return ArrayCreationReference.newBuilder()
        .setTargetTypeDescriptor(targetTypeDescriptor)
        .setInterfaceMethodDescriptor(interfaceMethodDescriptor)
        .setSourcePosition(sourcePosition)
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_ArrayCreationReference.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for FunctionExpression. */
  public static class Builder {
    private ArrayTypeDescriptor targetTypeDescriptor;
    private MethodDescriptor interfaceMethodDescriptor;
    private SourcePosition sourcePosition;

    public static Builder from(ArrayCreationReference expression) {
      return new Builder()
          .setTargetTypeDescriptor(expression.getTargetTypeDescriptor())
          .setInterfaceMethodDescriptor(expression.getInterfacedMethodDescriptor())
          .setSourcePosition(expression.getSourcePosition());
    }

    @CanIgnoreReturnValue
    public Builder setTargetTypeDescriptor(ArrayTypeDescriptor targetTypeDescriptor) {
      this.targetTypeDescriptor = targetTypeDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setInterfaceMethodDescriptor(MethodDescriptor methodDescriptor) {
      this.interfaceMethodDescriptor = methodDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public ArrayCreationReference build() {
      return new ArrayCreationReference(
          sourcePosition, targetTypeDescriptor, interfaceMethodDescriptor);
    }
  }
}
