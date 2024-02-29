/*
 * Copyright 2018 Google Inc.
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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AssertStatement;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;

/** Replaces assert statements with the corresponding method call to the runtime. */
public class ImplementAssertStatements extends NormalizationPass {

  private final boolean useWasmDebugFlag;

  public ImplementAssertStatements() {
    this(false);
  }

  public ImplementAssertStatements(boolean useWasmDebugFlag) {
    this.useWasmDebugFlag = useWasmDebugFlag;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Statement rewriteAssertStatement(AssertStatement assertStatement) {
            SourcePosition sourcePosition = assertStatement.getSourcePosition();

            Statement assertMethodCall =
                implementAssertStatement(assertStatement).makeStatement(sourcePosition);
            if (!useWasmDebugFlag) {
              return assertMethodCall;
            }
            return IfStatement.newBuilder()
                .setConditionExpression(RuntimeMethods.createAreWasmAssertionsEnabledMethodCall())
                .setThenStatement(assertMethodCall)
                .setSourcePosition(sourcePosition)
                .build();
          }
        });
  }

  private static Expression implementAssertStatement(AssertStatement assertStatement) {
    if (assertStatement.getMessage() == null) {
      return RuntimeMethods.createAssertMethodCall(assertStatement.getExpression());
    }
    return RuntimeMethods.createAssertWithMessageMethodCall(
        assertStatement.getExpression(), assertStatement.getMessage());
  }
}
