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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/**
 * Inserts a narrowing operation when a non-boxed reference type is being put into a primitive
 * type slot in cast conversion contexts.
 */
public class InsertNarrowingReferenceConversions extends ConversionContextVisitor {

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new InsertNarrowingReferenceConversions());
  }

  public InsertNarrowingReferenceConversions() {
    super(
        new ContextRewriter() {
          @Override
          public Expression rewriteCastContext(CastExpression castExpression) {
            TypeDescriptor toTypeDescriptor = castExpression.getCastTypeDescriptor();
            TypeDescriptor fromTypeDescriptor = castExpression.getExpression().getTypeDescriptor();
            // Casting from any reference type other than the eight boxed types to primitive types
            // does a narrowing reference conversion first, and then does a unboxing conversion.
            if (toTypeDescriptor.isPrimitive()
                && TypeDescriptors.isNonBoxedReferenceType(fromTypeDescriptor)) {
              TypeDescriptor boxedTypeDescriptor =
                  TypeDescriptors.getBoxTypeFromPrimitiveType(toTypeDescriptor);
              // (int) new Object(); => (int) (Integer) new Object();
              return CastExpression.create(
                  CastExpression.create(castExpression.getExpression(), boxedTypeDescriptor),
                  toTypeDescriptor);
            }
            // In other casting context, narrowing reference conversion should have been explicitly
            // applied.
            return castExpression;
          }
        });
  }
}
