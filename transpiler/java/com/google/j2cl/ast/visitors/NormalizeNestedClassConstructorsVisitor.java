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
import com.google.j2cl.ast.ASTUtils;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Add outer parameters to constructors of nested class that has capture variables and/or enclosing
 * instances, and add initializers to the added fields in each constructor.
 */
public class NormalizeNestedClassConstructorsVisitor extends AbstractRewriter {
  public static void doNormalizeNestedClassConstructors(CompilationUnit compilationUnit) {
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
    compilationUnit.accept(this);
  }

  private Map<FieldDescriptor, Variable> parameterByFieldForCaptures = new HashMap<>();
  private Map<TypeDescriptor, JavaType> javaTypeByDescriptor = new HashMap<>();

  @SuppressWarnings("unchecked")
  @Override
  public Node rewriteMethod(Method method) {
    if (!isConstructorOfNestedClass(method)) {
      return method;
    }
    MethodDescriptor methodDescriptor = method.getDescriptor();
    addParamsAndInitsToConstructor(method);
    return new Method(
        methodDescriptor, method.getParameters(), method.getBody(), method.isOverride());
  }

  @Override
  public Node rewriteNewInstance(NewInstance newInstance) {
    TypeDescriptor typeDescriptor =
        newInstance.getConstructorMethodDescriptor().getEnclosingClassTypeDescriptor();
    JavaType type = javaTypeByDescriptor.get(typeDescriptor.getRawTypeDescriptor());
    if (type == null || type.isStatic() || type.getEnclosingTypeDescriptor() == null) {
      return newInstance;
    }
    List<Expression> arguments = new ArrayList<>(newInstance.getArguments());
    arguments.addAll(collectArgumentsForCaptures(typeDescriptor));
    // add 'this' as the last argument to the constructor of a local class.
    if (type.isLocal()) {
      TypeDescriptor currentTypeDescriptor = getCurrentJavaType().getDescriptor();
      // Expression: this;
      Expression outerThisExpression = new ThisReference(currentTypeDescriptor);

      // Lambdas never enclose any other type, so when an inner class is defined inside of a lambda
      // its recorded enclosing type is actually the enclosing type of the lambda.
      while (type.getEnclosingTypeDescriptor() != currentTypeDescriptor) {
        // To reference that progressively more distant enclosing instance expand:
        // Expression: this -> this.$outer_this;
        JavaType currentType =
            javaTypeByDescriptor.get(currentTypeDescriptor.getRawTypeDescriptor());
        Field enclosingInstanceField = ASTUtils.getEnclosingInstanceField(currentType);
        outerThisExpression =
            new FieldAccess(outerThisExpression, enclosingInstanceField.getDescriptor());

        currentTypeDescriptor = enclosingInstanceField.getDescriptor().getTypeDescriptor();
      }

      arguments.add(outerThisExpression);
    }
    MethodDescriptor target = newInstance.getConstructorMethodDescriptor();
    Preconditions.checkArgument(newInstance.getQualifier() == null);
    return new NewInstance(null, target, arguments);
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    MethodDescriptor target = methodCall.getTarget();
    List<Expression> arguments = new ArrayList<>(methodCall.getArguments());
    Expression qualifier = methodCall.getQualifier();
    if (target.isConstructor() // super() call
        && !target.getEnclosingClassTypeDescriptor().equals(getCurrentJavaType().getDescriptor())) {
      arguments.addAll(collectArgumentsForCaptures(target.getEnclosingClassTypeDescriptor()));
      // a.super() => super(a)
      if (methodCall.getQualifier() != null) {
        arguments.add(methodCall.getQualifier());
        qualifier = null;
      }
    }
    return new MethodCall(qualifier, target, arguments);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Node rewriteFieldAccess(FieldAccess fieldAccess) {
    // replace references to added field in the constructor with the reference to parameter.
    if (!isConstructorOfNestedClass(getCurrentMethod())
        || !parameterByFieldForCaptures.containsKey(fieldAccess.getTarget())) {
      return fieldAccess;
    }
    return parameterByFieldForCaptures.get(fieldAccess.getTarget()).getReference();
  }

  private List<Expression> collectArgumentsForCaptures(TypeDescriptor typeDescriptor) {
    List<Expression> extraArguments = new ArrayList<>();
    JavaType type = javaTypeByDescriptor.get(typeDescriptor.getRawTypeDescriptor());
    if (type == null) {
      return extraArguments;
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
        extraArguments.add(
            new FieldAccess(new ThisReference(typeDescriptor), capturingField.getDescriptor()));
      } else {
        // otherwise, the captured variable is in the scope of the current type, so pass the
        // variable directly as an argument.
        extraArguments.add(capturedVariable.getReference());
      }
    }
    return extraArguments;
  }

  /**
   * Add parameters and initializers to the added fields.
   */
  private void addParamsAndInitsToConstructor(Method method) {
    Iterable<Field> capturedFields = getFieldsForCapturedVariables(getCurrentJavaType());
    MethodCall constructorCall = ASTUtils.getConstructorInvocation(method);
    int i = 0;
    boolean isThisCall =
        constructorCall != null
            && constructorCall
                .getTarget()
                .getEnclosingClassTypeDescriptor()
                .equals(getCurrentJavaType().getDescriptor());
    for (Field field : capturedFields) {
      Variable parameter = parameterByFieldForCaptures.get(field.getDescriptor());
      method.addParameter(parameter);
      if (isThisCall) { // this() call
        // add argument (reference to outer parameter) to this() call.
        constructorCall.getArguments().add(parameter.getReference());
      } else {
        // add initializer.
        BinaryExpression initializer =
            new BinaryExpression(
                field.getDescriptor().getTypeDescriptor(),
                new FieldAccess(
                    new ThisReference(method.getDescriptor().getEnclosingClassTypeDescriptor()),
                    field.getDescriptor()),
                BinaryOperator.ASSIGN,
                parameter.getReference());
        method.getBody().getStatements().add(i + 1, new ExpressionStatement(initializer));
      }
      i++;
    }
  }

  private boolean isConstructorOfNestedClass(Method method) {
    return getCurrentJavaType().getEnclosingTypeDescriptor() != null
        && !getCurrentJavaType().isStatic()
        && method != null
        && method.isConstructor();
  }

  /**
   * Creates parameters for captures and enclosing instance.
   */
  private void createParametersOfConstructor(JavaType type) {
    Iterable<Field> capturedFields = getFieldsForCapturedVariables(type);
    for (Field field : capturedFields) {
      Variable parameter = ASTUtils.createOuterParamByField(field);
      parameterByFieldForCaptures.put(field.getDescriptor(), parameter);
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
}
