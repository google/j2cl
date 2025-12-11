/*
 * Copyright 2024 Google Inc.
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
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.YieldStatement;

/**
 * Implements {@code yield} statements that return values from IIFEs resulting from enclosing {@code
 * EmbeddedStatements}.
 *
 * <p>These are the result of translating switch constructs, which n JavaScript are only allowed as
 * statements, to embedded statements. This pass is the final step which converts the {@code yield}
 * statement into the {@code return} for the IIFE.
 */
public class ImplementYieldStatement extends NormalizationPass {

  // For constructs where we need to have statements embedded in expression, the statements are
  // enclosed by `EmbeddedStatement` and the resulting value is returned using `yield`. And since
  // in JS `EmbeddedStatements` are emitted as IIFE the `yield` statement just needs to be converted
  // to `return`. This translation is used, for example, for switch constructs where expressions
  // like:
  //
  //    int x = switch(x) {
  //        case 1, 3 -> 3;
  //        case 4 -> { int j = 3; break j; }
  //        case 6 -> throw new Exception();
  //        default -> f();
  //      };
  //
  // get desugared here to:
  //
  //    int x = ((JsSupplier) (() -> switch(x) {
  //        case 1:
  //        case 3:
  //          return 3;
  //        case 4:
  //          { int j = 3; return j; }
  //        case 6 :
  //          throw new Exception();
  //        default:
  //          return f();
  //      })).get();
  //
  // taking advantage the functions in JavaScript can modify the values that are captured.

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {

          @Override
          public ReturnStatement rewriteYieldStatement(YieldStatement yieldStatement) {
            return ReturnStatement.newBuilder()
                .setExpression(yieldStatement.getExpression())
                .setSourcePosition(yieldStatement.getSourcePosition())
                .build();
          }
        });
  }
}
