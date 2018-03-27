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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeVariable;

/**
 * Rewrites casts to {@code @type {Foo<?>}} where {@code ? extends Bar} as casts to {@code @type
 * {Foo<Bar>}} to avoid "unknown type" errors.
 */
public class NormalizeJsDocCastExpressions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteJsDocCastExpression(JsDocCastExpression jsDocCastExpression) {
            if (!(jsDocCastExpression.getTypeDescriptor() instanceof DeclaredTypeDescriptor)) {
              return jsDocCastExpression;
            }

            DeclaredTypeDescriptor castTypeDescriptor =
                (DeclaredTypeDescriptor) jsDocCastExpression.getTypeDescriptor();

            if (!castTypeDescriptor.hasTypeArguments()) {
              return jsDocCastExpression;
            }

            return JsDocCastExpression.Builder.from(jsDocCastExpression)
                .setCastType(
                    DeclaredTypeDescriptor.Builder.from(castTypeDescriptor)
                        .setTypeArgumentDescriptors(
                            castTypeDescriptor
                                .getTypeArgumentDescriptors()
                                .stream()
                                .map(NormalizeJsDocCastExpressions::replaceWildcardWithBound)
                                .collect(ImmutableList.toImmutableList()))
                        .build())
                .build();
          }
        });
  }

  private static TypeDescriptor replaceWildcardWithBound(TypeDescriptor typeDescriptor) {
    if (!(typeDescriptor instanceof TypeVariable)) {
      return typeDescriptor;
    }
    TypeVariable typeVariable = (TypeVariable) typeDescriptor;
    return typeVariable.isWildcardOrCapture() ? typeDescriptor.toRawTypeDescriptor() : typeVariable;
  }
}
