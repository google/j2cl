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
import com.google.j2cl.ast.processors.Visitable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Class for method call expression.
 */
@Visitable
public class MethodCall extends Expression implements MemberReference, Call {
  @Visitable @Nullable Expression qualifier;
  @Visitable MethodDescriptor targetMethodDescriptor;
  @Visitable List<Expression> arguments = new ArrayList<>();

  /**
   * The default constructor that creates a method of the form:
   * qualifier.targetMethodDescriptor(arguments)
   */
  public MethodCall(
      Expression qualifier, MethodDescriptor targetMethodDescriptor, List<Expression> arguments) {
    Preconditions.checkNotNull(targetMethodDescriptor);
    Preconditions.checkNotNull(arguments);
    this.qualifier = qualifier;
    this.targetMethodDescriptor = targetMethodDescriptor;
    this.arguments.addAll(arguments);
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

  public void setQualifier(Expression qualifier) {
    this.qualifier = qualifier;
  }

  public void setTargetMethodDescriptor(MethodDescriptor targetMethodDescriptor) {
    this.targetMethodDescriptor = targetMethodDescriptor;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return targetMethodDescriptor.getReturnTypeDescriptor();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_MethodCall.visit(processor, this);
  }
}
