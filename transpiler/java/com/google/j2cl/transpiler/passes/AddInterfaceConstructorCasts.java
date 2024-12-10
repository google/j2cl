/*
 * Copyright 2024 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/** Adds jsdoc casts to workaround JsCompiler handling of interface constructor (b/79389970). */
public final class AddInterfaceConstructorCasts extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteJavaScriptConstructorReference(
              JavaScriptConstructorReference constructorReference) {
            if (!constructorReference.getReferencedTypeDeclaration().isInterface()) {
              return constructorReference;
            }

            return JsDocCastExpression.newBuilder()
                .setCastTypeDescriptor(TypeDescriptors.get().nativeFunction)
                .setExpression(constructorReference)
                .build();
          }
        });
  }
}
