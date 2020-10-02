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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.EmptyStatement;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.Statement;

/**
 * Removes trivial statements that have no effect.
 *
 * <p>Some of the normalization passes might leave statements that are empty or just a literal, this
 * pass performs the cleanup.
 */
public class RemoveNoopStatements extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Statement rewriteEmptyStatement(EmptyStatement emptyStatement) {
            // Remove empty statements.
            return null;
          }

          @Override
          public Statement rewriteExpressionStatement(ExpressionStatement expressionStatement) {
            if (!expressionStatement.getExpression().hasSideEffects()) {
              // Remove statements that are just an expression that does not have side effects.
              // This removes some statements that are added by other normalization passes when
              // rewriting expressions.
              return null;
            }
            return expressionStatement;
          }
        });
  }
}
