/*
 * Copyright 2025 Google Inc.
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
import com.google.j2cl.transpiler.ast.BindingPattern;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.PatternMatchExpression;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchCasePattern;
import com.google.j2cl.transpiler.ast.SwitchConstruct;
import com.google.j2cl.transpiler.ast.SwitchExpression;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableReference;

/**
 * Normalizes switch switch patterns into a simpler form that can be emitted in Kotlin.
 *
 * <p>The normalization of a pattern case:
 *
 * <pre>{@code
 * switch (selector) {
 *   ....
 *   case Pattern when guard ....
 *   ...
 * }
 * }</pre>
 *
 * <p>becomes:
 *
 * <pre>{@code
 * switch (selector) {
 *   ....
 *   case PatternType _ when selector instanceof Pattern && guard ....
 *   ...
 * }
 * }</pre>
 *
 * <p>where {@code PatternType} is the top type in the pattern.
 */
public class NormalizeSwitchPatternsJ2kt extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // 1. Remove the pattern in the switch case to a simple binding pattern and move the actual
    // pattern matching to the guard.
    normalizeSwitchPatterns(compilationUnit);

    // 2. Desugar the newly introduced pattern matching expressions.
    new DesugarInstanceOfPatterns().applyTo(compilationUnit);
  }

  private void normalizeSwitchPatterns(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {

          @Override
          public Statement rewriteSwitchStatement(SwitchStatement switchStatement) {
            return normalizeSwitchPatterns(switchStatement);
          }

          @Override
          public Expression rewriteSwitchExpression(SwitchExpression switchExpression) {
            return normalizeSwitchPatterns(switchExpression);
          }
        });
  }

  private <T extends SwitchConstruct<T>> T normalizeSwitchPatterns(T switchConstruct) {
    if (!switchConstruct.hasPatterns()) {
      return switchConstruct;
    }

    // Extract the evaluation of the selector if necessary to avoid evaluating the selector
    // multiple times.
    var rewrittenSelector = MultiExpression.newBuilder();
    var selectorExpression = switchConstruct.getExpression();
    if (!(selectorExpression instanceof VariableReference)) {
      var selectorVariable =
          Variable.newBuilder()
              .setName("$selector")
              .setTypeDescriptor(selectorExpression.getTypeDescriptor())
              .setFinal(true)
              .build();
      rewrittenSelector.addExpressions(
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclaration(selectorVariable, selectorExpression)
              .build());
      selectorExpression = selectorVariable.createReference();
    }
    rewrittenSelector.addExpressions(selectorExpression);

    var cases = switchConstruct.getCases();
    for (int i = 0; i < cases.size(); i++) {
      if (!(cases.get(i) instanceof SwitchCasePattern switchCase)) {
        continue;
      }
      cases.set(i, convertPatternToGuard(selectorExpression, switchCase));
    }

    return switchConstruct.toBuilder().setExpression(rewrittenSelector.build()).build();
  }

  private static SwitchCase convertPatternToGuard(
      Expression selectorExpression, SwitchCasePattern switchCase) {
    // Rewrite the record pattern into a binding top level pattern and a guard.
    //
    //  case Pattern when guard ->
    //
    // becomes
    //
    //  case PatternType _ when selector instanceof Pattern && guard ->
    //

    // TODO(b/470329542): The desugaring of this simple conversion duplicates the instance of
    // operation which will be fixed in a post pass.

    // selector instanceof Pattern
    var patternMatchExpression =
        PatternMatchExpression.newBuilder()
            .setPattern(switchCase.getPattern())
            .setExpression(selectorExpression.clone())
            .build();

    return switchCase.toBuilder()
        .setPattern(
            // Replace the variable by a dummy unnamed variable that will be ignored
            // in the backend.
            new BindingPattern(
                Variable.newBuilder()
                    .setName("_")
                    .setTypeDescriptor(switchCase.getPattern().getTypeDescriptor())
                    .build()))
        .setGuard(
            // Prepend the pattern matching to the current guard.
            switchCase.getGuard() == null
                ? patternMatchExpression
                : patternMatchExpression.infixAnd(switchCase.getGuard()))
        .build();
  }
}
