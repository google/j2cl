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
import java.util.List;

/**
 * Class for method call expression.
 */
@Visitable
public class MethodCall extends Invocation {
  private final SourcePosition sourcePosition;

  /** If an instance call should be dispatched statically, e.g. A.super.method() invocation. */
  private final boolean isStaticDispatch;

  private MethodCall(
      SourcePosition sourcePosition,
      Expression qualifier,
      MethodDescriptor target,
      List<Expression> arguments,
      boolean isStaticDispatch) {
    super(qualifier, target, arguments);

    this.isStaticDispatch = isStaticDispatch;
    this.sourcePosition = checkNotNull(sourcePosition);
  }

  public boolean isStaticDispatch() {
    return isStaticDispatch;
  }

  /** Returns true if the call needs dynamic dispatch. */
  public boolean isPolymorphic() {
    return getTarget().isPolymorphic()
        && !isStaticDispatch
        && !(qualifier instanceof SuperReference);
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return getTarget().getReturnTypeDescriptor();
  }

  @Override
  public TypeDescriptor getDeclaredTypeDescriptor() {
    return getTarget().getDeclarationDescriptor().getReturnTypeDescriptor();
  }

  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public MethodCall clone() {
    return new MethodCall(
        sourcePosition,
        AstUtils.clone(qualifier),
        getTarget(),
        AstUtils.clone(arguments),
        isStaticDispatch);
  }

  @Override
  Builder createBuilder() {
    return new Builder(this);
  }

  @Override
  Node acceptInternal(Processor processor) {
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
    private SourcePosition sourcePosition;

    public static Builder from(MethodCall methodCall) {
      return new Builder(methodCall);
    }

    public static Builder from(MethodDescriptor methodDescriptor) {
      Builder builder = new Builder();
      builder.setTarget(methodDescriptor).setSourcePosition(SourcePosition.NONE);
      return builder;
    }

    public final Builder setStaticDispatch(boolean isStaticDispatch) {
      this.isStaticDispatch = isStaticDispatch;
      return this;
    }

    public final Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    @Override
    public MethodCall build() {
      return new MethodCall(
          sourcePosition, getQualifier(), getTarget(), getArguments(), isStaticDispatch);
    }

    private Builder(MethodCall methodCall) {
      super(methodCall);
      this.isStaticDispatch = methodCall.isStaticDispatch;
      this.sourcePosition = methodCall.sourcePosition;
    }

    private Builder() {}
  }
}
