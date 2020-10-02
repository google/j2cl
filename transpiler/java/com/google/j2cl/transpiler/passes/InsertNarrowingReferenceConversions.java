/*
 * Copyright 2016 Google Inc.
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

import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Inserts a narrowing operation when a non-boxed reference type is being put into a primitive type
 * slot in cast conversion contexts.
 */
public class InsertNarrowingReferenceConversions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new ConversionContextVisitor(getContextRewriter()));
  }

  private ConversionContextVisitor.ContextRewriter getContextRewriter() {
    return new ConversionContextVisitor.ContextRewriter() {
      @Override
      public Expression rewriteCastContext(CastExpression castExpression) {
        TypeDescriptor toTypeDescriptor = castExpression.getCastTypeDescriptor();
        TypeDescriptor fromTypeDescriptor = castExpression.getExpression().getTypeDescriptor();
        // Casting from any reference type other than the eight boxed types to primitive types
        // does a narrowing reference conversion first, and then does a unboxing conversion.
        if (toTypeDescriptor.isPrimitive()
            && TypeDescriptors.isNonBoxedReferenceType(fromTypeDescriptor)) {
          // This is to make sure that code like
          //
          //   Object o = new Long(10);
          //   int i = (int) o;
          //
          // throws a ClassCastException trying to cast Long to Integer instead of silently
          // assigning 10 to i by calling o.intValue.
          //
          // In this case
          //
          //   int i = (int) o;
          //
          // will become
          //
          //   int i = (int) (Integer) o;
          //
          return CastExpression.newBuilder()
              .setExpression(
                  CastExpression.newBuilder()
                      .setExpression(castExpression.getExpression())
                      .setCastTypeDescriptor(toTypeDescriptor.toBoxedType())
                      .build())
              .setCastTypeDescriptor(toTypeDescriptor)
              .build();
        }
        // In other casting context, narrowing reference conversion should have been
        // explicitly applied.
        return castExpression;
      }
    };
  }
}
