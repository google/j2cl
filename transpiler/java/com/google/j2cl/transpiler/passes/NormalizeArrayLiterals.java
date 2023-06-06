/*
 * Copyright 2015 Google Inc.
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
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Rewrites the rare short form array literal initializers (like "int[] foo = {1, 2, 3};") into the
 * more common long form (like "int[] foo = new int[] {1, 2, 3};").
 */
public class NormalizeArrayLiterals extends NormalizationPass {

  private final Set<ArrayLiteral> longFormArrayLiterals = new HashSet<>();

  @Override
  public void applyTo(Type type) {
    // Collect long form array literals
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitNewArray(NewArray newArray) {
            if (newArray.getInitializer() instanceof ArrayLiteral) {
              longFormArrayLiterals.add((ArrayLiteral) newArray.getInitializer());
            }
          }
        });

    // Rewrite short form array literals
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
            if (longFormArrayLiterals.contains(arrayLiteral)) {
              return arrayLiteral;
            }

            return NewArray.newBuilder()
                .setTypeDescriptor(arrayLiteral.getTypeDescriptor())
                .setDimensionExpressions(
                    AstUtils.createListOfNullValues(
                        arrayLiteral.getTypeDescriptor().getDimensions()))
                .setInitializer(arrayLiteral)
                .build();
          }
        });
  }
}
