/*
 * Copyright 2024 Google Inc.
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

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveInt;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveLong;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;

/**
 * Rewrites integral MIN_VALUE literals to preserve their semantics.
 *
 * <p>In Kotlin there is no way to directly express Long.MIN_VALUE and there are contradicting
 * semantics for Int.MIN_VALUE in the different versions of the Kotlin compiler. To avoid this,
 * integral MIN_VALUE literals are rewritten using an equivalent arithmetic expression that is
 * guaranteed to have the expected semantics across the different Kotlin versions.
 */
public class NormalizeMinValueIntegralLiterals extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNumberLiteral(NumberLiteral numberLiteral) {
            // Long.MIN_VALUE can not be represented as a literal in Kotlin.
            if (isPrimitiveLong(numberLiteral.getTypeDescriptor())
                && numberLiteral.getValue().longValue() == Long.MIN_VALUE) {
              return NumberLiteral.fromLong(Long.MIN_VALUE + 1)
                  .infixMinus(NumberLiteral.fromLong(1L));
            }

            // In some contexts, Int.MIN_VALUE is inferred to Long in K2.
            // See: b/330785447, b/331668649.
            if (isPrimitiveInt(numberLiteral.getTypeDescriptor())
                && numberLiteral.getValue().intValue() == Integer.MIN_VALUE) {
              return NumberLiteral.fromInt(Integer.MIN_VALUE + 1)
                  .infixMinus(NumberLiteral.fromInt(1));
            }

            return numberLiteral;
          }
        });
  }
}
