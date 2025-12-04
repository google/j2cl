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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BindingPattern;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Pattern;
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
            Pattern pattern = instanceOfExpression.getPattern();
            if (pattern == null) {
              return instanceOfExpression;
            }

            return desugarPattern(
                instanceOfExpression.getSourcePosition(),
                instanceOfExpression.getExpression(),
                pattern);
          }
        });
  }

  private static Expression desugarPattern(
      SourcePosition sourcePosition, Expression expression, Pattern pattern) {
    return switch (pattern) {
      case BindingPattern bindingPattern ->
          desugarBindingPattern(sourcePosition, expression, bindingPattern);
    };
  }

  private static Expression desugarBindingPattern(
      SourcePosition sourcePosition, Expression expression, BindingPattern pattern) {
    Variable patternVariable = pattern.getVariable();

    // The instanceof has a pattern and will be represented as a multi expression as
    // follows:
    //    (ExpType exp = expression,               // avoid double evaluation of expression
    //       exp instanceof T &&                   // perform the instance of operation and
    //       (patternVariable = (T) exp, true),    // cast exp and assign to pattern variable
    //    )

    var resultBuilder = MultiExpression.newBuilder();
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
      resultBuilder.addExpressions(
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclaration(expressionVariable, expression)
              .build());
    }

    //  exp instanceof T && (T patternVariable = (T) exp, true)
    resultBuilder.addExpressions(
        InstanceOfExpression.newBuilder()
            .setExpression(expressionVariable.createReference())
            .setTestTypeDescriptor(patternVariable.getTypeDescriptor())
            .setPattern(null)
            .setSourcePosition(sourcePosition)
            .build()
            .infixAnd(
                assignPatternVariableReturningTrue(
                    patternVariable,
                    JsDocCastExpression.newBuilder()
                        .setExpression(expressionVariable.createReference())
                        .setCastTypeDescriptor(patternVariable.getTypeDescriptor())
                        .build())));

    return resultBuilder.build();
  }

  /** Declares and assigns the pattern variable in an expression that is true. */
  private static Expression assignPatternVariableReturningTrue(
      Variable patternVariable, Expression expression) {
    // (Type var = (Type) expression, true)
    return MultiExpression.newBuilder()
        .addExpressions(
            VariableDeclarationExpression.newBuilder()
                .addVariableDeclaration(patternVariable, expression)
                .build(),
            BooleanLiteral.get(true))
        .build();
  }
}
