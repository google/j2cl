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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodBuilder;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodCallBuilder;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes synthesized default constructor for anonymous classes.
 *
 * <p>The default constructor of an anonymous class is defined by the anonymous class expression,
 * and sometimes has non-empty parameter list. Then the synthesized super call should also be fixed
 * to call corresponding constructors.
 *
 * <p>For example, A a = new A(1) {}. The default constructor of the anonymous class should have
 * a parameter list of (int), and the super call should call A(int).
 */
public class FixAnonymousClassConstructorsVisitor extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    new FixAnonymousClassConstructorsVisitor()
        .fixAnonymousClassConstructorsVisitor(compilationUnit);
  }

  private void fixAnonymousClassConstructorsVisitor(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  private List<Variable> constructorParameters = new ArrayList<>();

  @Override
  public boolean shouldProcessJavaType(JavaType javaType) {
    if (javaType.getConstructorParameterTypeDescriptors().isEmpty()) {
      return false;
    }
    // Create and collect parameters for the default constructor of only the current JavaType.
    constructorParameters.clear();
    int i = 0;
    for (TypeDescriptor parameterTypeDescriptor :
        getCurrentJavaType().getConstructorParameterTypeDescriptors()) {
      Variable parameter = new Variable("$_" + i++, parameterTypeDescriptor, false, true);
      constructorParameters.add(parameter);
    }
    return true;
  }

  @Override
  public Node rewriteMethod(Method method) {
    if (!method.isConstructor()) {
      return method;
    }
    // Add parameters to the constructor.
    MethodBuilder methodBuilder = MethodBuilder.from(method);
    for (Variable parameter : constructorParameters) {
      methodBuilder.parameter(parameter, parameter.getTypeDescriptor());
    }

    return methodBuilder.build();
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    /*
     * Add arguments to super() calls inside of constructor methods in anonymous classes.
     */
    if (getCurrentMethod() == null
        || !getCurrentMethod().isConstructor()
        || !methodCall.getTarget().isConstructor()) {
      return methodCall;
    }

    MethodCallBuilder methodCallBuilder = MethodCallBuilder.from(methodCall);
    for (Variable parameter : constructorParameters) {
      methodCallBuilder.argument(parameter.getReference(), parameter.getTypeDescriptor());
    }

    return methodCallBuilder.build();
  }
}
