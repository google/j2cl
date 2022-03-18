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

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Operator;
import com.google.j2cl.transpiler.ast.UnaryExpression;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.HashSet;
import java.util.Set;

/** Marks variables and parameters as final if they are effectively final. */
public class MakeVariablesFinal extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    Set<Variable> finalVariables = new HashSet<>();
    // Collect variables that are not assigned outside their declaration.
    // Note that it is safe to do one traversal collecting all declarations and removing them when
    // references are seen because variable references can not appear in the AST before their
    // declaration (that invariant is checked in VerifyReferenceScoping).
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterVariable(Variable variable) {
            finalVariables.add(variable);
            return true;
          }

          @Override
          public boolean enterBinaryExpression(BinaryExpression binaryExpression) {
            maybeRecordVariableAssignment(
                binaryExpression.getOperator(), binaryExpression.getLeftOperand());
            return true;
          }

          @Override
          public boolean enterUnaryExpression(UnaryExpression unaryExpression) {
            maybeRecordVariableAssignment(
                unaryExpression.getOperator(), unaryExpression.getOperand());
            return true;
          }

          private void maybeRecordVariableAssignment(Operator operator, Expression operand) {
            if (operator.hasSideEffect() && operand instanceof VariableReference) {
              finalVariables.remove(((VariableReference) operand).getTarget());
            }
          }
        });

    finalVariables.forEach(v -> v.setFinal(true));
  }
}
