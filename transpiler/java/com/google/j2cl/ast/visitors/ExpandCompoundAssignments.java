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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.CompoundOperationsUtils;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrimitiveTypes;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.UnaryExpression;

/** Expands compound assignments where conversions need to be performed. */
public class ExpandCompoundAssignments extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        // Transform postfix operations into prefix operations if they need expansion and their
        // result value is not needed.
        new AbstractRewriter() {
          @Override
          public ExpressionStatement rewriteExpressionStatement(
              ExpressionStatement expressionStatement) {
            return normalizePostfixExpression(expressionStatement.getExpression())
                .makeStatement(expressionStatement.getSourcePosition());
          }

          @Override
          public ForStatement rewriteForStatement(ForStatement forStatement) {

            return ForStatement.Builder.from(forStatement)
                .setInitializers(
                    forStatement.getInitializers().stream()
                        .map(ExpandCompoundAssignments::normalizePostfixExpression)
                        .collect(ImmutableList.toImmutableList()))
                .setUpdates(
                    forStatement.getUpdates().stream()
                        .map(ExpandCompoundAssignments::normalizePostfixExpression)
                        .collect(ImmutableList.toImmutableList()))
                .build();
          }
        });

    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (needsExpansion(binaryExpression)) {
              return CompoundOperationsUtils.expandCompoundExpression(binaryExpression);
            }
            return binaryExpression;
          }

          @Override
          public Expression rewritePrefixExpression(PrefixExpression prefixExpression) {
            if (needsExpansion(prefixExpression)) {
              return CompoundOperationsUtils.expandExpression(prefixExpression);
            }
            return prefixExpression;
          }

          @Override
          public Expression rewritePostfixExpression(PostfixExpression postfixExpression) {
            if (needsExpansion(postfixExpression)) {
              return CompoundOperationsUtils.expandExpression(postfixExpression);
            }
            return postfixExpression;
          }
        });
  }

  private static boolean needsExpansion(BinaryExpression expression) {
    TypeDescriptor lhsTypeDescriptor = expression.getLeftOperand().getTypeDescriptor();
    TypeDescriptor rhsTypeDescriptor = expression.getRightOperand().getTypeDescriptor();
    BinaryOperator operator = expression.getOperator();

    if (!operator.isCompoundAssignment()) {
      return false;
    }

    // For floating point, native arithmetic is good enough and doesn't need instrumentation.
    if (TypeDescriptors.isPrimitiveFloatOrDouble(lhsTypeDescriptor)) {
      return false;
    }

    // For int, native arithmetic is mostly good but we need instrumentation in a few cases:
    // - Division operations (/ and %) requires 32-bit coercion and divide-by-zero check
    // - Addition, subtraction and multiplication require 32-bit coercion
    // - Right-shift requires 32-bit coercion
    // - Right-hand-size w/ a wider type requires 32-bit coercion
    if (TypeDescriptors.isPrimitiveInt(lhsTypeDescriptor)
        && operator != BinaryOperator.DIVIDE_ASSIGN
        && operator != BinaryOperator.REMAINDER_ASSIGN
        && operator != BinaryOperator.RIGHT_SHIFT_UNSIGNED_ASSIGN
        && operator != BinaryOperator.PLUS_ASSIGN
        && operator != BinaryOperator.MINUS_ASSIGN
        && operator != BinaryOperator.TIMES_ASSIGN
        && !rhsTypeDescriptor.toUnboxedType().isWiderThan(PrimitiveTypes.INT)) {
      return false;
    }

    return true;
  }

  private static boolean needsExpansion(UnaryExpression expression) {
    TypeDescriptor targetTypeDescriptor = expression.getOperand().getTypeDescriptor();

    if (!expression.getOperator().hasSideEffect()) {
      return false;
    }

    // For floating point, native arithmetic is good enough for unary operations.
    if (TypeDescriptors.isPrimitiveFloatOrDouble(targetTypeDescriptor)) {
      return false;
    }

    return true;
  }

  /** Normalizes expandable postfix expressions into the corresponding prefix expressions. */
  private static Expression normalizePostfixExpression(Expression expression) {
    if (expression instanceof PostfixExpression) {
      PostfixExpression postfixExpression = (PostfixExpression) expression;
      if (needsExpansion(postfixExpression)) {
        // Only normalize the ones that are expanded.
        return PrefixExpression.Builder.from(postfixExpression)
            .setOperator(postfixExpression.getOperator().toPrefixOperator())
            .build();
      }
    }
    return expression;
  }
}
