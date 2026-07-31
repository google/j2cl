/*
 * Copyright 2026 Google Inc.
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

import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a call to a Wasm funcref (function reference).
 *
 * <p>This node stores a qualifier, to be passed on invocation, and an expression evaluating to the
 * funcref.
 *
 * <p>This node is not an {@link Invocation} because it's not really a {@link MemberReference}, ie,
 * there is no target member descriptor, but rather an expression evaluating to a funcref and a
 * functional interface type, which is what we use to describe the type of a "function".
 */
@Visitable
public class WasmFuncrefCall extends Expression {
  @Visitable Expression funcref;
  @Visitable Expression instance;
  @Visitable List<Expression> arguments;
  private final DeclaredTypeDescriptor functionalInterface;

  private WasmFuncrefCall(
      Expression funcref,
      Expression instance,
      DeclaredTypeDescriptor functionalInterface,
      List<Expression> arguments) {
    this.funcref = checkNotNull(funcref);
    checkState(TypeDescriptors.isWasmFuncref(funcref.getTypeDescriptor()));

    this.instance = checkNotNull(instance);
    this.functionalInterface = checkNotNull(functionalInterface);
    this.arguments = checkNotNull(arguments);
  }

  public Expression getFuncref() {
    return funcref;
  }

  public Expression getInstance() {
    return instance;
  }

  public DeclaredTypeDescriptor getFunctionalInterface() {
    return functionalInterface;
  }

  public List<Expression> getArguments() {
    return arguments;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return functionalInterface.getSingleAbstractMethodDescriptor().getReturnTypeDescriptor();
  }

  @Override
  public TypeDescriptor getDeclaredTypeDescriptor() {
    return functionalInterface
        .getSingleAbstractMethodDescriptor()
        .getDeclarationDescriptor()
        .getReturnTypeDescriptor();
  }

  @Override
  public Precedence getPrecedence() {
    return Precedence.MEMBER_ACCESS;
  }

  @Override
  public WasmFuncrefCall clone() {
    return WasmFuncrefCall.builder()
        .setFuncref(funcref.clone())
        .setInstance(instance.clone())
        .setFunctionalInterface(functionalInterface)
        .setArguments(AstUtils.clone(arguments))
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_WasmFuncrefCall.visit(processor, this);
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** A builder for {@link WasmFuncrefCall}. */
  public static class Builder {
    private Expression funcref;
    private Expression instance;
    private DeclaredTypeDescriptor functionalInterface;
    private List<Expression> arguments = new ArrayList<>();

    @CanIgnoreReturnValue
    public Builder setFuncref(Expression funcref) {
      this.funcref = funcref;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setInstance(Expression instance) {
      this.instance = instance;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setFunctionalInterface(DeclaredTypeDescriptor functionalInterface) {
      this.functionalInterface = functionalInterface;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setArguments(List<Expression> arguments) {
      this.arguments.clear();
      this.arguments.addAll(arguments);
      return this;
    }

    public WasmFuncrefCall build() {
      return new WasmFuncrefCall(funcref, instance, functionalInterface, arguments);
    }

    private Builder(WasmFuncrefCall wasmFuncrefCall) {
      this.funcref = wasmFuncrefCall.getFuncref();
      this.instance = wasmFuncrefCall.getInstance();
      this.functionalInterface = wasmFuncrefCall.getFunctionalInterface();
      this.arguments = Lists.newArrayList(wasmFuncrefCall.getArguments());
    }

    private Builder() {}
  }
}
