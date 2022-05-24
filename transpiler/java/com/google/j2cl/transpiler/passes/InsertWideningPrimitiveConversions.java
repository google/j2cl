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
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Inserts a widening operation when a smaller primitive type is being put into a large primitive
 * type slot in assignment, binary numeric promotion, cast and method invocation conversion
 * contexts.
 */
public class InsertWideningPrimitiveConversions extends NormalizationPass {
  private final boolean needFloatOrDoubleWidening;

  public InsertWideningPrimitiveConversions() {
    this(false);
  }

  public InsertWideningPrimitiveConversions(boolean needFloatOrDoubleWidening) {
    this.needFloatOrDoubleWidening = needFloatOrDoubleWidening;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new ConversionContextVisitor(getContextRewriter()));
  }

  private ConversionContextVisitor.ContextRewriter getContextRewriter() {
    return new ConversionContextVisitor.ContextRewriter() {

      @Override
      public Expression rewriteTypeConversionContext(
          TypeDescriptor toTypeDescriptor,
          TypeDescriptor declaredTypeDescriptor,
          Expression expression) {
        if (!shouldWiden(toTypeDescriptor, expression)) {
          return expression;
        }
        return widenTo(toTypeDescriptor, expression);
      }

      @Override
      public Expression rewriteBinaryNumericPromotionContext(
          TypeDescriptor otherOperandTypeDescriptor, Expression operand) {
        if (!TypeDescriptors.isNumericPrimitive(operand.getTypeDescriptor())
            || !TypeDescriptors.isNumericPrimitive(otherOperandTypeDescriptor)) {
          // Widening only applies between primitive types.
          return operand;
        }

        TypeDescriptor widenedTypeDescriptor =
            AstUtils.getNumericBinaryExpressionTypeDescriptor(
                (PrimitiveTypeDescriptor) otherOperandTypeDescriptor,
                (PrimitiveTypeDescriptor) operand.getTypeDescriptor());
        if (!shouldWiden(widenedTypeDescriptor, operand)) {
          return operand;
        }
        return widenTo(widenedTypeDescriptor, operand);
      }

      @Override
      public Expression rewriteCastContext(CastExpression castExpression) {
        if (!shouldWiden(castExpression.getCastTypeDescriptor(), castExpression.getExpression())) {
          return castExpression;
        }
        return widenTo(castExpression.getCastTypeDescriptor(), castExpression.getExpression());
      }
    };
  }

  private static boolean shouldWiden(
      TypeDescriptor toTypeDescriptor, Expression subjectExpression) {
    TypeDescriptor fromTypeDescriptor = subjectExpression.getTypeDescriptor();

    if (!TypeDescriptors.isNumericPrimitive(fromTypeDescriptor)
        || !TypeDescriptors.isNumericPrimitive(toTypeDescriptor)) {
      // Widening only applies between primitive types.
      return false;
    }

    return ((PrimitiveTypeDescriptor) toTypeDescriptor)
        .isWiderThan((PrimitiveTypeDescriptor) fromTypeDescriptor);
  }

  private Expression widenTo(TypeDescriptor toTypeDescriptor, Expression expression) {
    TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();

    // Don't emit known NOOP widenings.
    if (fromTypeDescriptor.isAssignableTo(toTypeDescriptor)
        && !needFloatOrDoubleWidening(toTypeDescriptor)) {
      return expression;
    }

    // Widen literals at compile time.
    if (expression instanceof NumberLiteral) {
      PrimitiveTypeDescriptor literalTypeDescriptor = (PrimitiveTypeDescriptor) toTypeDescriptor;
      return new NumberLiteral(literalTypeDescriptor, ((NumberLiteral) expression).getValue());
    }

    return RuntimeMethods.createPrimitivesWideningMethodCall(
        expression, (PrimitiveTypeDescriptor) toTypeDescriptor);
  }

  private boolean needFloatOrDoubleWidening(TypeDescriptor toTypeDescriptor) {
    return needFloatOrDoubleWidening && TypeDescriptors.isPrimitiveFloatOrDouble(toTypeDescriptor);
  }
}
