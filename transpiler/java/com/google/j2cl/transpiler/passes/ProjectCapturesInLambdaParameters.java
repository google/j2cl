/*
 * Copyright 2025 Google Inc.
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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.Node;

/** Projects captures in method descriptors and variables. */
public class ProjectCapturesInLambdaParameters extends AbstractJ2ktNormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFunctionExpression(FunctionExpression functionExpression) {
            DeclaredTypeDescriptor declaredTypeDescriptor =
                functionExpression.getTypeDescriptor().getFunctionalInterface();
            functionExpression =
                FunctionExpression.Builder.from(functionExpression)
                    .setTypeDescriptor(
                        declaredTypeDescriptor.withTypeArguments(
                            declaredTypeDescriptor.getTypeArgumentDescriptors().stream()
                                .map(AbstractJ2ktNormalizationPass::projectCaptures)
                                .collect(toImmutableList())))
                    .build();
            functionExpression
                .getParameters()
                .forEach(it -> it.setTypeDescriptor(projectCaptures(it.getTypeDescriptor())));
            return functionExpression;
          }
        });
  }
}
