/*
 * Copyright 2022 Google Inc.
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

import com.google.common.collect.Iterables;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Make variable scoping to be consistent with JavaScript and J2CL invariants.
 *
 * <p>Note: different source languages have different scoping rules, for example in Kotlin the
 * condition of the DoWhileStatement is in the scope of its body.
 */
public class RestoreVariableScoping extends NormalizationPass {
  private final Map<Variable, Block> targetScopeByVariableToHoist = new LinkedHashMap<>();

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    collectVariablesReferredOutOfScope(compilationUnit);
    replaceDeclarationsByAssignments(compilationUnit);
    hoistOutOfScoperVariableDeclarationsToEnclosingBlock();
  }

  private void collectVariablesReferredOutOfScope(CompilationUnit compilationUnit) {
    // Find variables that are out of scope.
    compilationUnit.accept(
        new ScopeAwareVisitor<Variable>() {

          @Override
          public boolean enterVariable(Variable variable) {
            addElementsToScope(variable);
            return true;
          }

          @Override
          public void exitVariableReference(VariableReference variableReference) {
            Variable variable = variableReference.getTarget();
            if (!isElementInScope(variable)) {
              targetScopeByVariableToHoist.put(
                  variable, (Block) getParent(Block.class::isInstance));
            }
          }
        });
  }

  private void replaceDeclarationsByAssignments(CompilationUnit compilationUnit) {
    // Rewrite declarations into assignments.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteVariableDeclarationExpression(
              VariableDeclarationExpression variableDeclarationExpression) {
            if (variableDeclarationExpression.getFragments().size() != 1) {
              // Out of scope variables are a product of Kotlin scoping rules and in Kotlin there
              // is only one variable in a declaration. If this were to change our current
              // verifiers will catch the error.
              return variableDeclarationExpression;
            }
            VariableDeclarationFragment variableDeclarationFragment =
                Iterables.getOnlyElement(variableDeclarationExpression.getFragments());
            Variable variable = variableDeclarationFragment.getVariable();

            if (!targetScopeByVariableToHoist.containsKey(variable)) {
              return variableDeclarationExpression;
            }

            Expression initializer = variableDeclarationFragment.getInitializer();
            if (initializer == null) {
              // The variable was declared without an initializer; since we are moving the
              // declaration up, we need to leave an expression in its place.  We leave a null
              // literal for simplicity.
              // Since variable declaration expressions can only be at the top of an
              // ExpressionStatement, this will be just a statement with just a null literal which
              // is effectively a noop that is removed by a cleanup pass later.
              return TypeDescriptors.get().javaLangObject.getNullValue();
            }

            // Replace declaration by a plain assignment, and mark the variable as non-final since
            // its declaration will be hoisted up and the original variable declaration will be
            // turned into a plain assignment.
            variable.setFinal(false);
            return BinaryExpression.Builder.asAssignmentTo(variable)
                .setRightOperand(initializer)
                .build();
          }
        });
  }

  private void hoistOutOfScoperVariableDeclarationsToEnclosingBlock() {
    // Move the declarations to the enclosing block.
    targetScopeByVariableToHoist.forEach(
        (variable, targetBlock) ->
            targetBlock
                .getStatements()
                .add(
                    0,
                    VariableDeclarationExpression.newBuilder()
                        .addVariableDeclarations(variable)
                        .build()
                        .makeStatement(SourcePosition.NONE)));
  }
}
