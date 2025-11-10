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
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableReference;

/** Normalizes instanceof patterns out. */
public class DesugarInstanceOfPatterns extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
            if (instanceOfExpression.getPatternVariable() == null) {
              return instanceOfExpression;
            }

            Expression expression = instanceOfExpression.getExpression();
            Variable patternVariable = instanceOfExpression.getPatternVariable();
            TypeDescriptor testTypeDescriptor = instanceOfExpression.getTestTypeDescriptor();

            // The instanceof has a pattern and will be represented as a multi expression as
            // follows:
            //   ( ExpType exp = expression,               // avoid double evaluation of expression
            //     PatternVarType v =
            //         exp instanceof T ? (T) exp : null, // Cast exp and assign to pattern variable
            //     v != null)                             // return the result of instanceof.

            var variableDeclarationBuilder = VariableDeclarationExpression.newBuilder();

            // Always use a variable for the expression that needs to be evaluated twice to
            // prevent increasing code size and make the code more readable.
            Variable expressionVariable;
            if (expression instanceof VariableReference variableReference) {
              // If it is already a variable just use it.
              expressionVariable = variableReference.getTarget();
            } else {
              // Create a new variable to avoid evaluating expression twice.
              expressionVariable =
                  Variable.newBuilder()
                      .setName("exp")
                      .setFinal(true)
                      .setTypeDescriptor(expression.getTypeDescriptor())
                      .build();
              variableDeclarationBuilder.addVariableDeclaration(expressionVariable, expression);
            }
            // Declare and initialize the pattern variable.
            //
            //     PatternVarType patternVariable =
            //         exp instanceof T ? (T) exp : null
            variableDeclarationBuilder.addVariableDeclaration(
                patternVariable,
                ConditionalExpression.newBuilder()
                    .setTypeDescriptor(patternVariable.getTypeDescriptor())
                    .setConditionExpression(
                        InstanceOfExpression.Builder.from(instanceOfExpression)
                            .setExpression(expressionVariable.createReference())
                            .setPatternVariable(null)
                            .build())
                    .setTrueExpression(
                        CastExpression.newBuilder()
                            .setExpression(expressionVariable.createReference())
                            .setCastTypeDescriptor(testTypeDescriptor)
                            .build())
                    .setFalseExpression(testTypeDescriptor.getNullValue())
                    .build());

            return MultiExpression.newBuilder()
                .addExpressions(
                    variableDeclarationBuilder.build(),
                    // patternVariable != null is the actual value of this multiexpression.
                    patternVariable.createReference().infixNotEqualsNull())
                .build();
          }
        });
  }
}
