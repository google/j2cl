/*
 * Copyright 2023 Google Inc.
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
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.ThrowStatement;

/** Replace Java throws to call JS throw API to attach stack traces. */
public class InsertExceptionConversionsWasm extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteThrowStatement(ThrowStatement throwStatement) {
            // Keep the ThrowStatement so we can emit (unreachable) later on. This is not great but
            // looks like the least hacky way at the moment.
            return ThrowStatement.Builder.from(throwStatement)
                .setExpression(
                    RuntimeMethods.createExceptionsMethodCall(
                        "throwJsError", throwStatement.getExpression()))
                .build();
          }
        });
  }
}
