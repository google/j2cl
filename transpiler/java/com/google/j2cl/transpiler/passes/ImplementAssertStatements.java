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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AssertStatement;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;

/** Replaces assert statements with the corresponding method call to the runtime. */
public class ImplementAssertStatements extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Statement rewriteAssertStatement(AssertStatement assertStatement) {
            if (assertStatement.getMessage() == null) {
              return RuntimeMethods.createAssertsMethodCall(
                      "$assert", ImmutableList.of(assertStatement.getExpression()))
                  .makeStatement(assertStatement.getSourcePosition());
            }
            return RuntimeMethods.createAssertsMethodCall(
                    "$assertWithMessage",
                    ImmutableList.of(assertStatement.getExpression(), assertStatement.getMessage()))
                .makeStatement(assertStatement.getSourcePosition());
          }
        });
  }
}
