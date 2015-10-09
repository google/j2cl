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
import com.google.j2cl.ast.CallBuilder;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodBuilder;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodCallBuilder;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NewInstanceBuilder;
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
 * <p>Normalization of nested classes are done in two parts, one is in CompilationUnitBuilder, and
 * the other one is here in NormalizeNestedClassConstructorsVisitor. CompilationUnitBuilder resolves
 * all the qualifiers and arguments. NormalizeNestedClassConstructorsVisitor does all normalization
 * and structural AST changes.
 */
public class NormalizeNestedClassConstructorsVisitor extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    new NormalizeNestedClassConstructorsVisitor().normalizeNestedClassConstructors(compilationUnit);
  }

  private void normalizeNestedClassConstructors(CompilationUnit compilationUnit) {
    for (JavaType type : compilationUnit.getTypes()) {
      javaTypeByDescriptor.put(type.getDescriptor().getRawTypeDescriptor(), type);
      if (type.getEnclosingTypeDescriptor() == null || type.isStatic()) {
        continue;
      }
      // Creates parameters for instance nested classes' constructors.
      for (Method method : type.getMethods()) {
        if (method.isConstructor()) {
          createParametersOfConstructor(type);
        }
      }
    }

    // Replace new InnerClass() with the wrapper function call OuterClass.m_$create__InnerClass();
    new FixNewInstanceOfInnerClassesVisitor().applyTo(compilationUnit);

    // Create wrapper functions in outer class. This pass should be executed after replacing
    // inner class creation. Because the wrapper function calls new InnerClass() which should
    // be kept as normal NewInstance and should not be replaced.
    new InsertConstructorWrappersVisitor().applyTo(compilationUnit);

    // Normalize nested class constructors.
    compilationUnit.accept(this);
  }

  private Map<FieldDescriptor, Variable> parameterByFieldForCaptures = new HashMap<>();
  private Map<TypeDescriptor, JavaType> javaTypeByDescriptor = new HashMap<>();

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
        return new MethodCall(
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

  @Override
  public Node rewriteMethod(Method method) {
    MethodBuilder methodBuilder = MethodBuilder.from(method);
    if (!AstUtils.isConstructorOfImmediateNestedClass(method, getCurrentJavaType())) {
      return method;
    }

    // Maybe add capturing field initialization statements if the current constructor method does
    // not delegate to any other constructor method in the current class.
    if (!AstUtils.isDelegatedConstructorCall(
        AstUtils.getConstructorInvocation(method), getCurrentJavaType().getDescriptor())) {
      addCapturingFieldInitializers(
          methodBuilder, method.getDescriptor().getEnclosingClassTypeDescriptor());
    }

    // Add parameters through which to pass captured variables for the current type.
    addCapturedValueParameters(methodBuilder);

    return methodBuilder.build();
  }

  @Override
  public Node rewriteNewInstance(NewInstance newInstance) {
    NewInstanceBuilder newInstanceBuilder = NewInstanceBuilder.from(newInstance);
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
      newInstanceBuilder.argument(
          newInstance.getQualifier(), newInstance.getQualifier().getTypeDescriptor());
    }

    return newInstanceBuilder.build();
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    MethodCallBuilder methodCallBuilder = MethodCallBuilder.from(methodCall);
    MethodDescriptor target = methodCall.getTarget();
    if (!target.isConstructor()) {
      return methodCall;
    }

    if (AstUtils.isDelegatedConstructorCall(methodCall, getCurrentJavaType().getDescriptor())) {
      // this() call
      addCapturedVariableArgumentsInConstructorCascade(methodCallBuilder);
    } else {
      // super() call
      TypeDescriptor superTypeDescriptor = target.getEnclosingClassTypeDescriptor();
      addCapturedVariableArguments(methodCallBuilder, superTypeDescriptor);

      // a.super() => super(a)
      if (methodCall.getQualifier() != null) {
        methodCallBuilder
            .argument(methodCall.getQualifier(), superTypeDescriptor.getEnclosingTypeDescriptor())
            .qualifier(null);
      }
    }

    return methodCallBuilder.build();
  }

  @Override
  public Node rewriteFieldAccess(FieldAccess fieldAccess) {
    // replace references to added field in the constructor with the reference to parameter.
    if (AstUtils.isConstructorOfImmediateNestedClass(getCurrentMethod(), getCurrentJavaType())
        && parameterByFieldForCaptures.containsKey(fieldAccess.getTarget())
        && fieldAccess.getTarget().getEnclosingClassTypeDescriptor()
            == getCurrentJavaType().getDescriptor()) {
      return parameterByFieldForCaptures.get(fieldAccess.getTarget()).getReference();
    }
    return fieldAccess;
  }

  /**
   * Expands the given arguments list with references to the variables captured by the given type.
   */
  private void addCapturedVariableArguments(
      CallBuilder<?> callBuilder, TypeDescriptor typeDescriptor) {
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
        callBuilder.argument(
            new FieldAccess(new ThisReference(typeDescriptor), capturingField.getDescriptor()),
            capturingField.getDescriptor().getTypeDescriptor());
      } else {
        // otherwise, the captured variable is in the scope of the current type, so pass the
        // variable directly as an argument.
        callBuilder.argument(capturedVariable.getReference(), capturedVariable.getTypeDescriptor());
      }
    }
  }

  /**
   * Runs in the context of a 'this()' call inside of a constructor and so expands the given
   * arguments list with references to the captured variable passing parameters in the current
   * constructor method.
   */
  private void addCapturedVariableArgumentsInConstructorCascade(
      MethodCallBuilder methodCallBuilder) {
    for (Field capturedField : getFieldsForCapturedVariables(getCurrentJavaType())) {
      Variable parameter = parameterByFieldForCaptures.get(capturedField.getDescriptor());
      methodCallBuilder.argument(parameter.getReference(), parameter.getTypeDescriptor());
    }
  }

  /**
   * Expands the given statements list with initializer statements for the capturing fields that
   * hold references to the variables captured by the current type.
   */
  private void addCapturingFieldInitializers(
      MethodBuilder methodBuilder, TypeDescriptor enclosingTypeDescriptor) {
    int i = 0;
    for (Field capturedField : getFieldsForCapturedVariables(getCurrentJavaType())) {
      Variable parameter = parameterByFieldForCaptures.get(capturedField.getDescriptor());
      BinaryExpression initializer =
          new BinaryExpression(
              capturedField.getDescriptor().getTypeDescriptor(),
              new FieldAccess(
                  new ThisReference(enclosingTypeDescriptor), capturedField.getDescriptor()),
              BinaryOperator.ASSIGN,
              parameter.getReference());
      methodBuilder.statement(i++, new ExpressionStatement(initializer));
    }
  }

  /**
   * Expands the given parameters list with parameters through which to pass captured variables for
   * the current type.
   */
  private void addCapturedValueParameters(MethodBuilder methodBuilder) {
    for (Field capturedField : getFieldsForCapturedVariables(getCurrentJavaType())) {
      Variable parameter = parameterByFieldForCaptures.get(capturedField.getDescriptor());
      methodBuilder.parameter(parameter, parameter.getTypeDescriptor());
    }
  }

  /**
   * Creates parameters for captures and enclosing instance.
   */
  private void createParametersOfConstructor(JavaType type) {
    for (Field capturedField : getFieldsForCapturedVariables(type)) {
      Variable parameter = AstUtils.createOuterParamByField(capturedField);
      parameterByFieldForCaptures.put(capturedField.getDescriptor(), parameter);
    }
  }

  /**
   * Returns all the added fields corresponding to captured variables.
   */
  private Iterable<Field> getFieldsForCapturedVariables(JavaType type) {
    return Iterables.filter(
        type.getInstanceFields(),
        new Predicate<Field>() {
          @Override
          public boolean apply(Field field) {
            return field.isCapturedField();
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

  private JavaType getJavaType(TypeDescriptor typeDescriptor) {
    return javaTypeByDescriptor.get(typeDescriptor.getRawTypeDescriptor());
  }
}
