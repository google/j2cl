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
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.RegularTypeDescriptor;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableReference;

import java.util.HashMap;
import java.util.Map;

/**
 * Add outer parameters to constructors of local class that has capture variables,
 * and add initializers to the added fields in each constructor.
 */
public class NormalizeLocalClassConstructorsVisitor extends AbstractRewriter {
  public static void doNormalizeLocalClassConstructors(CompilationUnit compilationUnit) {
    new NormalizeLocalClassConstructorsVisitor().normalizeLocalClassConstructors(compilationUnit);
  }

  private Map<FieldDescriptor, Variable> parameterByFieldForCaptures = new HashMap<>();

  @Override
  public boolean shouldProcessMethod(Method method) {
    // Add parameters before the method is traversed to make sure that the parameters have been
    // created when the references to them are created.
    if (isConstructorOfLocalClass(method)) {
      addParametersToConstructor(method);
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Node rewriteMethod(Method method) {
    // Add initializers after the method is traversed to avoid field accesses in the initializers
    // are replaced.
    if (isConstructorOfLocalClass(method)) {
      addInitializersToConstructor(method);
    }
    return method;
  }

  private boolean isConstructorOfLocalClass(Method method) {
    return getCurrentJavaType().isLocal() && method != null && method.isConstructor();
  }

  @Override
  public boolean shouldProcessNewInstance(NewInstance newInstance) {
    newInstance.getArguments().addAll(newInstance.getExtraArguments());
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Node rewriteFieldAccess(FieldAccess fieldAccess) {
    // replace references to added field for captures in the constructor with the reference to
    // parameter.
    if (!isConstructorOfLocalClass(getCurrentMethod())
        || !parameterByFieldForCaptures.containsKey(fieldAccess.getTarget())) {
      return fieldAccess;
    }
    return new VariableReference(
        parameterByFieldForCaptures.get(fieldAccess.getTarget()));
  }

  private void normalizeLocalClassConstructors(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  /**
   * Add outer parameters for each captured variables.
   */
  private void addParametersToConstructor(Method method) {
    Iterable<Field> capturedFields = getFieldsForCapturedVariables(getCurrentJavaType());
    for (Field field : capturedFields) {
      Variable parameter = ASTUtils.createOuterParamByField(field);
      method.addParameter(parameter);
      parameterByFieldForCaptures.put(field.getDescriptor(), parameter);
    }
  }

  /**
   * If the constructor starts with a this() call, add arguments to this() call, otherwise
   * add initializer to the added fields.
   */
  private void addInitializersToConstructor(Method method) {
    Iterable<Field> capturedFields = getFieldsForCapturedVariables(getCurrentJavaType());
    MethodCall constructorCall = ASTUtils.getConstructorInvocation(method);
    int i = 0;
    for (Field field : capturedFields) {
      Variable parameter = parameterByFieldForCaptures.get(field.getDescriptor());
      if (constructorCall != null
          && constructorCall
              .getTarget()
              .getEnclosingClassTypeDescriptor()
              .equals(getCurrentJavaType().getDescriptor())) { // this() call
        // add argument (reference to outer parameter) to this() call.
        constructorCall
            .getArguments()
            .add(new VariableReference(parameter));
      } else {
        // add initializer.
        BinaryExpression initializer =
            new BinaryExpression(
                field.getDescriptor().getTypeDescriptor(),
                new FieldAccess(
                    new ThisReference((RegularTypeDescriptor) getCurrentJavaType().getDescriptor()),
                    field.getDescriptor()),
                BinaryOperator.ASSIGN,
                new VariableReference(parameter));
        method.getBody().getStatements().add(i + 1, new ExpressionStatement(initializer));
      }
      i++;
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
}
