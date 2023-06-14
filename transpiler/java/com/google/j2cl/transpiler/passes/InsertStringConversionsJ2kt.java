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

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangString;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.TypeDescriptor;

/** Inserts string conversion for Kotlin. */
public class InsertStringConversionsJ2kt extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (!binaryExpression.isStringConcatenation()) {
              return binaryExpression;
            }

            TypeDescriptor leftTypeDescriptor =
                binaryExpression.getLeftOperand().getTypeDescriptor();
            if (!leftTypeDescriptor.isNullable() && isJavaLangString(leftTypeDescriptor)) {
              return binaryExpression;
            }

            return BinaryExpression.Builder.from(binaryExpression)
                .setLeftOperand(
                    BinaryExpression.newBuilder()
                        .setLeftOperand(new StringLiteral(""))
                        .setOperator(BinaryOperator.PLUS)
                        .setRightOperand(binaryExpression.getLeftOperand())
                        .build())
                .build();
          }
        });
  }
}
