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
import com.google.j2cl.ast.processors.common.Processor;
import com.google.j2cl.common.SourcePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class for method call expression.
 */
@Visitable
public class MethodCall extends Invocation {
  @Visitable Expression qualifier;
  @Visitable MethodDescriptor targetMethodDescriptor;
  @Visitable List<Expression> arguments = new ArrayList<>();
  private final Optional<SourcePosition> sourcePosition;

  /**
   * If an instance call should be dispatched statically, e.g. A.super.method() invocation.
   */
  private boolean isStaticDispatch;

  private MethodCall(
      Optional<SourcePosition> sourcePosition,
      Expression qualifier,
      MethodDescriptor targetMethodDescriptor,
      List<Expression> arguments,
      boolean isStaticDispatch) {
    this.targetMethodDescriptor = checkNotNull(targetMethodDescriptor);
    this.qualifier = checkNotNull(AstUtils.getExplicitQualifier(qualifier, targetMethodDescriptor));
    this.arguments.addAll(checkNotNull(arguments));
    this.isStaticDispatch = isStaticDispatch;
    this.sourcePosition = checkNotNull(sourcePosition);
  }

  @Override
  public Expression getQualifier() {
    return this.qualifier;
  }

  /**
   * Would normally be named getTargetMethodDescriptor() but in this situation it was more important
   * to implement the MemberReference interface.
   */
  @Override
  public MethodDescriptor getTarget() {
    return this.targetMethodDescriptor;
  }

  @Override
  public List<Expression> getArguments() {
    return this.arguments;
  }

  public boolean isStaticDispatch() {
    return isStaticDispatch;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return targetMethodDescriptor.getReturnTypeDescriptor();
  }

  @Override
  public TypeDescriptor getDeclaredTypeDescriptor() {
    return targetMethodDescriptor.getDeclarationDescriptor().getReturnTypeDescriptor();
  }

  public SourcePosition getSourcePosition() {
    return sourcePosition.get();
  }

  @Override
  public MethodCall clone() {
    return new MethodCall(
        sourcePosition,
        qualifier.clone(),
        targetMethodDescriptor,
        AstUtils.clone(arguments),
        isStaticDispatch);
  }

  @Override
  Builder createBuilder() {
    return new Builder(this);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_MethodCall.visit(processor, this);
  }

  /**
   * A Builder for MethodCall.
   *
   * <p>Takes care of the busy work of keeping argument list and method descriptor parameter types
   * list in sync.
   */
  public static class Builder extends Invocation.Builder<Builder, MethodCall> {
    private boolean isStaticDispatch;
    private Optional<SourcePosition> sourcePosition;

    public static Builder from(MethodCall methodCall) {
      return new Builder(methodCall);
    }

    public static Builder from(MethodDescriptor methodDescriptor) {
      Builder builder = new Builder();
      builder.setMethodDescriptor(methodDescriptor).setSourcePosition(null);
      return builder;
    }

    public final Builder setStaticDispatch(boolean isStaticDispatch) {
      this.isStaticDispatch = isStaticDispatch;
      return this;
    }

    public final Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = Optional.ofNullable(sourcePosition);
      return this;
    }

    @Override
    protected MethodCall doCreateInvocation(
        Expression qualifierExpression,
        MethodDescriptor methodDescriptor,
        List<Expression> arguments) {
      return new MethodCall(
          sourcePosition, qualifierExpression, methodDescriptor, arguments, isStaticDispatch);
    }

    private Builder(MethodCall methodCall) {
      super(methodCall);
      this.isStaticDispatch = methodCall.isStaticDispatch();
      this.sourcePosition = methodCall.sourcePosition;
    }

    private Builder() {}
  }
}
