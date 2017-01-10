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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DoWhileStatement;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.WhileStatement;

/** Makes sure that body of conditional are always blocks (except in the else if case). */
public class ControlStatementFormatter extends NormalizationPass {
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

            thenStatement =
                thenStatement instanceof Block ? thenStatement : new Block(thenStatement);
            elseStatement =
                elseStatement == null
                        || elseStatement instanceof Block
                        || elseStatement instanceof IfStatement
                    ? elseStatement
                    : new Block(elseStatement);
            return new IfStatement(
                ifStatement.getConditionExpression(), thenStatement, elseStatement);
          }

          @Override
          public ForStatement rewriteForStatement(ForStatement forStatement) {
            Statement body = forStatement.getBody();
            if (body instanceof Block) {
              return forStatement;
            }

            return ForStatement.newBuilder()
                .setConditionExpression(forStatement.getConditionExpression())
                .setBody(body)
                .setInitializers(forStatement.getInitializers())
                .setUpdates(forStatement.getUpdates())
                .build();
          }

          @Override
          public DoWhileStatement rewriteDoWhileStatement(DoWhileStatement doWhileStatement) {
            Statement body = doWhileStatement.getBody();
            if (body instanceof Block) {
              return doWhileStatement;
            }

            return new DoWhileStatement(doWhileStatement.getConditionExpression(), new Block(body));
          }

          @Override
          public WhileStatement rewriteWhileStatement(WhileStatement whileStatement) {
            Statement body = whileStatement.getBody();
            if (body instanceof Block) {
              return whileStatement;
            }

            return new WhileStatement(whileStatement.getConditionExpression(), new Block(body));
          }
        });
  }

}
