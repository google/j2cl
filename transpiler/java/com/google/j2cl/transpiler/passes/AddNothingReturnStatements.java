/*
 * Copyright 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Transforms expression statements that have the Kotlin Nothing type to be return statements.
 *
 * <p>This pass relies on several constraints:
 *
 * <ul>
 *   <li>{@link TypeDescriptors#kotlinNothing} must be non-null, else the pass makes no changes.
 *   <li>It's expected that all Nothing expressions have already been transformed to be statements.
 *   <li>Statements after the Nothing expression should have already been cleaned up.
 * </ul>
 */
public final class AddNothingReturnStatements extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    // If we don't know about the Kotlin nothing type then there's nothing to be done.
    if (TypeDescriptors.get().kotlinNothing == null) {
      return;
    }
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteExpressionStatement(ExpressionStatement expressionStatement) {
            if (!TypeDescriptors.isKotlinNothing(
                expressionStatement.getExpression().getTypeDescriptor())) {
              return expressionStatement;
            }

            Method currentMethod = (Method) getCurrentMember();

            // If we're in a void method and it's the last statement we can skip adding the return.
            if (TypeDescriptors.isPrimitiveVoid(
                    currentMethod.getDescriptor().getReturnTypeDescriptor())
                && Iterables.getLast(currentMethod.getBody().getStatements())
                    == expressionStatement) {
              return expressionStatement;
            }

            return ReturnStatement.newBuilder()
                .setExpression(expressionStatement.getExpression())
                .setSourcePosition(expressionStatement.getSourcePosition())
                .build();
          }
        });
  }
}
