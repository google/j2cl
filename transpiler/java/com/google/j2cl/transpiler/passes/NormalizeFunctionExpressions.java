/*
 * Copyright 2019 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import java.util.ArrayList;
import java.util.List;

/**
 * Normalizes FunctionExpression to provide type safety when called through the generic functional
 * interface, to provide the runtime type checks that normally bridges perform.
 *
 * <p>Since there is no way to create bridges for (specialized) lambdas, they are normalized to have
 * the declared type of the functional interface instead, and explicit casts are inserted to satisfy
 * their specialized type.
 */
public class NormalizeFunctionExpressions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public FunctionExpression rewriteFunctionExpression(
              FunctionExpression functionExpression) {
            return rewriteParameters(functionExpression);
          }
        });
  }

  private FunctionExpression rewriteParameters(FunctionExpression functionExpression) {
    MethodDescriptor declaredMethodDescriptor =
        functionExpression.getDescriptor().getDeclarationDescriptor();

    List<Statement> prologue = new ArrayList<>();
    List<Variable> newParameters = new ArrayList<>(functionExpression.getParameters());

    for (int i = 0; i < declaredMethodDescriptor.getParameterDescriptors().size(); i++) {
      MethodDescriptor.ParameterDescriptor declaredParameterDescriptor =
          declaredMethodDescriptor.getParameterDescriptors().get(i);
      Variable parameter = functionExpression.getParameters().get(i);

      TypeDescriptor declaredParameterTypeDescriptor =
          declaredParameterDescriptor.getTypeDescriptor();
      // Only replace parameters that have been specialized.
      if (!declaredParameterTypeDescriptor.isAssignableTo(parameter.getTypeDescriptor())
          // Don't replace the varargs parameter since FunctionalExpressions are jsfunctions, and
          // hence if they have a varargs parameter it is a jsvarargs which will be stamped to the
          // right array type due to varargs JavaScript semantics.
          && !declaredParameterDescriptor.isVarargs()) {
        // Create a new parameter variable of the type "? extends DeclaredType", this takes
        // care of having the right parameter type without the problem of introducing type
        // variables that are not in the current context. (this is a hack).
        TypeVariable targetType =
            TypeVariable.createWildcardWithUpperBound(declaredParameterTypeDescriptor);

        Variable newParameter =
            Variable.Builder.from(parameter).setTypeDescriptor(targetType).build();

        // InferredType oldPar = (InferredType) newPar;
        Statement castToOldParameter =
            VariableDeclarationExpression.newBuilder()
                .addVariableDeclaration(
                    parameter,
                    CastExpression.newBuilder()
                        .setCastTypeDescriptor(parameter.getTypeDescriptor().toRawTypeDescriptor())
                        .setExpression(newParameter.createReference())
                        .build())
                .build()
                .makeStatement(functionExpression.getSourcePosition());

        newParameters.set(i, newParameter);
        prologue.add(castToOldParameter);
      }
    }
    return FunctionExpression.Builder.from(functionExpression)
        .setParameters(newParameters)
        .setStatements(
            ImmutableList.<Statement>builder()
                .addAll(prologue)
                .addAll(functionExpression.getBody().getStatements())
                .build())
        .build();
  }
}
