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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.Node;

/** Normalizes array creation for Kotlin, by extracting array literal from new array expression. */
public class NormalizeArrayCreationsJ2kt extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNewArray(NewArray newArray) {
            Expression initializer = newArray.getInitializer();
            checkState(initializer == null || initializer instanceof ArrayLiteral);
            return initializer != null ? initializer : newArray;
          }

          @Override
          public Node rewriteMethod(Method method) {
            Expression defaultValue = method.getDefaultValue();
            boolean isArray = method.getDescriptor().getReturnTypeDescriptor().isArray();
            if (defaultValue != null && isArray) {
              // Normalize single-element scalar shorthand defaults (e.g. default 42 -> default
              // {42}) and align ArrayLiteral type descriptors with the non-nullable method return
              // type.
              boolean isSingleElementShorthand = !(defaultValue instanceof ArrayLiteral);
              ArrayTypeDescriptor arrayType =
                  (ArrayTypeDescriptor) method.getDescriptor().getReturnTypeDescriptor();
              return method.toBuilder()
                  .setDefaultValue(
                      ArrayLiteral.builder()
                          .setTypeDescriptor(arrayType)
                          .setValueExpressions(
                              isSingleElementShorthand
                                  ? ImmutableList.of(defaultValue)
                                  : ((ArrayLiteral) defaultValue).getValueExpressions())
                          .build())
                  .build();
            }
            return method;
          }
        });
  }
}
