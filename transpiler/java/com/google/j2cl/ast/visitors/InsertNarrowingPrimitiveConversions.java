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
import com.google.j2cl.ast.CharacterLiteral;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

import java.util.Set;

/**
 * Inserts a narrowing operation when a wider primitive type is being put into a narrower primitive
 * type slot in assignment and cast conversion contexts.
 */
public class InsertNarrowingPrimitiveConversions extends ConversionContextVisitor {

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new InsertNarrowingPrimitiveConversions());
  }

  public InsertNarrowingPrimitiveConversions() {
    super(
        new ContextRewriter() {
          @Override
          public Expression rewriteAssignmentContext(
              TypeDescriptor toTypeDescriptor, Expression expression) {
            TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();

            if (AstUtils.canRemoveCast(fromTypeDescriptor, toTypeDescriptor)
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
                && AstUtils.canRemoveCast(fromTypeDescriptor, toTypeDescriptor)) {
              return expression;
            }

            if (!shouldNarrow(fromTypeDescriptor, toTypeDescriptor)) {
              return castExpression;
            }

            return insertNarrowingCall(expression, toTypeDescriptor);
          }

          private boolean shouldNarrow(
              TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor) {

            if (fromTypeDescriptor.equalsIgnoreNullability(toTypeDescriptor)) {
              return false;
            }

            if (!fromTypeDescriptor.isPrimitive() || !toTypeDescriptor.isPrimitive()) {
              // Non-primitive casts are not narrowing.
              return false;
            }

            int fromWidth = TypeDescriptors.getWidth(fromTypeDescriptor);
            int toWidth = TypeDescriptors.getWidth(toTypeDescriptor);

            Set<TypeDescriptor> typeDescriptors =
                Sets.newHashSet(fromTypeDescriptor, toTypeDescriptor);

            if (fromWidth <= toWidth
                && !(typeDescriptors.contains(TypeDescriptors.get().primitiveShort)
                    && typeDescriptors.contains(TypeDescriptors.get().primitiveChar))) {
              // Don't modify non-narrowing casts, except for the special case between
              // short and char.
              return false;
            }

            return true;
          }

          private Expression convertLiteral(Object literalValue, TypeDescriptor toTypeDescriptor) {
            if (literalValue instanceof Number) {
              Number numberLiteral = (Number) literalValue;
              // Narrow at compile time.
              if (toTypeDescriptor == TypeDescriptors.get().primitiveByte) {
                return new NumberLiteral(toTypeDescriptor, numberLiteral.byteValue());
              } else if (toTypeDescriptor == TypeDescriptors.get().primitiveChar) {
                return new CharacterLiteral((char) numberLiteral.intValue());
              } else if (toTypeDescriptor == TypeDescriptors.get().primitiveShort) {
                return new NumberLiteral(toTypeDescriptor, numberLiteral.shortValue());
              } else if (toTypeDescriptor == TypeDescriptors.get().primitiveInt) {
                return new NumberLiteral(toTypeDescriptor, numberLiteral.intValue());
              } else if (toTypeDescriptor == TypeDescriptors.get().primitiveLong) {
                return new NumberLiteral(toTypeDescriptor, numberLiteral.longValue());
              } else if (toTypeDescriptor == TypeDescriptors.get().primitiveFloat) {
                return new NumberLiteral(toTypeDescriptor, numberLiteral.floatValue());
              } else if (toTypeDescriptor == TypeDescriptors.get().primitiveFloat) {
                return new NumberLiteral(toTypeDescriptor, numberLiteral.doubleValue());
              }
            } else if (literalValue instanceof Character) {
              Character characterLiteral = (Character) literalValue;
              return convertLiteral((int) characterLiteral.charValue(), toTypeDescriptor);
            }
            throw new IllegalArgumentException();
          }

          private Expression insertNarrowingCall(
              Expression expression, TypeDescriptor toTypeDescriptor) {

            // Narrow literals at compile time.
            if (expression instanceof NumberLiteral) {
              return convertLiteral(((NumberLiteral) expression).getValue(), toTypeDescriptor);
            } else if (expression instanceof CharacterLiteral) {
              return convertLiteral(((CharacterLiteral) expression).getValue(), toTypeDescriptor);
            }

            TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();
            String narrowMethodName =
                String.format(
                    "$narrow%sTo%s",
                    AstUtils.toProperCase(fromTypeDescriptor.getSimpleName()),
                    AstUtils.toProperCase(toTypeDescriptor.getSimpleName()));
            MethodDescriptor narrowMethodDescriptor =
                MethodDescriptor.Builder.fromDefault()
                    .jsInfo(JsInfo.RAW)
                    .isStatic(true)
                    .enclosingClassTypeDescriptor(BootstrapType.PRIMITIVES.getDescriptor())
                    .methodName(narrowMethodName)
                    .parameterTypeDescriptors(Lists.newArrayList(fromTypeDescriptor))
                    .returnTypeDescriptor(toTypeDescriptor)
                    .build();
            // Primitives.$narrowAToB(expr);
            return MethodCall.createMethodCall(null, narrowMethodDescriptor, expression);
          }
        });
  }
}
