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
import com.google.j2cl.ast.AnonymousJavaType;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodBuilder;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodCallBuilder;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NewInstanceBuilder;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    new FixSuperCallQualifiersVisitor().applyTo(compilationUnit);
    new FixAnonymousClassCreationsVisitor().applyTo(compilationUnit);
  }

  private List<Variable> constructorParameters = new ArrayList<>();
  private List<Variable> superConstructorParameters = new ArrayList<>();

  @Override
  public boolean shouldProcessJavaType(JavaType javaType) {
    if (!(javaType instanceof AnonymousJavaType)) {
      return false;
    }
    AnonymousJavaType anonymousJavaType = (AnonymousJavaType) javaType;
    // Create and collect parameters for the default constructor of only the current JavaType.
    constructorParameters.clear();
    superConstructorParameters.clear();
    int i = 0;
    for (TypeDescriptor parameterTypeDescriptor :
        anonymousJavaType.getConstructorParameterTypeDescriptors()) {
      Variable parameter = new Variable("$_" + i++, parameterTypeDescriptor, false, true);
      constructorParameters.add(parameter);
    }
    int j = 0;
    for (TypeDescriptor parameterTypeDescriptor :
        anonymousJavaType.getSuperConstructorParameterTypeDescriptors()) {
      Variable parameter = new Variable("$_" + j++, parameterTypeDescriptor, false, true);
      superConstructorParameters.add(parameter);
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
    for (Variable parameter : superConstructorParameters) {
      methodCallBuilder.argument(parameter.getReference(), parameter.getTypeDescriptor());
    }

    return methodCallBuilder.build();
  }

  /**
   * Visitor class that fixes super() call qualifiers in anonymous classes.
   *
   * <p>The qualifier of a NewInstance that creates an anonymous class is actually the qualifier
   * of the super call and is a reference to what will be the enclosing instance for the anonymous
   * class's super type. For example, q.new A(){}; A is an instance inner class, q is actually the
   * qualifier that is used to create the super instance. This is translated as follows:
   * class $1 extends A {
   *   $1($sq) {$sq.super();}
   * }
   * q.new A(){} => this.new $1(q);
   */
  private class FixSuperCallQualifiersVisitor extends AbstractRewriter {
    public void applyTo(CompilationUnit compilationUnit) {
      compilationUnit.accept(this);
    }

    private Variable parameterForSuperCallQualifier;

    @Override
    public boolean shouldProcessJavaType(JavaType javaType) {
      if (!(javaType instanceof AnonymousJavaType)) {
        return false;
      }
      parameterForSuperCallQualifier = null;
      // Create parameter that holds the qualifier of super call.
      if (javaType.getSuperTypeDescriptor().isInstanceMemberClass()) {
        parameterForSuperCallQualifier =
            new Variable(
                "$super_outer_this",
                javaType.getSuperTypeDescriptor().getEnclosingTypeDescriptor(),
                false,
                true);
      }
      return true;
    }

    @Override
    public Node rewriteMethod(Method method) {
      // Add parameter to the constructor.
      if (!method.isConstructor() || parameterForSuperCallQualifier == null) {
        return method;
      }
      MethodBuilder methodBuilder = MethodBuilder.from(method);
      methodBuilder.parameter(
          parameterForSuperCallQualifier, parameterForSuperCallQualifier.getTypeDescriptor());
      return methodBuilder.build();
    }

    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      Method currentMethod = getCurrentMethod();
      if (currentMethod == null
          || !currentMethod.isConstructor()
          || !methodCall.getTarget().isConstructor()) {
        return methodCall;
      }
      // super() call, set the qualifier.
      MethodCallBuilder methodCallBuilder = MethodCallBuilder.from(methodCall);
      if (parameterForSuperCallQualifier != null) {
        methodCallBuilder.qualifier(parameterForSuperCallQualifier.getReference());
      }

      return methodCallBuilder.build();
    }
  }

  /**
   * Visitor class that fixes the NewInstance call of an anonymous class.
   *
   * <p>If an anonymous class is created with a qualifier, pass the qualifier as an argument.
   * For example, q.new A(){}; // A is an instance inner class, is translated to
   * new $1(q);
   */
  private class FixAnonymousClassCreationsVisitor extends AbstractRewriter {
    public void applyTo(CompilationUnit compilationUnit) {
      collectSuperCallQualifiers(compilationUnit);
      compilationUnit.accept(this);
    }

    private Map<TypeDescriptor, Expression> qualifierByTypeDescriptor = new HashMap<>();

    private void collectSuperCallQualifiers(CompilationUnit compilationUnit) {
      for (JavaType javaType : compilationUnit.getTypes()) {
        if (javaType instanceof AnonymousJavaType) {
          AnonymousJavaType anonymousJavaType = (AnonymousJavaType) javaType;
          if (anonymousJavaType.getSuperTypeDescriptor().isInstanceMemberClass()) {
            qualifierByTypeDescriptor.put(
                javaType.getDescriptor(), anonymousJavaType.getSuperCallQualifier());
          }
        }
      }
    }

    @Override
    public Node rewriteNewInstance(NewInstance newInstance) {
      TypeDescriptor typeDescriptor = newInstance.getTypeDescriptor();
      if (!qualifierByTypeDescriptor.containsKey(typeDescriptor)) {
        return newInstance;
      }
      NewInstanceBuilder newInstanceBuilder = NewInstanceBuilder.from(newInstance);
      newInstanceBuilder.argument(
          qualifierByTypeDescriptor.get(typeDescriptor),
          typeDescriptor.getSuperTypeDescriptor().getEnclosingTypeDescriptor());
      return newInstanceBuilder.build();
    }
  }
}
