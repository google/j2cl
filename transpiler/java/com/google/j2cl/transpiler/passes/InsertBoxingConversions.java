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


import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;

/**
 * Inserts a boxing operation when a primitive type is being put into a reference type slot in
 * assignment or method invocation conversion contexts.
 */
public class InsertBoxingConversions extends NormalizationPass {
  private final boolean areBooleanAndDoubleBoxed;

  public InsertBoxingConversions(boolean areBooleanAndDoubleBoxed) {
    this.areBooleanAndDoubleBoxed = areBooleanAndDoubleBoxed;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new ConversionContextVisitor(getContextRewriter()));
  }

  private ConversionContextVisitor.ContextRewriter getContextRewriter() {
    return new ConversionContextVisitor.ContextRewriter() {
      @Override
      public Expression rewriteTypeConversionContext(
          TypeDescriptor inferredTypeDescriptor,
          TypeDescriptor declaredTypeDescriptor,
          Expression expression) {
        // A narrowing primitive conversion may precede boxing a number or character literal.
        // (See JLS 5.2).
        if (expression instanceof NumberLiteral) {
          expression = maybeNarrowNumberLiteral(inferredTypeDescriptor, (NumberLiteral) expression);
        }
        // There should be a following 'widening reference conversion' if the targeting type
        // is not the boxed type, but as widening reference conversion is always NOOP, and it
        // is mostly impossible to be optimized by JSCompiler, just avoid the insertion of the
        // NOOP cast here.
        return maybeBox(inferredTypeDescriptor, expression);
      }

      @Override
      public Expression rewriteCastContext(CastExpression castExpression) {
        TypeDescriptor toTypeDescriptor = castExpression.getCastTypeDescriptor();
        TypeDescriptor fromTypeDescriptor = castExpression.getExpression().getTypeDescriptor();
        if (needsBoxing(toTypeDescriptor, fromTypeDescriptor)) {
          // Actually remove the cast and replace it with the boxing.
          Expression boxedExpression = box(castExpression.getExpression());
          // It's possible that casting a primitive type to a non-boxed reference type.
          // e.g. (Object) i; in this case, just keep the NOOP casting after boxing.
          if (!boxedExpression.getTypeDescriptor().isAssignableTo(toTypeDescriptor)) {
            return CastExpression.newBuilder()
                .setExpression(boxedExpression)
                .setCastTypeDescriptor(toTypeDescriptor)
                .build();
          }
          return boxedExpression;
        }
        return castExpression;
      }

      @Override
      public Expression rewriteMethodInvocationContext(
          ParameterDescriptor inferredParameterDescriptor,
          ParameterDescriptor declaredParameterDescriptor,
          Expression argument) {
        if (inferredParameterDescriptor.isDoNotAutobox()) {
          return argument;
        }
        return maybeBox(inferredParameterDescriptor.getTypeDescriptor(), argument);
      }
    };
  }

  private Expression maybeBox(TypeDescriptor toTypeDescriptor, Expression expression) {
    if (needsBoxing(toTypeDescriptor, expression.getTypeDescriptor())) {
      Expression boxedExpression = box(expression);
      // The expression is of some type T that is guaranteed to be a boxed type that, for example,
      // could be assigned back to a variable of type T; therefore the result type should be
      // preserved.
      boolean insertCast =
          toTypeDescriptor instanceof TypeVariable
              && !((TypeVariable) toTypeDescriptor).isWildcardOrCapture();
      return insertCast
          ? CastExpression.newBuilder()
              .setExpression(boxedExpression)
              .setCastTypeDescriptor(toTypeDescriptor)
              .build()
          : boxedExpression;
    }
    return expression;
  }

  private boolean needsBoxing(TypeDescriptor toTypeDescriptor, TypeDescriptor fromTypeDescriptor) {
    return !TypeDescriptors.isNonVoidPrimitiveType(toTypeDescriptor)
        && TypeDescriptors.isNonVoidPrimitiveType(fromTypeDescriptor)
        && (areBooleanAndDoubleBoxed
            || !TypeDescriptors.isPrimitiveBooleanOrDouble(fromTypeDescriptor));
  }

  private static Expression maybeNarrowNumberLiteral(
      TypeDescriptor toTypeDescriptor, NumberLiteral numberLiteral) {

    if (!TypeDescriptors.isBoxedOrPrimitiveType(toTypeDescriptor)) {
      return numberLiteral;
    }
    return new NumberLiteral(toTypeDescriptor.toUnboxedType(), numberLiteral.getValue());
  }

  /**
   * Boxes {@code expression} using the valueOf() method of the corresponding boxed type. e.g.
   * expression => Integer.valueOf(expression).
   */
  private Expression box(Expression expression) {
    PrimitiveTypeDescriptor primitiveType =
        (PrimitiveTypeDescriptor) expression.getTypeDescriptor();

    MethodDescriptor valueOfMethodDescriptor =
        primitiveType
            .toBoxedType()
            .getMethodDescriptor(MethodDescriptor.VALUE_OF_METHOD_NAME, primitiveType);
    return MethodCall.Builder.from(valueOfMethodDescriptor).setArguments(expression).build();
  }
}
