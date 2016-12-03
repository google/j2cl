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
public class InsertNarrowingPrimitiveConversions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new ConversionContextVisitor(getContextRewriter()));
  }

  private ConversionContextVisitor.ContextRewriter getContextRewriter() {
    return new ConversionContextVisitor.ContextRewriter() {
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

        if (fromTypeDescriptor.hasSameRawType(toTypeDescriptor)) {
          return false;
        }

        if (!fromTypeDescriptor.isPrimitive() || !toTypeDescriptor.isPrimitive()) {
          // Non-primitive casts are not narrowing.
          return false;
        }

        int fromWidth = TypeDescriptors.getWidth(fromTypeDescriptor);
        int toWidth = TypeDescriptors.getWidth(toTypeDescriptor);

        Set<TypeDescriptor> typeDescriptors = Sets.newHashSet(fromTypeDescriptor, toTypeDescriptor);

        if (fromWidth <= toWidth
            && !(typeDescriptors.contains(TypeDescriptors.get().primitiveShort)
                && typeDescriptors.contains(TypeDescriptors.get().primitiveChar))) {
          // Don't modify non-narrowing casts, except for the special case between
          // short and char.
          return false;
        }

        return true;
      }

      @SuppressWarnings("ReferenceEquality")
      private Expression convertLiteral(Object literalValue, TypeDescriptor toTypeDescriptor) {
        if (literalValue instanceof Number) {
          Number numberLiteral = (Number) literalValue;
          // Narrow at compile time.
          if (TypeDescriptors.isPrimitiveByte(toTypeDescriptor)) {
            return new NumberLiteral(toTypeDescriptor, numberLiteral.byteValue());
          } else if (TypeDescriptors.isPrimitiveChar(toTypeDescriptor)) {
            return new CharacterLiteral((char) numberLiteral.intValue());
          } else if (TypeDescriptors.isPrimitiveShort(toTypeDescriptor)) {
            return new NumberLiteral(toTypeDescriptor, numberLiteral.shortValue());
          } else if (TypeDescriptors.isPrimitiveInt(toTypeDescriptor)) {
            return new NumberLiteral(toTypeDescriptor, numberLiteral.intValue());
          } else if (TypeDescriptors.isPrimitiveLong(toTypeDescriptor)) {
            return new NumberLiteral(toTypeDescriptor, numberLiteral.longValue());
          } else if (TypeDescriptors.isPrimitiveFloat(toTypeDescriptor)) {
            return new NumberLiteral(toTypeDescriptor, numberLiteral.floatValue());
          } else if (TypeDescriptors.isPrimitiveDouble(toTypeDescriptor)) {
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
                AstUtils.toProperCase(fromTypeDescriptor.getSimpleSourceName()),
                AstUtils.toProperCase(toTypeDescriptor.getSimpleSourceName()));
        MethodDescriptor narrowMethodDescriptor =
            MethodDescriptor.newBuilder()
                .setJsInfo(JsInfo.RAW)
                .setStatic(true)
                .setEnclosingClassTypeDescriptor(BootstrapType.PRIMITIVES.getDescriptor())
                .setName(narrowMethodName)
                .setParameterTypeDescriptors(Lists.newArrayList(fromTypeDescriptor))
                .setReturnTypeDescriptor(toTypeDescriptor)
                .build();
        // Primitives.$narrowAToB(expr);
        return MethodCall.Builder.from(narrowMethodDescriptor).setArguments(expression).build();
      }
    };
  }
}
