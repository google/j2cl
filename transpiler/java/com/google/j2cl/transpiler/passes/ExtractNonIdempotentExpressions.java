/*
 * Copyright 2021 Google Inc.
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
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;

/**
 * Extracts non-idempotent expressions into local variables to avoid double evaluation.
 *
 * <p>Wasm does not provide a good alternative solution that could be used at generation time, e.g.
 * a 'dup' instruction which would allow duplicating a value in the stack.
 */
public class ExtractNonIdempotentExpressions extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
            Expression expression = instanceOfExpression.getExpression();

            // instanceof Interface evaluates the expression twice.
            if (!expression.isIdempotent()
                && instanceOfExpression.getTestTypeDescriptor().isInterface()) {
              Variable qualifierVariable =
                  Variable.newBuilder()
                      .setName("$expression")
                      .setFinal(true)
                      .setTypeDescriptor(expression.getTypeDescriptor())
                      .build();
              return MultiExpression.newBuilder()
                  .setExpressions(
                      VariableDeclarationExpression.newBuilder()
                          .addVariableDeclaration(qualifierVariable, expression)
                          .build(),
                      InstanceOfExpression.Builder.from(instanceOfExpression)
                          .setExpression(qualifierVariable.createReference())
                          .build())
                  .build();
            }
            return instanceOfExpression;
          }

          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            Expression qualifier = methodCall.getQualifier();

            if (methodCall.isPolymorphic() && !qualifier.isIdempotent()) {
              Variable qualifierVariable =
                  Variable.newBuilder()
                      .setName("$qualifier")
                      .setFinal(true)
                      .setTypeDescriptor(qualifier.getTypeDescriptor())
                      .build();
              return MultiExpression.newBuilder()
                  .setExpressions(
                      VariableDeclarationExpression.newBuilder()
                          .addVariableDeclaration(qualifierVariable, qualifier)
                          .build(),
                      MethodCall.Builder.from(methodCall)
                          .setQualifier(qualifierVariable.createReference())
                          .build())
                  .build();
            }
            return methodCall;
          }

          @Override
          public Statement rewriteSwitchStatement(SwitchStatement switchStatement) {
            Expression switchExpression = switchStatement.getSwitchExpression();
            if (!switchExpression.isIdempotent()) {
              Variable switchVariable =
                  Variable.newBuilder()
                      .setName("$expression")
                      .setFinal(true)
                      .setTypeDescriptor(switchExpression.getTypeDescriptor())
                      .build();
              return Block.newBuilder()
                  .setStatements(
                      VariableDeclarationExpression.newBuilder()
                          .addVariableDeclaration(switchVariable, switchExpression)
                          .build()
                          .makeStatement(switchStatement.getSourcePosition()),
                      SwitchStatement.Builder.from(switchStatement)
                          .setSwitchExpression(switchVariable.createReference())
                          .build())
                  .build();
            }
            return switchStatement;
          }
        });
  }
}
