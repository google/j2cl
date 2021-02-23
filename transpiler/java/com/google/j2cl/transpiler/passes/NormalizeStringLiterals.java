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
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/** Replaces String literals with String construction. */
public class NormalizeStringLiterals extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteStringLiteral(StringLiteral stringLiteral) {
            ArrayTypeDescriptor charArrayDescriptor =
                ArrayTypeDescriptor.newBuilder()
                    .setComponentTypeDescriptor(PrimitiveTypes.CHAR)
                    .build();
            MethodDescriptor stringCreator =
                TypeDescriptors.get()
                    .javaLangString
                    .getMethodDescriptor("fromInternalArray", charArrayDescriptor);
            return MethodCall.Builder.from(stringCreator)
                .setArguments(new ArrayLiteral(charArrayDescriptor, stringLiteral.toCharLiterals()))
                .build();
          }
        });
  }
}
