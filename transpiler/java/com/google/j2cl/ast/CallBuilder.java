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

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Common logic for a builder for easily and correctly creates modified versions of calls.
 *
 * <p>Takes care of the busy work of keeping argument list and method descriptor parameter types
 * list in sync.
 */
public abstract class CallBuilder<C extends Call> {

  private Expression qualifierExpression;
  private MethodDescriptor originalMethodDescriptor;
  private List<Expression> originalArguments = new ArrayList<>();
  private List<Expression> addedArgumentExpressions = new ArrayList<>();
  private List<TypeDescriptor> addedParameterTypeDescriptors = new ArrayList<>();

  protected void initFrom(C call) {
    this.qualifierExpression = call.getQualifier();
    this.originalMethodDescriptor = call.getTarget();
    this.originalArguments = Lists.newArrayList(call.getArguments());
  }

  public CallBuilder<C> argument(
      Expression argumentExpression, TypeDescriptor parameterTypeDescriptor) {
    addedArgumentExpressions.add(argumentExpression);
    addedParameterTypeDescriptors.add(parameterTypeDescriptor);
    return this;
  }

  public CallBuilder<C> qualifier(Expression qualifierExpression) {
    this.qualifierExpression = qualifierExpression;
    return this;
  }

  public CallBuilder<C> enclosingClass(TypeDescriptor enclosingClassTypeDescriptor) {
    this.originalMethodDescriptor =
        MethodDescriptorBuilder.from(originalMethodDescriptor)
            .enclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
            .build();
    return this;
  }

  public C build() {
    List<Expression> finalArguments = new ArrayList<>();
    finalArguments.addAll(originalArguments);
    finalArguments.addAll(addedArgumentExpressions);

    MethodDescriptor finalMethodDescriptor =
        MethodDescriptors.createModifiedCopy(
            originalMethodDescriptor, addedParameterTypeDescriptors);

    return createReplacement(qualifierExpression, finalMethodDescriptor, finalArguments);
  }

  protected abstract C createReplacement(
      Expression qualifierExpression,
      MethodDescriptor finalMethodDescriptor,
      List<Expression> finalArguments);
}
