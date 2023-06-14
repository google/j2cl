/*
 * Copyright 2022 Google Inc.
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

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;

/** Normalize vararg parameters for Kotlin from {@code Array<T>} to {@code Array<out T>}. */
public class NormalizeVarargParametersJ2kt extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethod(Method method) {
            normalizeVarargParameters(method);
          }

          @Override
          public void exitFunctionExpression(FunctionExpression functionExpression) {
            normalizeVarargParameters(functionExpression);
          }

          private void normalizeVarargParameters(MethodLike methodLike) {
            if (!methodLike.getDescriptor().isVarargs()) {
              return;
            }

            Variable varargParameter = Iterables.getLast(methodLike.getParameters());
            ArrayTypeDescriptor varargTypeDescriptor =
                (ArrayTypeDescriptor) varargParameter.getTypeDescriptor();
            if (varargTypeDescriptor.getComponentTypeDescriptor().isPrimitive()) {
              return;
            }

            varargParameter.setTypeDescriptor(
                ArrayTypeDescriptor.Builder.from(varargTypeDescriptor)
                    .setComponentTypeDescriptor(
                        TypeVariable.createWildcardWithUpperBound(
                            varargTypeDescriptor.getComponentTypeDescriptor()))
                    .build());
          }
        });
  }
}
