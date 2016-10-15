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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import java.util.List;

/**
 * Verifies that the method call argument counts match the method descriptor parameter counts and
 * method declaration parameter counts match the method descriptor.
 */
public class VerifyParamAndArgCounts extends AbstractVisitor {

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new VerifyParamAndArgCounts());
  }

  @Override
  public void exitMethod(Method method) {
    verifyParameters(method.getParameters(), method.getDescriptor());
  }

  @Override
  public void exitMethodCall(MethodCall methodCall) {
    verifyArguments(methodCall.getArguments(), methodCall.getTarget());
  }

  @Override
  public void exitNewInstance(NewInstance newInstance) {
    verifyArguments(newInstance.getArguments(), newInstance.getTarget());
  }

  private void verifyArguments(
      List<Expression> passedArguments, MethodDescriptor methodDescriptor) {
    ImmutableList<TypeDescriptor> declaredParameterTypes =
        methodDescriptor.getParameterTypeDescriptors();
    if (methodDescriptor.isJsMethodVarargs()) {
      checkState(
          passedArguments.size() >= declaredParameterTypes.size() - 1,
          "Invalid method call argument count. Expected at lease %s arguments but received "
              + "%s in call to method '%s() from compilation unit %s",
          declaredParameterTypes.size(),
          passedArguments.size(),
          methodDescriptor.getName(),
          getCurrentCompilationUnit().getName());
    } else {
      checkState(
          passedArguments.size() == declaredParameterTypes.size(),
          "Invalid method call argument count. Expected %s arguments but received "
              + "%s in call to method '%s() from compilation unit %s",
          declaredParameterTypes.size(),
          passedArguments.size(),
          methodDescriptor.getName(),
          getCurrentCompilationUnit().getName());
    }
  }

  private void verifyParameters(
      List<Variable> declaredParameters, MethodDescriptor methodDescriptor) {
    ImmutableList<TypeDescriptor> declaredParameterTypes =
        methodDescriptor.getParameterTypeDescriptors();
    checkState(
        declaredParameters.size() == declaredParameterTypes.size(),
        "Invalid method call argument count. Expected %s arguments but received "
            + "%s in call to method '%s() from compilation unit %s",
        declaredParameterTypes.size(),
        declaredParameters.size(),
        methodDescriptor.getName(),
        getCurrentCompilationUnit().getName());
  }
}
