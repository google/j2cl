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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.processors.Visitable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstracts invocations, i.e. method calls and new instances.
 */
@Visitable
public abstract class Invocation extends Expression implements MemberReference {

  @Override
  public abstract MethodDescriptor getTarget();

  public abstract List<Expression> getArguments();

  abstract Builder newBuilder();

  @Override
  public abstract Node accept(Processor processor);
  /**
   * Common logic for a builder to create method calls and new instances.
   *
   * <p>Takes care of the busy work of keeping argument list and method descriptor parameter types
   * list in sync.
   */
  public abstract static class Builder {

    private Expression qualifierExpression;
    private MethodDescriptor methodDescriptor;
    private List<Expression> arguments = new ArrayList<>();

    public static Builder from(Invocation invocation) {
      return invocation.newBuilder();
    }

    public Builder setArguments(List<Expression> arguments) {
      this.arguments.clear();
      this.arguments.addAll(arguments);
      return this;
    }

    public Builder appendArgumentsAndUpdateDescriptor(Expression... argumentExpressions) {
      return appendArgumentsAndUpdateDescriptor(Arrays.asList(argumentExpressions));
    }

    public Builder appendArgumentsAndUpdateDescriptor(Iterable<Expression> argumentExpressions) {
      Iterables.addAll(arguments, argumentExpressions);
      methodDescriptor =
          MethodDescriptors.createWithExtraParameters(
              methodDescriptor,
              Iterables.transform(
                  argumentExpressions,
                  new Function<Expression, TypeDescriptor>() {
                    @Override
                    public TypeDescriptor apply(Expression expression) {
                      return expression.getTypeDescriptor();
                    }
                  }));
      return this;
    }

    public Builder appendArgumentAndUpdateDescriptor(Expression argumentExpression) {
      arguments.add(argumentExpression);
      methodDescriptor =
          MethodDescriptors.createWithExtraParameters(
              methodDescriptor, argumentExpression.getTypeDescriptor());
      return this;
    }

    public Builder appendArgumentAndUpdateDescriptor(
        Expression argumentExpression, TypeDescriptor parameterTypeDescriptor) {
      arguments.add(argumentExpression);
      methodDescriptor =
          MethodDescriptors.createWithExtraParameters(methodDescriptor, parameterTypeDescriptor);
      return this;
    }

    public Builder replaceVarargsArgument(Expression... replacementArguments) {
      return replaceVarargsArgument(Arrays.asList(replacementArguments));
    }

    public Builder replaceVarargsArgument(List<Expression> replacementArguments) {
      checkState(methodDescriptor.isJsMethodVarargs());
      int lastArgumentPosition = arguments.size() - 1;
      arguments.remove(lastArgumentPosition);
      arguments.addAll(replacementArguments);
      return this;
    }

    public Builder setQualifier(Expression qualifierExpression) {
      this.qualifierExpression = qualifierExpression;
      return this;
    }

    public Builder setMethodDescriptor(MethodDescriptor methodDescriptor) {
      this.methodDescriptor = methodDescriptor;
      return this;
    }

    public Builder setEnclosingClass(TypeDescriptor enclosingClassTypeDescriptor) {
      this.methodDescriptor =
          MethodDescriptor.Builder.from(methodDescriptor)
              .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
              .build();
      return this;
    }

    public Invocation build() {
      return doCreateInvocation(qualifierExpression, methodDescriptor, arguments);
    }

    protected abstract Invocation doCreateInvocation(
        Expression qualifierExpression,
        MethodDescriptor finalMethodDescriptor,
        List<Expression> finalArguments);
    
    protected Builder(Invocation invocation) {
      this.qualifierExpression = invocation.getQualifier();
      this.methodDescriptor = invocation.getTarget();
      this.arguments = Lists.newArrayList(invocation.getArguments());
    }
  }
}
