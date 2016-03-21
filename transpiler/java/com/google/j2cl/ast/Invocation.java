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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstracts invocations, i.e. method calls and new instances.
 */
public interface Invocation extends MemberReference {

  @Override
  MethodDescriptor getTarget();

  List<Expression> getArguments();

  /**
   * Common logic for a builder to create method calls and new instances.
   *
   * <p>Takes care of the busy work of keeping argument list and method descriptor parameter types
   * list in sync.
   */
  abstract class Builder<T extends Invocation> {

    private Expression qualifierExpression;
    private MethodDescriptor originalMethodDescriptor;
    private List<Expression> originalArguments = new ArrayList<>();
    private List<Expression> additionalArguments = new ArrayList<>();
    private List<TypeDescriptor> additionalTypeDescriptors = new ArrayList<>();

    protected void initFrom(T call) {
      this.qualifierExpression = call.getQualifier();
      this.originalMethodDescriptor = call.getTarget();
      this.originalArguments = Lists.newArrayList(call.getArguments());
    }

    public Builder<T> arguments(List<Expression> arguments) {
      originalArguments.clear();
      originalArguments.addAll(arguments);
      return this;
    }

    public Builder<T> addArgument(
        Expression argumentExpression, TypeDescriptor parameterTypeDescriptor) {
      additionalArguments.add(argumentExpression);
      if (parameterTypeDescriptor == null) {
        // We allow adding an argument to a method call without adding a corresponding parameter in
        // the MethodDescriptor if the method is a JsMethod vararg method, because the method call
        // will accept multiple individual arguments as the varargs, rather than a wrapped array.
        Preconditions.checkArgument(
            originalMethodDescriptor.isJsMethodVarargs(),
            "Parameter TypeDescriptor must be provided when adding argument to a MethodCall that is"
                + "not a vararg JS method call.");
      } else {
        additionalTypeDescriptors.add(parameterTypeDescriptor);
      }
      return this;
    }

    public Builder<T> removeLastArgument() {
      Preconditions.checkArgument(
          originalMethodDescriptor.isJsMethodVarargs(),
          "Unsupported operation for methods that are not varargs JS methods.");
      originalArguments.remove(originalArguments.size() - 1);
      return this;
    }

    public Builder<T> qualifier(Expression qualifierExpression) {
      this.qualifierExpression = qualifierExpression;
      return this;
    }

    public Builder<T> methodDescriptor(MethodDescriptor methodDescriptor) {
      this.originalMethodDescriptor = methodDescriptor;
      return this;
    }

    public Builder<T> enclosingClass(TypeDescriptor enclosingClassTypeDescriptor) {
      this.originalMethodDescriptor =
          MethodDescriptorBuilder.from(originalMethodDescriptor)
              .enclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
              .build();
      return this;
    }

    public T build() {
      List<Expression> finalArguments = new ArrayList<>();
      finalArguments.addAll(originalArguments);
      finalArguments.addAll(additionalArguments);

      MethodDescriptor finalMethodDescriptor =
          MethodDescriptors.createModifiedCopy(originalMethodDescriptor, additionalTypeDescriptors);

      return doCreateInvocation(qualifierExpression, finalMethodDescriptor, finalArguments);
    }

    protected abstract T doCreateInvocation(
        Expression qualifierExpression,
        MethodDescriptor finalMethodDescriptor,
        List<Expression> finalArguments);
  }
}
