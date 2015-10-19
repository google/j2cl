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

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Visibility;

import java.util.Arrays;

/**
 * Inserts underflow/overflow checking conversion.
 *
 * <p>This is not a conversion in the strict JLS sense of the word but underflow/overflow checking
 * insertion benefits from conversion context visitor system and so makes use of it.
 *
 * <p>A completely correct implementation would simply wrap the result of every arithmetic binary
 * expression but doing so would have heavy readability, output size and performance costs.
 *
 * <p>This implementation takes a more GWT approach by checking only at the last boundary between an
 * assignment (there are many flavors of assignment) and a binary expression and by omitting the
 * checking on very common and safe int operations.
 *
 * <p>Must be run after InsertUnboxingConversionVisitor so that the available inspectable operations
 * are in primitive form and after InsertNarrowingPrimitiveConversionVisitor since it will pre-fix
 * some situations and some this pass some work. Must be run before InsertBoxingConversionVisitor
 * since that pass will re-box some things and then some operations will no longer be in a primitive
 * form that is legal input to the overflow correcting method calls.
 */
public class InsertUnderflowOverflowConversionVisitor extends ConversionContextVisitor {
  public static void applyTo(CompilationUnit compilationUnit) {
    new InsertUnderflowOverflowConversionVisitor().run(compilationUnit);
  }

  public InsertUnderflowOverflowConversionVisitor() {
    super(
        new ContextRewriter() {
          @Override
          public Expression rewriteBinaryNumericPromotionContext(
              Expression subjectOperandExpression, Expression otherOperandExpression) {
            return maybeCheckOverflow(
                subjectOperandExpression,
                subjectOperandExpression.getTypeDescriptor(),
                otherOperandExpression.getTypeDescriptor());
          }

          @Override
          public Expression rewriteMethodInvocationContext(
              TypeDescriptor parameterTypeDescriptor, Expression argumentExpression) {
            return maybeCheckOverflow(
                argumentExpression,
                argumentExpression.getTypeDescriptor(),
                parameterTypeDescriptor);
          }

          @Override
          public Expression rewriteAssignmentContext(
              TypeDescriptor toTypeDescriptor, Expression expression) {
            return maybeCheckOverflow(expression, expression.getTypeDescriptor(), toTypeDescriptor);
          }

          @Override
          public Expression rewriteUnaryNumericPromotionContext(Expression operandExpression) {
            return maybeCheckOverflow(
                operandExpression,
                operandExpression.getTypeDescriptor(),
                operandExpression.getTypeDescriptor());
          }
        });
  }

  private static Expression maybeCheckOverflow(
      Expression expression, TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor) {
    // Only examine same type assignments since overflow can only occur there (other cases
    // are already covered by Narrowing Primitive Conversion).
    if (fromTypeDescriptor != toTypeDescriptor) {
      return expression;
    }
    // Only examine primitive assignments.
    if (!TypeDescriptors.isNumericPrimitive(fromTypeDescriptor)) {
      return expression;
    }
    // Binary expressions are the place where a numeric value changes and may start exceeding an
    // upper or lower bound, so if the expression we're looking at is not a binary expression then
    // there's no need to insert under/overflow checking.
    if (!(expression instanceof BinaryExpression)) {
      return expression;
    }

    BinaryExpression binaryExpression = (BinaryExpression) expression;

    // SPECIAL CASE FOR INT
    if (fromTypeDescriptor == TypeDescriptors.get().primitiveInt
        && binaryExpression.getOperator() != BinaryOperator.DIVIDE) {
      // Technically we should be emitting underflow/overflow narrowing operations for
      // int regardless of which binary operator is being used. But if we did so our int
      // operations would be spammed over with underflow/overflow checks. Instead, for int,
      // we do what GWT does which is to only emit the check when doing division.
      return expression;
    }

    if (fromTypeDescriptor == TypeDescriptors.get().primitiveLong) {
      // The long emulation library already handles over and underflow internally.
      return expression;
    }

    if (fromTypeDescriptor == TypeDescriptors.get().primitiveFloat
        || fromTypeDescriptor == TypeDescriptors.get().primitiveDouble) {
      // The float and double numeric operations in JS already get the benefit of
      // native over and underflow.
      return expression;
    }

    // The result of all these checks is that we emit checks for all byte<->byte, char<->char, and
    // short<->short binary expressions and also for division with int<->int.

    String overflowMethodName =
        String.format("$to%s", AstUtils.toProperCase(toTypeDescriptor.getSimpleName()));
    MethodDescriptor overflowMethodDescriptor =
        MethodDescriptor.createRaw(
            true,
            Visibility.PUBLIC,
            TypeDescriptors.VM_PRIMITIVES_TYPE_DESCRIPTOR,
            overflowMethodName,
            Lists.newArrayList(fromTypeDescriptor),
            toTypeDescriptor);
    // Primitives.$toA(expr);
    return new MethodCall(null, overflowMethodDescriptor, Arrays.asList(expression));
  }
}
