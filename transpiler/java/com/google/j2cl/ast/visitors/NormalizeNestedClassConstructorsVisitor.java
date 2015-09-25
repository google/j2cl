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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.ASTUtils;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
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
import com.google.j2cl.ast.Statement;
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

  @Override
  public Node rewriteMethod(Method method) {
    if (!ASTUtils.isConstructorOfImmediateNestedClass(method, getCurrentJavaType())) {
      return method;
    }

    // Maybe add capturing field initialization statements if the current constructor method does
    // not delegate to any other constructor method in the current class.
    List<Statement> statements = new ArrayList<>(method.getBody().getStatements());
    if (!ASTUtils.isDelegatedConstructorCall(
        ASTUtils.getConstructorInvocation(statements), getCurrentJavaType().getDescriptor())) {
      addCapturingFieldInitializers(
          statements, method.getDescriptor().getEnclosingClassTypeDescriptor());
    }

    // Add parameters through which to pass captured variables for the current type.
    List<Variable> parameters = new ArrayList<>(method.getParameters());
    addCapturedValueParameters(parameters);

    return new Method(
        method.getDescriptor(), parameters, new Block(statements), method.isOverride());
  }

  @Override
  public Node rewriteNewInstance(NewInstance newInstance) {
    TypeDescriptor typeDescriptor =
        newInstance.getConstructorMethodDescriptor().getEnclosingClassTypeDescriptor();
    JavaType type = getJavaType(typeDescriptor);
    if (type == null || type.isStatic() || type.getEnclosingTypeDescriptor() == null) {
      return newInstance;
    }

    // Add arguments that reference the variables captured by the given type.
    List<Expression> arguments = new ArrayList<>(newInstance.getArguments());
    addCapturedVariableArguments(arguments, typeDescriptor);

    // Maybe add the qualifier of the NewInstance as the last argument to the constructor of a
    // local class. The qualifier may be null if the local class is in a static context.
    if (type.isLocal() && newInstance.getQualifier() != null) {
      arguments.add(newInstance.getQualifier());
    }

    return new NewInstance(null, newInstance.getConstructorMethodDescriptor(), arguments);
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    MethodDescriptor target = methodCall.getTarget();
    if (!target.isConstructor()) {
      return methodCall;
    }

    Expression qualifier = methodCall.getQualifier();
    List<Expression> arguments = new ArrayList<>(methodCall.getArguments());

    if (ASTUtils.isDelegatedConstructorCall(methodCall, getCurrentJavaType().getDescriptor())) {
      // this() call
      addCapturedVariableArgumentsInConstructorCascade(arguments);
    } else {
      // super() call
      addCapturedVariableArguments(arguments, target.getEnclosingClassTypeDescriptor());

      // a.super() => super(a)
      if (methodCall.getQualifier() != null) {
        arguments.add(methodCall.getQualifier());
        qualifier = null;
      }
    }

    return new MethodCall(qualifier, target, arguments);
  }

  @Override
  public Node rewriteFieldAccess(FieldAccess fieldAccess) {
    // replace references to added field in the constructor with the reference to parameter.
    if (ASTUtils.isConstructorOfImmediateNestedClass(getCurrentMethod(), getCurrentJavaType())
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
      List<Expression> arguments, TypeDescriptor typeDescriptor) {
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
        arguments.add(
            new FieldAccess(new ThisReference(typeDescriptor), capturingField.getDescriptor()));
      } else {
        // otherwise, the captured variable is in the scope of the current type, so pass the
        // variable directly as an argument.
        arguments.add(capturedVariable.getReference());
      }
    }
  }

  /**
   * Runs in the context of a 'this()' call inside of a constructor and so expands the given
   * arguments list with references to the captured variable passing parameters in the current
   * constructor method.
   */
  private void addCapturedVariableArgumentsInConstructorCascade(List<Expression> arguments) {
    for (Field capturedField : getFieldsForCapturedVariables(getCurrentJavaType())) {
      arguments.add(parameterByFieldForCaptures.get(capturedField.getDescriptor()).getReference());
    }
  }

  /**
   * Expands the given statements list with initializer statements for the capturing fields that
   * hold references to the variables captured by the current type.
   */
  private void addCapturingFieldInitializers(
      List<Statement> statements, TypeDescriptor enclosingTypeDescriptor) {
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
      statements.add(i++, new ExpressionStatement(initializer));
    }
  }

  /**
   * Expands the given parameters list with parameters through which to pass captured variables for
   * the current type.
   */
  private void addCapturedValueParameters(List<Variable> parameters) {
    for (Field capturedField : getFieldsForCapturedVariables(getCurrentJavaType())) {
      parameters.add(parameterByFieldForCaptures.get(capturedField.getDescriptor()));
    }
  }

  /**
   * Creates parameters for captures and enclosing instance.
   */
  private void createParametersOfConstructor(JavaType type) {
    for (Field capturedField : getFieldsForCapturedVariables(type)) {
      Variable parameter = ASTUtils.createOuterParamByField(capturedField);
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
