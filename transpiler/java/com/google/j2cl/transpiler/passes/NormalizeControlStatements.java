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
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.LoopStatement;
import com.google.j2cl.transpiler.ast.Statement;

/** Makes sure that body of conditional are always blocks (except in the else if case). */
public class NormalizeControlStatements extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public IfStatement rewriteIfStatement(IfStatement ifStatement) {
            Statement thenStatement = ifStatement.getThenStatement();
            Statement elseStatement = ifStatement.getElseStatement();
            if (thenStatement instanceof Block
                && (elseStatement == null
                    || elseStatement instanceof Block
                    || elseStatement instanceof IfStatement)) {
              return ifStatement;
            }

            thenStatement = thenStatement.ensureBlock();
            elseStatement =
                elseStatement == null || elseStatement instanceof IfStatement
                    ? elseStatement
                    : elseStatement.ensureBlock();

            return IfStatement.builder()
                .setSourcePosition(ifStatement.getSourcePosition())
                .setConditionExpression(ifStatement.getConditionExpression())
                .setThenStatement(thenStatement)
                .setElseStatement(elseStatement)
                .build();
          }

          @Override
          public LoopStatement rewriteLoopStatement(LoopStatement loopStatement) {
            Statement body = loopStatement.getBody();
            if (body instanceof Block) {
              return loopStatement;
            }

            return loopStatement.toBuilder().setBody(body.ensureBlock()).build();
          }
        });
  }

}
