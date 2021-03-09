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

import com.google.common.collect.Sets;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.Set;

/**
 * Inserts a narrowing operation when a wider primitive type is being put into a narrower primitive
 * type slot in assignment and cast conversion contexts.
 */
public class InsertNarrowingPrimitiveConversions extends NormalizationPass {
  private final boolean treatFloatAsDouble;

  public InsertNarrowingPrimitiveConversions() {
    this(true);
  }

  public InsertNarrowingPrimitiveConversions(boolean treatFloatAsDouble) {
    this.treatFloatAsDouble = treatFloatAsDouble;
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
        TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();

        if (isAssignable(fromTypeDescriptor, toTypeDescriptor)
            || !shouldNarrow(fromTypeDescriptor, toTypeDescriptor)) {
          return expression;
        }

        return insertNarrowingCall(expression, toTypeDescriptor);
      }

      @Override
      public Expression rewriteCastContext(CastExpression castExpression) {
        Expression expression = castExpression.getExpression();
        TypeDescriptor toTypeDescriptor = castExpression.getCastTypeDescriptor();
        TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();

        if (toTypeDescriptor.isPrimitive()
            && fromTypeDescriptor.isPrimitive()
            && isAssignable(fromTypeDescriptor, toTypeDescriptor)) {
          return expression;
        }

        if (!shouldNarrow(fromTypeDescriptor, toTypeDescriptor)) {
          return castExpression;
        }

        return insertNarrowingCall(expression, toTypeDescriptor);
      }

      private boolean shouldNarrow(
          TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor) {

        if (fromTypeDescriptor.isSameBaseType(toTypeDescriptor)) {
          return false;
        }

        if (!fromTypeDescriptor.isPrimitive() || !toTypeDescriptor.isPrimitive()) {
          // Non-primitive casts are not narrowing.
          return false;
        }

        Set<TypeDescriptor> typeDescriptors = Sets.newHashSet(fromTypeDescriptor, toTypeDescriptor);

        if (!((PrimitiveTypeDescriptor) fromTypeDescriptor)
                .isWiderThan((PrimitiveTypeDescriptor) toTypeDescriptor)
            && !(typeDescriptors.contains(PrimitiveTypes.SHORT)
                && typeDescriptors.contains(PrimitiveTypes.CHAR))) {
          // Don't modify non-narrowing casts, except for the special case between
          // short and char.
          return false;
        }

        return true;
      }

      private Expression insertNarrowingCall(
          Expression expression, TypeDescriptor toTypeDescriptor) {

        // Narrow literals at compile time.
        if (expression instanceof NumberLiteral) {
          PrimitiveTypeDescriptor literalTypeDescriptor =
              (PrimitiveTypeDescriptor) toTypeDescriptor;
          return new NumberLiteral(literalTypeDescriptor, ((NumberLiteral) expression).getValue());
        }

        return RuntimeMethods.createPrimitivesNarrowingMethodCall(
            expression, (PrimitiveTypeDescriptor) toTypeDescriptor);
      }
    };
  }

  private boolean isAssignable(TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor) {
    return fromTypeDescriptor.isAssignableTo(toTypeDescriptor)
        && !needsDoubleToFloatNarrowing(fromTypeDescriptor, toTypeDescriptor);
  }

  private boolean needsDoubleToFloatNarrowing(
      TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor) {
    return !treatFloatAsDouble
        && TypeDescriptors.isPrimitiveDouble(fromTypeDescriptor)
        && TypeDescriptors.isPrimitiveFloat(toTypeDescriptor);
  }
}
