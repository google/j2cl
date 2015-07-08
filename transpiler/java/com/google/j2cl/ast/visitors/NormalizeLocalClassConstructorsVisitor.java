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
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableReference;

/**
 * Add outer parameters to constructors of local class that has capture variables,
 * and add initializers to the added fields in each constructor.
 */
public class NormalizeLocalClassConstructorsVisitor extends AbstractVisitor {
  public static void doNormalizeLocalClassConstructors(CompilationUnit compilationUnit) {
    new NormalizeLocalClassConstructorsVisitor().normalizeLocalClassConstructors(compilationUnit);
  }

  private JavaType currentType;
  private Method currentMethod;

  @Override
  public boolean enterJavaType(JavaType type) {
    currentType = type;
    return true;
  }

  @Override
  public void exitJavaType(JavaType type) {
    currentType = null;
  }

  @Override
  public boolean enterMethod(Method method) {
    currentMethod = method;
    addParametersToConstructor(method);
    return true;
  }

  @Override
  public void exitMethod(Method method) {
    currentMethod = null;
  }

  @Override
  public boolean enterNewInstance(NewInstance newInstance) {
    newInstance.getArguments().addAll(newInstance.getExtraArguments());
    return true;
  }

  @Override
  public boolean enterVariableReference(VariableReference variableRef) {
    // replace references to outer parameters in the constructor with the real outer parameter.
    if (!currentType.isLocal() || currentMethod == null || !currentMethod.isConstructor()
        || !variableRef.getTarget().isParameter()) {
      return false;
    }
    Variable parameter = findParamByName(variableRef.getTarget().getName(), currentMethod);
    Preconditions.checkNotNull(parameter);
    variableRef.setTarget(parameter);
    return false;
  }

  private Variable findParamByName(String name, Method method) {
    for (Variable parameter : method.getParameters()) {
      if (parameter.getName().equals(name)) {
        return parameter;
      }
    }
    return null;
  }

  private void normalizeLocalClassConstructors(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  /**
   * Add outer parameters for each captured variables.
   * <p>
   * If the constructor starts with a this() call, add arguments to this() call, otherwise
   * add initializer to the added fields.
   */
  private void addParametersToConstructor(Method method) {
    // only applys on constructors of local class.
    if (!currentType.isLocal() || !method.isConstructor()) {
      return;
    }
    Iterable<Field> capturedFields = getFieldsForCapturedVariables(currentType);
    MethodCall constructorCall = ASTUtils.getConstructorInvocation(method);
    int i = 0;
    for (Field field : capturedFields) {
      Variable parameter = ASTUtils.createOuterParamByField(field);
      method.addParameter(parameter);
      if (constructorCall != null
          && constructorCall.getTarget().getEnclosingClassDescriptor().equals(
              currentType.getDescriptor())) { // this() call
        // add argument (reference to outer parameter) to this() call.
        constructorCall.getArguments().add(new VariableReference(parameter));
      } else {
        // add initializer.
        BinaryExpression initializer =
            new BinaryExpression(new FieldAccess(new ThisReference(null), field.getDescriptor()),
                BinaryOperator.ASSIGN, new VariableReference(parameter));
        method.getBody().getStatements().add(i + 1, new ExpressionStatement(initializer));
      }
      i++;
    }
  }

  /**
   * Returns all the added fields corresponding to captured variables.
   */
  private Iterable<Field> getFieldsForCapturedVariables(JavaType type) {
    return Iterables.filter(type.getInstanceFields(), new Predicate<Field>() {
      @Override
      public boolean apply(Field field) {
        return field.isCapturedField();
      }
    });
  }
}
