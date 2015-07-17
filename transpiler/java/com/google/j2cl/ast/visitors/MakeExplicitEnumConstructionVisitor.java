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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.j2cl.ast.ASTUtils;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;

import java.util.List;

/**
 * Make the implicit parameters and super calls in enum constructors explicit.
 */
public class MakeExplicitEnumConstructionVisitor extends AbstractRewriter {
  private static final String ORDINAL_PARAMETER_NAME = "$ordinal";
  private static final String VALUE_NAME_PARAMETER_NAME = "$name";
  private Multiset<TypeDescriptor> ordinalsByTypeDescriptor = HashMultiset.create();

  public static void doMakeEnumConstructionExplicit(CompilationUnit compilationUnit) {
    new MakeExplicitEnumConstructionVisitor().makeEnumConstructionExplicit(compilationUnit);
  }

  @Override
  public Method rewriteMethod(Method method) {
    /*
     * Only inserts explicit super() call to a constructor that does not have
     * a super() or this() call, and the corresponding type does have a super class.
     */
    if (!method.isConstructor() || !getCurrentJavaType().isEnum()) {
      return method;
    }
    Variable ordinalVariable =
        new Variable(ORDINAL_PARAMETER_NAME, TypeDescriptors.INT_TYPE_DESCRIPTOR, false, true);
    Variable nameVariable =
        new Variable(
            VALUE_NAME_PARAMETER_NAME, TypeDescriptors.STRING_TYPE_DESCRIPTOR, false, true);
    method.addParameter(nameVariable);
    method.addParameter(ordinalVariable);
    replaceConstructorInvocationCall(method, ImmutableList.of(nameVariable, ordinalVariable));

    return new Method(
        ASTUtils.addParametersToMethodDescriptor(
            method.getDescriptor(),
            nameVariable.getTypeDescriptor(),
            ordinalVariable.getTypeDescriptor()),
        method.getParameters(),
        method.getBody());
  }

  @Override
  public NewInstance rewriteNewInstance(NewInstance newInstance) {
    TypeDescriptor targetTypeDescriptor =
        newInstance.getConstructorMethodDescriptor().getEnclosingClassTypeDescriptor();

    if (!getCurrentJavaType().isEnum()
        || !targetTypeDescriptor.equals(getCurrentJavaType().getDescriptor())) {
      return newInstance;
    }

    Field enumField = getCurrentField();
    Preconditions.checkState(
        enumField != null,
        "Enum values can only be instantiated inside their field initialization");

    int currentOrdinal = ordinalsByTypeDescriptor.add(targetTypeDescriptor, 1);

    return new NewInstance(
        newInstance.getQualifier(),
        ASTUtils.addParametersToMethodDescriptor(
            newInstance.getConstructorMethodDescriptor(),
            TypeDescriptors.STRING_TYPE_DESCRIPTOR,
            TypeDescriptors.INT_TYPE_DESCRIPTOR),
        ImmutableList.<Expression>builder()
            .addAll(newInstance.getArguments())
            .add(new StringLiteral("\"" + enumField.getDescriptor().getFieldName() + "\""))
            .add(new NumberLiteral(TypeDescriptors.INT_TYPE_DESCRIPTOR, currentOrdinal))
            .build());
  }

  private void makeEnumConstructionExplicit(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  private void replaceConstructorInvocationCall(Method method, List<Variable> implicitArguments) {

    MethodCall constructorInvocation =
        Preconditions.checkNotNull(ASTUtils.getConstructorInvocation(method));

    MethodDescriptor modifiedConstructorDescriptor =
        ASTUtils.addParametersToMethodDescriptor(
            constructorInvocation.getTarget(),
            Lists.transform(
                implicitArguments,
                new Function<Variable, TypeDescriptor>() {
                  @Override
                  public TypeDescriptor apply(Variable variable) {
                    return variable.getTypeDescriptor();
                  }
                }));

    List<Expression> arguments =
        Lists.transform(
            implicitArguments,
            new Function<Variable, Expression>() {
              @Override
              public Expression apply(Variable variable) {
                return variable.getReference();
              }
            });

    constructorInvocation.setTargetMethodDescriptor(modifiedConstructorDescriptor);
    constructorInvocation.getArguments().addAll(arguments);
  }
}
