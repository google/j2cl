/*
 * Copyright 2021 Google Inc.
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

import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/**
 * Inserts explicit array coercion casts.
 *
 * <p>Array types are covariant in Java and invariant in Kotlin from the perspective of type
 * checking however, at runtime their assignability follows Java semantics, allowing assignment of
 * Java compatible array types if they are cast.
 */
public class InsertExplicitArrayCoercionCasts extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor actualTypeDescriptor,
                  Expression expression) {
                return needsCast(inferredTypeDescriptor, expression.getTypeDescriptor())
                    ? CastExpression.newBuilder()
                        .setExpression(expression)
                        .setCastTypeDescriptor(inferredTypeDescriptor)
                        .build()
                    : expression;
              }

              @Override
              public Expression rewriteMethodInvocationContext(
                  ParameterDescriptor inferredParameterDescriptor,
                  ParameterDescriptor actualParameterDescriptor,
                  Expression argument) {
                // Don't rewrite vararg array literals.
                return actualParameterDescriptor.isVarargs() && argument instanceof ArrayLiteral
                    ? argument
                    : super.rewriteMethodInvocationContext(
                        inferredParameterDescriptor, actualParameterDescriptor, argument);
              }
            }));
  }

  private static boolean needsCast(TypeDescriptor expected, TypeDescriptor actual) {
    return expected.isArray()
        && actual.isArray()
        && !expected.isPrimitiveArray()
        && !expected.toNonNullable().equals(actual.toNonNullable());
  }
}
