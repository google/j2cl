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
import com.google.j2cl.ast.AnonymousType;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.Type;
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
 * <p>For example, A a = new A(1) {}. The default constructor of the anonymous class should have a
 * parameter list of (int), and the super call should call A(int).
 */
public class FixAnonymousClassConstructors extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new AnonymousClassConstructorRewriter());
    compilationUnit.accept(new FixSuperCallQualifiersVisitor());
    new FixAnonymousClassCreationsVisitor().applyTo(compilationUnit);
  }

  private static class AnonymousClassConstructorRewriter extends AbstractRewriter {
    private final List<Variable> constructorParameters = new ArrayList<>();

    @Override
    public boolean shouldProcessType(Type type) {
      if (!(type instanceof AnonymousType)) {
        return false;
      }
      AnonymousType anonymousType = (AnonymousType) type;
      // Create and collect parameters for the default constructor of only the current Type.
      constructorParameters.clear();
      int i = 0;
      for (TypeDescriptor parameterTypeDescriptor :
          anonymousType.getConstructorParameterTypeDescriptors()) {
        Variable parameter =
            Variable.Builder.fromDefault()
                .setName("$_" + i++)
                .setTypeDescriptor(parameterTypeDescriptor)
                .setIsParameter(true)
                .build();
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
      return Method.Builder.from(method).addParameters(constructorParameters).build();
    }

    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      /*
       * Add arguments to super() calls inside of constructor methods in anonymous classes.
       */
      if (!getCurrentMember().isConstructor() || !methodCall.getTarget().isConstructor()) {
        return methodCall;
      }

      return MethodCall.Builder.from(methodCall)
          .appendArgumentsAndUpdateDescriptors(
              AstUtils.getReferences(constructorParameters),
              ((AnonymousType) getCurrentType()).getSuperConstructorParameterTypeDescriptors())
          .build();
    }
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
  private static class FixSuperCallQualifiersVisitor extends AbstractRewriter {

    private Variable parameterForSuperCallQualifier;

    @Override
    public boolean shouldProcessType(Type type) {
      if (!(type instanceof AnonymousType)) {
        return false;
      }
      parameterForSuperCallQualifier = null;
      // Create parameter that holds the qualifier of super call.
      if (type.getSuperTypeDescriptor().isInstanceMemberClass()) {
        parameterForSuperCallQualifier =
            Variable.Builder.fromDefault()
                .setName("$super_outer_this")
                .setTypeDescriptor(type.getSuperTypeDescriptor().getEnclosingTypeDescriptor())
                .setIsParameter(true)
                .build();
      }
      return true;
    }

    @Override
    public Node rewriteMethod(Method method) {
      // Add parameter to the constructor.
      if (!method.isConstructor() || parameterForSuperCallQualifier == null) {
        return method;
      }
      return Method.Builder.from(method).addParameters(parameterForSuperCallQualifier).build();
    }

    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      if (!getCurrentMember().isConstructor() || !methodCall.getTarget().isConstructor()) {
        return methodCall;
      }
      // super() call, set the qualifier.
      MethodCall.Builder methodCallBuilder = MethodCall.Builder.from(methodCall);
      if (parameterForSuperCallQualifier != null) {
        methodCallBuilder.setQualifier(parameterForSuperCallQualifier.getReference());
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
  private static class FixAnonymousClassCreationsVisitor extends AbstractRewriter {
    public void applyTo(CompilationUnit compilationUnit) {
      collectSuperCallQualifiers(compilationUnit);
      compilationUnit.accept(this);
    }

    private Map<TypeDescriptor, Expression> qualifierByTypeDescriptor = new HashMap<>();

    private void collectSuperCallQualifiers(CompilationUnit compilationUnit) {
      for (Type type : compilationUnit.getTypes()) {
        if (type instanceof AnonymousType) {
          AnonymousType anonymousType = (AnonymousType) type;
          if (anonymousType.getSuperTypeDescriptor().isInstanceMemberClass()) {
            qualifierByTypeDescriptor.put(
                type.getDescriptor(), anonymousType.getSuperCallQualifier());
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

      return NewInstance.Builder.from(newInstance)
          .appendArgumentAndUpdateDescriptor(
              qualifierByTypeDescriptor.get(typeDescriptor),
              typeDescriptor.getSuperTypeDescriptor().getEnclosingTypeDescriptor())
          .build();
    }
  }
}
