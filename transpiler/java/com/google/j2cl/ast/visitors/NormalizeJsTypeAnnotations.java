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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JsTypeAnnotation;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/**
 * Rewrites /## @type {Foo<?>} #/ casts (but not declarations) to /## @type {Foo<*>} #/.
 *
 * <p>They are functionally equivalent except that the first trips up JSCompiler's unknown
 * properties checker.
 */
public class NormalizeJsTypeAnnotations extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Rewriter());
  }

  private static class Rewriter extends AbstractRewriter {
    @Override
    public Node rewriteJsTypeAnnotation(JsTypeAnnotation jsTypeAnnotation) {
      if (jsTypeAnnotation.isDeclaration()
          || jsTypeAnnotation.getTypeDescriptor().getTypeArgumentDescriptors().isEmpty()) {
        return jsTypeAnnotation;
      }

      TypeDescriptor annotationType = jsTypeAnnotation.getTypeDescriptor();
      return JsTypeAnnotation.Builder.from(jsTypeAnnotation)
          .setAnnotationType(
              TypeDescriptors.replaceTypeArgumentDescriptors(
                  annotationType,
                  Lists.transform(
                      annotationType.getTypeArgumentDescriptors(),
                      new Function<TypeDescriptor, TypeDescriptor>() {
                        @Override
                        public TypeDescriptor apply(TypeDescriptor typeArgument) {
                          return typeArgument.isWildCard()
                              ? TypeDescriptors.get().javaLangObject
                              : typeArgument;
                        }
                      })))
          .build();
    }
  }
}
