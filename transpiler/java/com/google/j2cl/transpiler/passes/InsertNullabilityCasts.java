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

import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/** Inserts casts in places where necessary due to nullability differences. */
public final class InsertNullabilityCasts extends NormalizationPass {
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
                return needsCast(expression.getTypeDescriptor(), inferredTypeDescriptor)
                    ? CastExpression.newBuilder()
                        .setExpression(expression)
                        .setCastTypeDescriptor(inferredTypeDescriptor)
                        .build()
                    : expression;
              }
            }));
  }

  private static boolean needsCast(TypeDescriptor from, TypeDescriptor to) {
    // Cast is needed:
    // - from nullable type to non-null type
    // - if nullability differs for any of type arguments
    return (from.isNullable() && !to.isNullable()) || needsCastForTypeArguments(from, to);
  }

  private static boolean needsCastForTypeArguments(TypeDescriptor from, TypeDescriptor to) {
    return (from instanceof DeclaredTypeDescriptor)
        && (to instanceof DeclaredTypeDescriptor)
        && Streams.zip(
                ((DeclaredTypeDescriptor) from).getTypeArgumentDescriptors().stream(),
                ((DeclaredTypeDescriptor) to).getTypeArgumentDescriptors().stream(),
                InsertNullabilityCasts::typeArgumentNeedsCast)
            .anyMatch(needsCast -> needsCast);
  }

  private static boolean typeArgumentNeedsCast(TypeDescriptor from, TypeDescriptor to) {
    return from.isNullable() != to.isNullable() || needsCastForTypeArguments(from, to);
  }
}
