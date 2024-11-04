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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;

/**
 * Normalize all local variables to have an explicit initializer.
 *
 * <p>It is necessary until this is fixed: https://youtrack.jetbrains.com/issue/KT-54319
 */
public class NormalizeVariableInitialization extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public VariableDeclarationFragment rewriteVariableDeclarationFragment(
              VariableDeclarationFragment variableDeclaration) {
            Variable variable = variableDeclaration.getVariable();
            if (variableDeclaration.getInitializer() != null) {
              return variableDeclaration;
            }

            // Make the variable non-final to allow for more than one assignment.
            variable.setFinal(false);

            return VariableDeclarationFragment.Builder.from(variableDeclaration)
                .setInitializer(variable.getTypeDescriptor().getDefaultValue())
                .build();
          }
        });
  }
}
