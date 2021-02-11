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
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Node;

/**
 * In wasm, object reference arrays are modelled as Array of Objects. We need to introduce an
 * explicit cast on each array access in order to get the right type at runtime.
 */
public class InsertCastOnArrayAccess extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteArrayAccess(ArrayAccess arrayAccess) {
            if (isPrimitiveArray(arrayAccess)) {
              return arrayAccess;
            }

            if (AstUtils.isExpressionResultUsed(arrayAccess, getParent())) {
              return CastExpression.newBuilder()
                  .setCastTypeDescriptor(arrayAccess.getTypeDescriptor())
                  .setExpression(arrayAccess)
                  .build();
            }

            return arrayAccess;
          }
        });
  }

  private static boolean isPrimitiveArray(ArrayAccess arrayAccess) {
    return ((ArrayTypeDescriptor) arrayAccess.getArrayExpression().getTypeDescriptor())
        .getComponentTypeDescriptor()
        .isPrimitive();
  }
}
