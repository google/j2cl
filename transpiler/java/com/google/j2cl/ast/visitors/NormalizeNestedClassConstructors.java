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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;

import java.util.HashMap;
import java.util.Map;

/**
 * Add outer parameters to constructors of nested class that has capture variables and/or enclosing
 * instances, fix calls to the constructors, and add initializers to the added fields in each
 * constructor.
 *
 * <p>
 * Normalization of nested classes are done in two parts, one is in CompilationUnitBuilder, and the
 * other one is here in NormalizeNestedClassConstructorsVisitor. CompilationUnitBuilder resolves all
 * the qualifiers and arguments. NormalizeNestedClassConstructorsVisitor does all normalization and
 * structural AST changes.
 */
public class NormalizeNestedClassConstructors extends AbstractRewriter {

  public static void applyTo(CompilationUnit compilationUnit) {
    new NormalizeNestedClassConstructors().normalizeNestedClassConstructors(compilationUnit);
  }

  private void normalizeNestedClassConstructors(CompilationUnit compilationUnit) {
    for (JavaType type : compilationUnit.getTypes()) {
      javaTypeByDescriptor.put(type.getDescriptor().getRawTypeDescriptor(), type);
    }

    // Replace new InnerClass() with the wrapper function call OuterClass.m_$create__InnerClass();
    new FixNewInstanceOfInnerClassesVisitor().applyTo(compilationUnit);

    // Create wrapper functions in outer class. This pass should be executed after replacing
    // inner class creation. Because the wrapper function calls new InnerClass() which should
    // be kept as normal NewInstance and should not be replaced.
    new InsertConstructorWrappersVisitor().applyTo(compilationUnit);

    // Add parameters to constructors so they can receive captured values.
    new AddConstructorParametersVisitor().applyTo(compilationUnit);

    // Normalize method calls to constructors.
    compilationUnit.accept(this);

    // Replace field accesses to capturing fields that hold references to the captured variables in
    // constructors with references to corresponding captured variable passing parameters.
    new FixFieldAccessInConstructorsVisitor().applyTo(compilationUnit);

    // Add capturing field initialization statements in constructors. This should be executed after
    // FixFieldAccessInConstructorsVisitor because these field accesses to the capturing fields
    // should not be replaced.
    new AddFieldInitializersVisitor().applyTo(compilationUnit);
  }

  private Map<TypeDescriptor, JavaType> javaTypeByDescriptor = new HashMap<>();

  /**
   * Visitor class that adds parameters to constructors so they can receive captured values.
   */
  private class AddConstructorParametersVisitor extends AbstractRewriter {
    public void applyTo(CompilationUnit compilationUnit) {
      compilationUnit.accept(this);
    }

    @Override
    public boolean shouldProcessJavaType(JavaType javaType) {
      return !javaType.isStatic() && javaType.getEnclosingTypeDescriptor() != null;
    }

    @Override
    public Node rewriteMethod(Method method) {
      if (!method.isConstructor()) {
        return method;
      }
      // Add parameters through which to pass captured variables for the current type.
      Method.Builder methodBuilder = Method.Builder.from(getCurrentMethod());
      for (Field capturedField : getFieldsForAllCaptures(getCurrentJavaType())) {
        Variable parameter = AstUtils.createOuterParamByField(capturedField);
        methodBuilder.parameter(parameter, parameter.getTypeDescriptor());
      }
      return methodBuilder.build();
    }
  }

  /**
   * Visitor class that replaces NewInstance of an inner class with wrapper function calls.
   */
  private class FixNewInstanceOfInnerClassesVisitor extends AbstractRewriter {
    public void applyTo(CompilationUnit compilationUnit) {
      compilationUnit.accept(this);
    }

    @Override
    public Node rewriteNewInstance(NewInstance newInstance) {
      MethodDescriptor targetMethod = newInstance.getTarget();
      TypeDescriptor targetTypeDescriptor = targetMethod.getEnclosingClassTypeDescriptor();
      if (targetTypeDescriptor.isInstanceMemberClass()) {
        // outerclass.new InnerClass() => outerClass.m_$create_InnerClass();
        TypeDescriptor outerclassTypeDescriptor = targetTypeDescriptor.getEnclosingTypeDescriptor();
        return MethodCall.createMethodCall(
            newInstance.getQualifier(),
            AstUtils.createMethodDescriptorForInnerClassCreation(
                outerclassTypeDescriptor, targetMethod),
            newInstance.getArguments());
      }
      return newInstance;
    }
  }

  /**
   * Visitor class that creates wrapper functions for inner class creation in outer class.
   */
  private class InsertConstructorWrappersVisitor extends AbstractVisitor {
    public void applyTo(CompilationUnit compilationUnit) {
      compilationUnit.accept(this);
    }

    @Override
    public void exitJavaType(JavaType javaType) {
      if (!javaType.getDescriptor().isInstanceMemberClass()) {
        return;
      }
      JavaType enclosingClass =
          javaTypeByDescriptor.get(javaType.getEnclosingTypeDescriptor().getRawTypeDescriptor());
      Preconditions.checkNotNull(enclosingClass);
      for (Method constructor : javaType.getConstructors()) {
        enclosingClass.addMethod(
            AstUtils.createMethodForInnerClassCreation(
                enclosingClass.getDescriptor(), constructor));
      }
    }
  }

  /**
   * Visitor class that adds capturing field initialization statements in constructors.
   */
  private class AddFieldInitializersVisitor extends AbstractRewriter {
    public void applyTo(CompilationUnit compilationUnit) {
      compilationUnit.accept(this);
    }

    @Override
    public boolean shouldProcessJavaType(JavaType javaType) {
      return !javaType.isStatic() && javaType.getEnclosingTypeDescriptor() != null;
    }

    @Override
    public Node rewriteMethod(Method method) {
      if (!method.isConstructor()) {
        return method;
      }
      // Maybe add capturing field initialization statements if the current constructor method does
      // not delegate to any other constructor method in the current class.
      if (!AstUtils.isDelegatedConstructorCall(
          AstUtils.getConstructorInvocation(method), getCurrentJavaType().getDescriptor())) {
        Method.Builder methodBuilder = Method.Builder.from(method);
        int i = 0;
        for (Field capturedField : getFieldsForAllCaptures(getCurrentJavaType())) {
          Variable parameter = getParameterForCapturedField(capturedField.getDescriptor(), method);
          BinaryExpression initializer =
              new BinaryExpression(
                  capturedField.getDescriptor().getTypeDescriptor(),
                  new FieldAccess(
                      new ThisReference(method.getDescriptor().getEnclosingClassTypeDescriptor()),
                      capturedField.getDescriptor()),
                  BinaryOperator.ASSIGN,
                  parameter.getReference());
          methodBuilder.statement(i++, new ExpressionStatement(initializer));
        }
        return methodBuilder.build();
      }
      return method;
    }
  }

  /**
   * Visitor class that fixes field accesses to capturing fields in a constructor.
   *
   * <p>
   * All field accesses to capturing fields in a constructor are replaced with references to the
   * corresponding parameters. Otherwise, the arguments to cascaded constructor call are evaluated
   * before the capturing fields are initialized, which may lead to wrong result.
   *
   * <p>For example,
   * <code>
   * class A {
   *   class B{
   *     final int a = 10;
   *     B (int f) {...}
   *     B () {this(a);} // if translated to B($c_a) {this(this.$c_a);} this.$c_a is evaluated to 0
   *                     // because field this.$c_a is not initialized yet.
   *   }
   * }
   * </code>
   */
  private class FixFieldAccessInConstructorsVisitor extends AbstractRewriter {
    public void applyTo(CompilationUnit compilationUnit) {
      compilationUnit.accept(this);
    }

    @Override
    public boolean shouldProcessJavaType(JavaType javaType) {
      return !javaType.isStatic() && javaType.getEnclosingTypeDescriptor() != null;
    }

    @Override
    public Node rewriteFieldAccess(FieldAccess fieldAccess) {
      Method currentMethod = getCurrentMethod();
      // replace references to added field in the constructor with the reference to parameter.
      if (currentMethod != null
          && currentMethod.isConstructor()
          && fieldAccess.getTarget().isFieldDescriptorForAllCaptures()) {
        Variable parameter = getParameterForCapturedField(fieldAccess.getTarget(), currentMethod);
        if (parameter != null) {
          return parameter.getReference();
        }
      }
      return fieldAccess;
    }
  }

  @Override
  public Node rewriteNewInstance(NewInstance newInstance) {
    NewInstance.Builder newInstanceBuilder = NewInstance.Builder.from(newInstance);
    TypeDescriptor typeDescriptor = newInstance.getTarget().getEnclosingClassTypeDescriptor();
    JavaType type = getJavaType(typeDescriptor);
    if (type == null || type.isStatic() || type.getEnclosingTypeDescriptor() == null) {
      return newInstance;
    }

    // Add arguments that reference the variables captured by the given type.
    addCapturedVariableArguments(newInstanceBuilder, typeDescriptor);

    // Maybe add the qualifier of the NewInstance as the last argument to the constructor of a
    // local class. The qualifier may be null if the local class is in a static context.
    if (type.isLocal() && newInstance.getQualifier() != null) {
      newInstanceBuilder.addArgument(
          newInstance.getQualifier(), newInstance.getQualifier().getTypeDescriptor());
    }

    return newInstanceBuilder.build();
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    MethodCall.Builder methodCallBuilder = MethodCall.Builder.from(methodCall);
    MethodDescriptor target = methodCall.getTarget();
    if (!target.isConstructor()) {
      return methodCall;
    }

    if (AstUtils.isDelegatedConstructorCall(methodCall, getCurrentJavaType().getDescriptor())) {
      // this() call, expands the given arguments list with references to the captured variable
      // passing parameters in the constructor method.
      for (Field capturedField : getFieldsForAllCaptures(getCurrentJavaType())) {
        Variable parameter =
            getParameterForCapturedField(capturedField.getDescriptor(), getCurrentMethod());
        methodCallBuilder.addArgument(parameter.getReference(), parameter.getTypeDescriptor());
      }
    } else {
      // super() call
      TypeDescriptor superTypeDescriptor = target.getEnclosingClassTypeDescriptor();
      addCapturedVariableArguments(methodCallBuilder, superTypeDescriptor);

      // a.super() => super(a)
      if (!AstUtils.hasThisReferenceAsQualifier(methodCall)) {
        methodCallBuilder
            .addArgument(
                methodCall.getQualifier(), superTypeDescriptor.getEnclosingTypeDescriptor())
            .qualifier(null);
      }
    }

    return methodCallBuilder.build();
  }

  /**
   * Expands the given arguments list with references to the variables captured by the given type.
   */
  private void addCapturedVariableArguments(
      Invocation.Builder<?> invocationBuilder, TypeDescriptor typeDescriptor) {
    JavaType type = getJavaType(typeDescriptor);
    if (type == null) {
      return;
    }

    for (Field field : type.getFields()) {
      Variable capturedVariable = field.getCapturedVariable();
      if (capturedVariable == null) {
        continue;
      }
      Field capturingField = getCapturingFieldInType(capturedVariable, getCurrentJavaType());
      if (capturingField != null) {
        // If the capturedVariable is also a captured variable in current type, pass the
        // corresponding field in current type as an argument.
        invocationBuilder.addArgument(
            new FieldAccess(new ThisReference(typeDescriptor), capturingField.getDescriptor()),
            capturingField.getDescriptor().getTypeDescriptor());
      } else {
        // otherwise, the captured variable is in the scope of the current type, so pass the
        // variable directly as an argument.
        invocationBuilder.addArgument(
            capturedVariable.getReference(), capturedVariable.getTypeDescriptor());
      }
    }
  }

  /**
   * Returns all the added fields corresponding to captured variables or enclosing instance.
   */
  private Iterable<Field> getFieldsForAllCaptures(JavaType type) {
    return Iterables.filter(
        type.getInstanceFields(),
        new Predicate<Field>() {
          @Override
          public boolean apply(Field field) {
            return field.getDescriptor().isFieldDescriptorForAllCaptures();
          }
        });
  }

  private Field getCapturingFieldInType(Variable variable, JavaType type) {
    for (Field field : type.getFields()) {
      if (field.getCapturedVariable() == variable) {
        return field;
      }
    }
    return null;
  }

  /**
   * Returns the added parameter in constructor {@code method} that corresponds to the capturing
   * field {@code fieldDescriptor}.
   */
  private Variable getParameterForCapturedField(FieldDescriptor fieldDescriptor, Method method) {
    Preconditions.checkArgument(method.isConstructor());
    Preconditions.checkArgument(fieldDescriptor.isFieldDescriptorForAllCaptures());
    for (Variable parameter : method.getParameters()) {
      if (parameter.getName().equals(fieldDescriptor.getFieldName())
          && parameter
              .getTypeDescriptor()
              .equalsIgnoreNullability(fieldDescriptor.getTypeDescriptor())
          && method
              .getDescriptor()
              .getEnclosingClassTypeDescriptor()
              .equalsIgnoreNullability(fieldDescriptor.getEnclosingClassTypeDescriptor())) {
        return parameter;
      }
    }
    return null;
  }

  private JavaType getJavaType(TypeDescriptor typeDescriptor) {
    return javaTypeByDescriptor.get(typeDescriptor.getRawTypeDescriptor());
  }
}
