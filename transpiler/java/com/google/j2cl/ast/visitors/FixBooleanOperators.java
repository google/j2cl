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
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.OperatorSideEffectUtils;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/**
 * Corrects the return type of some operators on boolean parameters.
 *
 * <p>
 * In Java &/|/^ operators return a boolean when given boolean input but in JS they return a number.
 *
 * <p>
 * This visitor finds these "bool ^ bool" situations and converts them to "!!(bool ^ bool)" so that
 * the return type is proper.
 */
public class FixBooleanOperators {

  private static final TypeDescriptor primitiveBoolean = TypeDescriptors.get().primitiveBoolean;

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new SplitBadBooleanCompoundAssignmentsVisitor());
    compilationUnit.accept(new FixBadBooleanOperatorsVisitor());
  }

  private static class FixBadBooleanOperatorsVisitor extends AbstractRewriter {
    @Override
    public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
      // Maybe perform this transformation:
      // "bool ^ bool" -> "!!(bool ^ bool)"
      if (binaryExpression.getTypeDescriptor().equalsIgnoreNullability(primitiveBoolean)) {
        if (binaryExpression.getOperator() == BinaryOperator.BIT_AND
            || binaryExpression.getOperator() == BinaryOperator.BIT_OR
            || binaryExpression.getOperator() == BinaryOperator.BIT_XOR) {
          return new PrefixExpression(
              primitiveBoolean,
              new PrefixExpression(
                  primitiveBoolean, new MultiExpression(binaryExpression), PrefixOperator.NOT),
              PrefixOperator.NOT);
        }
      }

      return binaryExpression;
    }
  }

  private static class SplitBadBooleanCompoundAssignmentsVisitor extends AbstractRewriter {
    @Override
    public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
      // Maybe perform this transformation:
      // "bool ^= bool" -> "bool = bool ^ bool"
      if (binaryExpression.getTypeDescriptor().equalsIgnoreNullability(primitiveBoolean)) {
        if (binaryExpression.getOperator() == BinaryOperator.BIT_AND_ASSIGN
            || binaryExpression.getOperator() == BinaryOperator.BIT_OR_ASSIGN
            || binaryExpression.getOperator() == BinaryOperator.BIT_XOR_ASSIGN) {
          return OperatorSideEffectUtils.splitBinaryExpression(binaryExpression);
        }
      }

      return binaryExpression;
    }
  }
}
