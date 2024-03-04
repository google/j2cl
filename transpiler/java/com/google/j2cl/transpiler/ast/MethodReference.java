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
import javax.annotation.Nullable;

/** Class for a method reference. */
@Visitable
public class MethodReference extends Expression {
  private final TypeDescriptor typeDescriptor;
  private final MethodDescriptor referencedMethodDescriptor;
  private final MethodDescriptor interfaceMethodDescriptor;
  @Nullable @Visitable protected Expression qualifier;
  private final SourcePosition sourcePosition;

  private MethodReference(
      SourcePosition sourcePosition,
      TypeDescriptor typeDescriptor,
      MethodDescriptor referencedMethodDescriptor,
      MethodDescriptor interfaceMethodDescriptor,
      Expression qualifier) {
    this.typeDescriptor = checkNotNull(typeDescriptor);
    this.referencedMethodDescriptor = checkNotNull(referencedMethodDescriptor);
    this.interfaceMethodDescriptor = checkNotNull(interfaceMethodDescriptor);
    this.qualifier = qualifier;
    this.sourcePosition = checkNotNull(sourcePosition);
    checkArgument(interfaceMethodDescriptor.getEnclosingTypeDescriptor().isFunctionalInterface());
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  public MethodDescriptor getReferencedMethodDescriptor() {
    return referencedMethodDescriptor;
  }

  public MethodDescriptor getInterfacedMethodDescriptor() {
    return interfaceMethodDescriptor;
  }

  public Expression getQualifier() {
    return this.qualifier;
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
  public MethodReference clone() {
    return MethodReference.newBuilder()
        .setTypeDescriptor(typeDescriptor)
        .setReferencedMethodDescriptor(referencedMethodDescriptor)
        .setInterfaceMethodDescriptor(interfaceMethodDescriptor)
        .setQualifier(AstUtils.clone(qualifier))
        .setSourcePosition(sourcePosition)
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_MethodReference.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for FunctionExpression. */
  public static class Builder {
    private TypeDescriptor typeDescriptor;
    private MethodDescriptor referencedMethodDescriptor;
    private MethodDescriptor interfaceMethodDescriptor;
    private Expression qualifier;
    private SourcePosition sourcePosition;

    public static Builder from(MethodReference expression) {
      return new Builder()
          .setTypeDescriptor(expression.getTypeDescriptor())
          .setReferencedMethodDescriptor(expression.getReferencedMethodDescriptor())
          .setInterfaceMethodDescriptor(expression.getInterfacedMethodDescriptor())
          .setQualifier(expression.getQualifier())
          .setSourcePosition(expression.getSourcePosition());
    }

    @CanIgnoreReturnValue
    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setReferencedMethodDescriptor(MethodDescriptor methodDescriptor) {
      this.referencedMethodDescriptor = methodDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setInterfaceMethodDescriptor(MethodDescriptor methodDescriptor) {
      this.interfaceMethodDescriptor = methodDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setQualifier(Expression qualifier) {
      this.qualifier = qualifier;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public MethodReference build() {
      return new MethodReference(
          sourcePosition,
          typeDescriptor,
          referencedMethodDescriptor,
          interfaceMethodDescriptor,
          qualifier);
    }
  }
}
