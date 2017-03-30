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

import com.google.common.collect.Lists;
import com.google.j2cl.ast.annotations.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstracts invocations, i.e. method calls and new instances.
 */
@Visitable
public abstract class Invocation extends Expression implements MemberReference {

  @Override
  public abstract MethodDescriptor getTarget();

  public abstract List<Expression> getArguments();

  abstract Builder createBuilder();

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
      return invocation.createBuilder();
    }

    public Builder setArguments(List<Expression> arguments) {
      this.arguments.clear();
      this.arguments.addAll(arguments);
      return this;
    }

    public Builder addArgumentsAndUpdateDescriptor(int index, Expression... argumentExpressions) {
      return addArgumentsAndUpdateDescriptor(index, Arrays.asList(argumentExpressions));
    }

    public Builder addArgumentsAndUpdateDescriptor(
        int index, Collection<Expression> argumentExpressions) {
      arguments.addAll(index, argumentExpressions);
      // Add the provided parameters to the proper index position of the existing parameters list.

      methodDescriptor =
          MethodDescriptor.Builder.from(methodDescriptor)
              .addParameterTypeDescriptors(
                  index,
                  argumentExpressions
                      .stream()
                      .map(Expression::getTypeDescriptor)
                      .collect(Collectors.toList()))
              .build();
      return this;
    }

    public Builder addArgumentAndUpdateDescriptor(
        int index, Expression argumentExpression, TypeDescriptor parameterTypeDescriptor) {
      arguments.add(index, argumentExpression);
      // Add the provided parameters to the proper index position of the existing parameters list.

      methodDescriptor =
          MethodDescriptor.Builder.from(methodDescriptor)
              .addParameterTypeDescriptors(index, parameterTypeDescriptor)
              .build();
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

    public Builder setEnclosingTypeDescriptor(TypeDescriptor enclosingTypeDescriptor) {
      this.methodDescriptor =
          MethodDescriptor.Builder.from(methodDescriptor)
              .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
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

    protected Builder() {
      this.arguments = Lists.newArrayList();
    }
  }
}
