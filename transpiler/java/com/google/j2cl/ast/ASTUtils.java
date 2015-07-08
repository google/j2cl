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

/**
 * Utility functions to manipulate J2CL AST.
 */
public class ASTUtils {
  public static final String CAPTURES_PREFIX = "$c_";

  /**
   * Create "$init" MethodReference.
   */
  public static MethodDescriptor createInitMethodDescriptor(
      TypeDescriptor enclosingClassDescriptor) {
    TypeDescriptor voidTypeDescriptor = TypeDescriptor.VOID_TYPE_DESCRIPTOR;
    return MethodDescriptor.create(
        false,
        Visibility.PRIVATE,
        enclosingClassDescriptor,
        MethodDescriptor.METHOD_INIT,
        false,
        voidTypeDescriptor);
  }

  /**
   * Returns the constructor invocation (super call or this call) in a specified constructor,
   * or returns null if it does not have one.
   */
  public static MethodCall getConstructorInvocation(Method method) {
    Preconditions.checkArgument(method.isConstructor());
    if (method.getBody().getStatements().isEmpty()) {
      return null;
    }
    Statement firstStatement = method.getBody().getStatements().get(0);
    if (!(firstStatement instanceof ExpressionStatement)) {
      return null;
    }
    Expression expression = ((ExpressionStatement) firstStatement).getExpression();
    if (!(expression instanceof MethodCall)) {
      return null;
    }
    MethodCall methodCall = (MethodCall) expression;
    return methodCall.getTarget().isConstructor() ? methodCall : null;
  }

  /**
   * Returns whether the specified constructor has a this() call.
   */
  public static boolean hasThisCall(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null
        && constructorInvocation
            .getTarget()
            .getEnclosingClassDescriptor()
            .equals(method.getDescriptor().getEnclosingClassDescriptor());
  }

  /**
   * Returns whether the specified constructor has a super() call.
   */
  public static boolean hasSuperCall(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null
        && !constructorInvocation
            .getTarget()
            .getEnclosingClassDescriptor()
            .equals(method.getDescriptor().getEnclosingClassDescriptor());
  }

  /**
   * Returns whether the specified constructor has a this() or a super() call.
   */
  public static boolean hasConstructorInvocation(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null;
  }

  /**
   * Returns the added field corresponding to the captured variable.
   */
  public static FieldDescriptor createFieldForCapture(
      TypeDescriptor enclosingClassRef, Variable capturedVariable) {
    return FieldDescriptor.createRaw(
        false,
        enclosingClassRef,
        CAPTURES_PREFIX + capturedVariable.getName(),
        capturedVariable.getTypeDescriptor());
  }

  /**
   * Returns the added outer parameter in constructor corresponding to the captured variable.
   */
  public static Variable createParamForCapture(Variable capturedVariable) {
    return new Variable(CAPTURES_PREFIX + capturedVariable.getName(),
        capturedVariable.getTypeDescriptor(), true, true);
  }

  /**
   * Returns the added outer parameter in constructor corresponding to the added field.
   */
  public static Variable createOuterParamByField(Field field) {
    return new Variable(
        field.getDescriptor().getFieldName(), field.getDescriptor().getType(), true, true);
  }
}
