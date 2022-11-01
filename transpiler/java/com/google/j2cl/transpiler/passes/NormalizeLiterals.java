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
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.common.StringUtils;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/** Replaces literals that are required to be emulated. */
public class NormalizeLiterals extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNumberLiteral(NumberLiteral numberLiteral) {
            if (TypeDescriptors.isPrimitiveLong(numberLiteral.getTypeDescriptor())) {
              long longValue = numberLiteral.getValue().longValue();
              int intValue = numberLiteral.getValue().intValue();

              if (longValue == intValue) {
                return RuntimeMethods.createNativeLongMethodCall(
                    "fromInt", NumberLiteral.fromInt(intValue));
              } else {
                long lowOrderBits = longValue << 32 >> 32;
                long highOrderBits = longValue >> 32;
                return RuntimeMethods.createNativeLongMethodCall(
                        "fromBits",
                        NumberLiteral.fromInt((int) lowOrderBits),
                        NumberLiteral.fromInt((int) highOrderBits))
                    .withComment(String.valueOf(longValue));
              }
            } else if (TypeDescriptors.isPrimitiveChar(numberLiteral.getTypeDescriptor())) {
              return numberLiteral.withComment(
                  "'" + StringUtils.escapeAsWtf16(numberLiteral.getValue().intValue()) + "'");

            } else {
              return numberLiteral;
            }
          }
        });
  }
}
