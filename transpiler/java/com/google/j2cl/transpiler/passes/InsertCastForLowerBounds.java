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

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject;

import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/**
 * Inserts explicit cast from type variables with lower bound.
 *
 * <p>In Java a lower bounded wildcard implicitly inherits the upper bound from the parameter
 * definition, e.g.
 *
 * <pre><code>{@code
 * interface NumberSupplier<T extends Number> {
 *   T get();
 * }
 *
 * NumberSupplier<? super Integer> ns  = ...
 * Number n = ns.get(); // this is allowed since the parameter in NumberSupplier has an upper bound.
 * }</code></pre>
 *
 * <p>However in the equivalent code in Kotlin this is not allowed, and is assumed that {@code
 * ns.get()} return {@code Any?}.
 */
public class InsertCastForLowerBounds extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                TypeDescriptor typeDescriptor = expression.getTypeDescriptor();
                boolean needsCast =
                    isWildcardOrCaptureWithLowerBound(typeDescriptor)
                        && !isWildcardOrCaptureWithLowerBound(inferredTypeDescriptor)
                        && !isJavaLangObject(inferredTypeDescriptor);
                return needsCast
                    ? CastExpression.newBuilder()
                        .setExpression(expression)
                        .setCastTypeDescriptor(inferredTypeDescriptor)
                        .build()
                    : expression;
              }
            }));
  }

  private static boolean isWildcardOrCaptureWithLowerBound(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      return typeVariable.isWildcardOrCapture()
          && typeVariable.getLowerBoundTypeDescriptor() != null;
    }

    return false;
  }
}
