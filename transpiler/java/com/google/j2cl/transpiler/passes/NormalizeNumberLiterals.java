/*
 * Copyright 2023 Google Inc.
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

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveByte;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveShort;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;

/**
 * Normalizes number literals by converting {@code byte} and {@code short} literals to {@code int}
 * with a cast.
 */
public class NormalizeNumberLiterals extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNumberLiteral(NumberLiteral numberLiteral) {
            PrimitiveTypeDescriptor literalTypeDescriptor = numberLiteral.getTypeDescriptor();
            return isPrimitiveByte(literalTypeDescriptor) || isPrimitiveShort(literalTypeDescriptor)
                ? CastExpression.newBuilder()
                    .setExpression(NumberLiteral.fromInt(numberLiteral.getValue().intValue()))
                    .setCastTypeDescriptor(literalTypeDescriptor)
                    .build()
                : numberLiteral;
          }
        });
  }
}
