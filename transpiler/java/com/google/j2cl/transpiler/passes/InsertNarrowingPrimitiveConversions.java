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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

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
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteCastExpression(CastExpression castExpression) {
            Expression expression = castExpression.getExpression();
            TypeDescriptor toTypeDescriptor = castExpression.getCastTypeDescriptor();
            TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();

            if (toTypeDescriptor.isPrimitive() && toTypeDescriptor.equals(fromTypeDescriptor)) {
              // Remove unnecessary primitive casts.
              return expression;
            }

            if (shouldNarrow(fromTypeDescriptor, toTypeDescriptor)) {
              if (treatFloatAsDouble && TypeDescriptors.isPrimitiveFloat(toTypeDescriptor)) {
                // The only possible narrowing call involving double and float is to narrow double
                // to float, which we ignore here if requested.
                return expression;
              }
              return insertNarrowingCall(expression, toTypeDescriptor);
            }

            return castExpression;
          }
        });
  }

  private static boolean shouldNarrow(
      TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor) {

    if (!TypeDescriptors.isNumericPrimitive(toTypeDescriptor)) {
      // Non-primitive casts are not narrowing.
      return false;
    }

    return ((PrimitiveTypeDescriptor) fromTypeDescriptor)
            .isWiderThan((PrimitiveTypeDescriptor) toTypeDescriptor)
        // Explicitly handle char to short and short to char that are also considered
        // narrowing per JLS 5.1.3 and need conversion.
        // Note that at this point to and from are different types.
        || (isCharOrShort(fromTypeDescriptor) && isCharOrShort(toTypeDescriptor));
  }

  private static boolean isCharOrShort(TypeDescriptor typeDescriptor) {
    return TypeDescriptors.isPrimitiveChar(typeDescriptor)
        || TypeDescriptors.isPrimitiveShort(typeDescriptor);
  }

  private static Expression insertNarrowingCall(
      Expression expression, TypeDescriptor toTypeDescriptor) {

    // Narrow literals at compile time.
    if (expression instanceof NumberLiteral) {
      PrimitiveTypeDescriptor literalTypeDescriptor = (PrimitiveTypeDescriptor) toTypeDescriptor;
      return new NumberLiteral(literalTypeDescriptor, ((NumberLiteral) expression).getValue());
    }

    return RuntimeMethods.createPrimitivesNarrowingMethodCall(
        expression, (PrimitiveTypeDescriptor) toTypeDescriptor);
  }
}
