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

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isBoxedType;

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Operator;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.UnaryExpression;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.VariableReference;

/** Removes unnecessary nullable annotations from variable type descriptors. */
public class MakeVariablesNonNull extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    removeNullableFromDeclaredVariables(compilationUnit);
    propagateVariableNullability(compilationUnit);
  }

  private static void removeNullableFromDeclaredVariables(Node node) {
    node.accept(
        new AbstractVisitor() {
          @Override
          public void exitVariableDeclarationFragment(
              VariableDeclarationFragment variableDeclarationFragment) {
            makeNonNullable(variableDeclarationFragment.getVariable());
          }
        });
  }

  private static void propagateVariableNullability(Node node) {
    boolean[] propagateChanges = {false};

    do {
      propagateChanges[0] = false;

      node.accept(
          new AbstractVisitor() {
            @Override
            public void exitVariableDeclarationFragment(
                VariableDeclarationFragment variableDeclarationFragment) {
              Variable variable = variableDeclarationFragment.getVariable();
              Expression initializer = variableDeclarationFragment.getInitializer();
              if (initializer != null) {
                exitAssignment(variable, initializer);
              }
            }

            @Override
            public void exitBinaryExpression(BinaryExpression binaryExpression) {
              exitOperator(
                  binaryExpression.getOperator(),
                  binaryExpression.getLeftOperand(),
                  binaryExpression.getRightOperand());
            }

            @Override
            public void exitUnaryExpression(UnaryExpression unaryExpression) {
              exitOperator(
                  unaryExpression.getOperator(),
                  unaryExpression.getOperand(),
                  unaryExpression.getOperand());
            }

            private void exitOperator(
                Operator operator, Expression operand, Expression expression) {
              if (operator.hasSideEffect() && operand instanceof VariableReference) {
                VariableReference variableReference = (VariableReference) operand;
                Variable variable = variableReference.getTarget();
                exitAssignment(variable, expression);
              }
            }

            private void exitAssignment(Variable variable, Expression expression) {
              TypeDescriptor typeDescriptor = variable.getTypeDescriptor();
              if (!typeDescriptor.isNullable() && expression.canBeNull()) {
                variable.setTypeDescriptor(typeDescriptor.toNullable());
                propagateChanges[0] = true;
              }
            }
          });
    } while (propagateChanges[0]);
  }

  private static void makeNonNullable(Variable variable) {
    TypeDescriptor typeDescriptor = variable.getTypeDescriptor();

    // Variables of boxed types should remain nullable, to avoid boxing/unboxing.
    if (isBoxedType(typeDescriptor)) {
      return;
    }

    variable.setTypeDescriptor(typeDescriptor.toNonNullable());
  }
}
