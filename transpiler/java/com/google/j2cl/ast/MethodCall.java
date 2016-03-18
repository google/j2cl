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
import java.util.Arrays;
import java.util.List;

/**
 * Class for method call expression.
 */
@Visitable
public class MethodCall extends Expression implements MemberReference, Call {
  /**
   * Represents the call styles: direct method call, or function.call(thisArg, ...), or
   * function.apply(thisArg, [...]).
   */
  public enum CallStyle {
    DIRECT,
    CALL
  }

  @Visitable Expression qualifier;
  @Visitable MethodDescriptor targetMethodDescriptor;
  @Visitable List<Expression> arguments = new ArrayList<>();
  private CallStyle callStyle;
  /**
   * If an instance call should be dispatched statically, e.g. A.super.method() invocation.
   */
  private boolean isStaticDispatch;

  public MethodCall(
      Expression qualifier,
      MethodDescriptor targetMethodDescriptor,
      List<Expression> arguments,
      CallStyle callStyle,
      boolean isStaticDispatch) {
    Preconditions.checkNotNull(targetMethodDescriptor);
    Preconditions.checkNotNull(arguments);
    this.qualifier = AstUtils.getExplicitQualifier(qualifier, targetMethodDescriptor);
    this.targetMethodDescriptor = targetMethodDescriptor;
    this.arguments.addAll(arguments);
    this.callStyle = callStyle;
    this.isStaticDispatch = isStaticDispatch;
  }

  public static MethodCall createRegularMethodCall(
      Expression qualifier, MethodDescriptor targetMethodDescriptor, List<Expression> arguments) {
    return new MethodCall(
        AstUtils.getExplicitQualifier(qualifier, targetMethodDescriptor),
        targetMethodDescriptor,
        arguments,
        CallStyle.DIRECT,
        false);
  }

  public static MethodCall createRegularMethodCall(
      Expression qualifier, MethodDescriptor targetMethodDescriptor, Expression... arguments) {
    return new MethodCall(
        AstUtils.getExplicitQualifier(qualifier, targetMethodDescriptor),
        targetMethodDescriptor,
        Arrays.asList(arguments),
        CallStyle.DIRECT,
        false);
  }

  public static MethodCall createCallMethodCall(
      Expression qualifier, MethodDescriptor targetMethodDescriptor, List<Expression> arguments) {
    return new MethodCall(
        AstUtils.getExplicitQualifier(qualifier, targetMethodDescriptor),
        targetMethodDescriptor,
        arguments,
        CallStyle.CALL,
        true); // 'call' style is always static dispatch.
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

  public CallStyle getCallStyle() {
    return callStyle;
  }

  public boolean isStaticDispatch() {
    return isStaticDispatch;
  }

  public void setTargetMethodDescriptor(MethodDescriptor targetMethodDescriptor) {
    this.targetMethodDescriptor = targetMethodDescriptor;
  }

  public void setCallStyle(CallStyle callStyle) {
    this.callStyle = callStyle;
  }

  public void setStaticDispatch(boolean isStaticDispatch) {
    this.isStaticDispatch = isStaticDispatch;
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
