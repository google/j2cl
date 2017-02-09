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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.CompoundOperationsUtils;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.Operator;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.UnaryExpression;
import java.util.ArrayList;
import java.util.List;

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
            Expression expression = expressionStatement.getExpression();

            if (expression instanceof PostfixExpression) {
              PostfixExpression postfixExpression = (PostfixExpression) expression;
              if (needsExpansion(postfixExpression.getOperator(), postfixExpression.getOperand())) {
                return toPrefixExpression(postfixExpression).makeStatement();
              }
            }
            return expressionStatement;
          }

          @Override
          public ForStatement rewriteForStatement(ForStatement forStatement) {
            List<Expression> modifiedInitializers =
                replacePrefixExpressionsWithPostfixExpression(forStatement.getInitializers());
            List<Expression> modifiedUpdates =
                replacePrefixExpressionsWithPostfixExpression(forStatement.getUpdates());
            if (!modifiedInitializers.equals(forStatement.getInitializers())
                || !modifiedUpdates.equals(forStatement.getUpdates())) {
              ForStatement.Builder.from(forStatement)
                  .setInitializers(modifiedInitializers)
                  .setUpdates(modifiedUpdates)
                  .build();
            }
            return forStatement;
          }
        });

    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (needsExpansion(binaryExpression.getOperator(), binaryExpression.getLeftOperand())) {
              return CompoundOperationsUtils.expandCompoundExpression(binaryExpression);
            }
            return binaryExpression;
          }

          @Override
          public Expression rewritePrefixExpression(PrefixExpression prefixExpression) {
            if (needsExpansion(prefixExpression.getOperator(), prefixExpression.getOperand())) {
              return CompoundOperationsUtils.expandExpression(prefixExpression);
            }
            return prefixExpression;
          }

          @Override
          public Expression rewritePostfixExpression(PostfixExpression postfixExpression) {
            if (needsExpansion(postfixExpression.getOperator(), postfixExpression.getOperand())) {
              return CompoundOperationsUtils.expandExpression(postfixExpression);
            }
            return postfixExpression;
          }
        });
  }

  private static boolean needsExpansion(Operator operator, Expression targetExpression) {
    if (!operator.hasSideEffect()
        || operator == BinaryOperator.ASSIGN
        || targetExpression instanceof ArrayAccess) {
      return false;
    }

    TypeDescriptor targetTypeDescriptor = targetExpression.getTypeDescriptor();
    if (operator == BinaryOperator.DIVIDE_ASSIGN && needsIntegralCoersion(targetTypeDescriptor)) {
      // Integral division always needs expansion for truncation and DivByZero checks.
      return true;
    }

    // Only a handful of types do not need compound assignment expansion, these are the ones which
    // do not need extra instrumentation to perform the operations due to emulation (longs), boxing
    // or coercion (short, byte and boolean operations).
    return !TypeDescriptors.isJavaLangString(targetTypeDescriptor)
        && !TypeDescriptors.isPrimitiveFloatOrDouble(targetTypeDescriptor)
        && !TypeDescriptors.isPrimitiveInt(targetTypeDescriptor);
  }

  private static boolean needsIntegralCoersion(TypeDescriptor targetTypeDescriptor) {
    return TypeDescriptors.isIntegralPrimitiveType(targetTypeDescriptor)
        || (TypeDescriptors.isBoxedType(targetTypeDescriptor)
            && TypeDescriptors.isIntegralPrimitiveType(
                CompoundOperationsUtils.getCorrespondingPrimitiveType(targetTypeDescriptor)));
  }

  /** Rewrites a postfix expressions int the list into the corresponding prefix expressions. */
  private static List<Expression> replacePrefixExpressionsWithPostfixExpression(
      List<Expression> expressions) {
    List<Expression> result = new ArrayList<>();

    for (Expression expression : expressions) {
      if (expression instanceof PostfixExpression) {
        PostfixExpression postfixExpression = (PostfixExpression) expression;
        if (needsExpansion(postfixExpression.getOperator(), postfixExpression.getOperand())) {
          expression = toPrefixExpression(postfixExpression);
        }
      }
      result.add(expression);
    }
    return result;
  }

  /** Rewrites a postfix expression into the corresponding prefix expression. */
  private static UnaryExpression toPrefixExpression(PostfixExpression postfixExpression) {
    PostfixOperator postfixOperator = postfixExpression.getOperator();
    checkArgument(
        postfixOperator == PostfixOperator.DECREMENT
            || postfixOperator == PostfixOperator.INCREMENT);
    return PrefixExpression.Builder.from(postfixExpression)
        .setOperator(
            postfixOperator == PostfixOperator.DECREMENT
                ? PrefixOperator.DECREMENT
                : PrefixOperator.INCREMENT)
        .build();
  }
}
