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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.StringLiteralGettersCreator;

/** Implements lazy initialization of String literals. */
public class ImplementStringCompileTimeConstants extends LibraryNormalizationPass {

  private final StringLiteralGettersCreator stringLiteralGettersCreator =
      new StringLiteralGettersCreator();

  @Override
  public void applyTo(Library library) {
    rewriteStringLiterals(library);
  }

  /**
   * Creates a method for each unique string literal in the program, and replaces each of them by a
   * call to the created getter.
   */
  private void rewriteStringLiterals(Library library) {
    library.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessMethod(Method method) {
            // Avoid processing the string literals that are moved to the getter.
            return method.getDescriptor().getOrigin()
                != MethodOrigin.SYNTHETIC_STRING_LITERAL_GETTER;
          }

          @Override
          public Expression rewriteStringLiteral(StringLiteral stringLiteral) {
            return MethodCall.Builder.from(
                    stringLiteralGettersCreator.getOrCreateLiteralMethod(
                        getCurrentType(), stringLiteral, /* synthesizeMethod= */ true))
                .build();
          }
        });
  }
}
