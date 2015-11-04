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
import com.google.common.collect.Sets;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Visibility;

import java.util.Arrays;
import java.util.Set;

/**
 * Inserts a narrowing operation when a wider primitive type is being put into a narrower primitive
 * type slot in assignment and cast conversion contexts.
 */
public class InsertNarrowingPrimitiveConversionVisitor extends ConversionContextVisitor {

  public static void applyTo(CompilationUnit compilationUnit) {
    new InsertNarrowingPrimitiveConversionVisitor().run(compilationUnit);
  }

  public InsertNarrowingPrimitiveConversionVisitor() {
    super(
        new ContextRewriter() {
          @Override
          public Expression rewriteAssignmentContext(
              TypeDescriptor toTypeDescriptor, Expression expression) {
            // Under most circumstances an implicit narrowing is a compile error in assignment. In
            // the rare circumstances where it is legal the JLS says it must be from a constant
            // expression into a variable where the constant value fits with no conversion. So
            // there is no work for us.
            return expression;
          }

          @Override
          public Expression rewriteCastContext(CastExpression castExpression) {
            Expression expression = castExpression.getExpression();
            TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();
            TypeDescriptor toTypeDescriptor = castExpression.getCastTypeDescriptor();

            // Don't modify non-primitive casts.
            if (!fromTypeDescriptor.isPrimitive() || !toTypeDescriptor.isPrimitive()) {
              return castExpression;
            }
            // Remove completely redundant primitive casts.
            if (fromTypeDescriptor == toTypeDescriptor) {
              return expression;
            }

            Set<TypeDescriptor> typeDescriptors =
                Sets.newHashSet(fromTypeDescriptor, toTypeDescriptor);

            int fromWidth = TypeDescriptors.getWidth(fromTypeDescriptor);
            int toWidth = TypeDescriptors.getWidth(toTypeDescriptor);

            // Don't modify non-narrowing casts, except for the special case between short and char.
            if (fromWidth <= toWidth
                && !(typeDescriptors.contains(TypeDescriptors.get().primitiveShort)
                    && typeDescriptors.contains(TypeDescriptors.get().primitiveChar))) {
              return castExpression;
            }
            // Don't emit known NOOP narrowings.
            if (AstUtils.canRemoveCast(fromTypeDescriptor, toTypeDescriptor)) {
              return expression;
            }

            String narrowMethodName =
                String.format(
                    "$narrow%sTo%s",
                    AstUtils.toProperCase(fromTypeDescriptor.getSimpleName()),
                    AstUtils.toProperCase(toTypeDescriptor.getSimpleName()));
            MethodDescriptor narrowMethodDescriptor =
                MethodDescriptor.createRaw(
                    true,
                    Visibility.PUBLIC,
                    BootstrapType.PRIMITIVES.getDescriptor(),
                    narrowMethodName,
                    Lists.newArrayList(fromTypeDescriptor),
                    toTypeDescriptor,
                    null,
                    null);
            // Primitives.$narrowAToB(expr);
            return MethodCall.createRegularMethodCall(
                null, narrowMethodDescriptor, Arrays.asList(expression));
          }
        });
  }
}
