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

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/**
 * Rewrites /## @type {Foo<?>} #/ casts (but not declarations) to /## @type {Foo<*>} #/.
 *
 * <p>They are functionally equivalent except that the first trips up JSCompiler's unknown
 * properties checker.
 */
public class NormalizeJsDocCastExpressions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteJsDocCastExpression(JsDocCastExpression jsDocCastExpression) {
            TypeDescriptor typeDescriptor = jsDocCastExpression.getTypeDescriptor();
            if (!(typeDescriptor instanceof DeclaredTypeDescriptor)) {
              return jsDocCastExpression;
            }

            DeclaredTypeDescriptor annotationTypeDescriptor =
                (DeclaredTypeDescriptor) typeDescriptor;

            if (!annotationTypeDescriptor.hasTypeArguments()) {
              return jsDocCastExpression;
            }

            Iterable<TypeDescriptor> typeArgumentTypeDescriptors =
                Lists.transform(
                    annotationTypeDescriptor.getTypeArgumentDescriptors(),
                    typeArgument ->
                        typeArgument.isWildCardOrCapture()
                            ? TypeDescriptors.get().javaLangObject
                            : typeArgument);
            return JsDocCastExpression.Builder.from(jsDocCastExpression)
                .setCastType(
                    DeclaredTypeDescriptor.Builder.from(annotationTypeDescriptor)
                        .setTypeArgumentDescriptors(typeArgumentTypeDescriptors)
                        .build())
                .build();
          }
        });
  }
}
