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

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveLong;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/** Normalizes literals for Kotlin. */
public class NormalizeLiteralsKotlin extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNumberLiteral(NumberLiteral numberLiteral) {
            // Rewrite -9223372036854775808L which is not valid in Kotlin to Long.MIN_VALUE.
            DeclaredTypeDescriptor longType = TypeDescriptors.get().javaLangLong;
            return !isPrimitiveLong(numberLiteral.getTypeDescriptor())
                    || numberLiteral.getValue().longValue() != Long.MIN_VALUE
                ? numberLiteral
                : FieldAccess.newBuilder()
                    .setSourcePosition(SourcePosition.NONE)
                    .setTargetFieldDescriptor(longType.getFieldDescriptor("MIN_VALUE"))
                    .build();
          }
        });
  }
}
