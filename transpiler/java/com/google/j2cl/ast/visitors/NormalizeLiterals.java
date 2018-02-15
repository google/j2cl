/*
 * Copyright 2018 Google Inc.
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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CharacterLiteral;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.RuntimeMethods;
import com.google.j2cl.ast.TypeDescriptors;

/** Replaces literals that are required to be emulated. */
public class NormalizeLiterals extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteCharacterLiteral(CharacterLiteral characterLiteral) {
            return NumberLiteral.of(characterLiteral.getValue())
                .withComment(characterLiteral.getEscapedValue());
          }

          @Override
          public Expression rewriteNumberLiteral(NumberLiteral numberLiteral) {
            if (!TypeDescriptors.isPrimitiveLong(numberLiteral.getTypeDescriptor())) {
              return numberLiteral;
            }

            long longValue = numberLiteral.getValue().longValue();

            long lowOrderBits = longValue << 32 >> 32;
            long highOrderBits = longValue >> 32;
            return RuntimeMethods.createNativeLongMethodCall(
                    "fromBits",
                    NumberLiteral.of((int) lowOrderBits),
                    NumberLiteral.of((int) highOrderBits))
                .withComment(String.valueOf(longValue));
          }
        });
  }
}
