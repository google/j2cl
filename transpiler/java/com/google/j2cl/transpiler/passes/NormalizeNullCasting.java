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
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;

// TODO(b/182007249): remove this pass when j2wasm use WasmGC milestone 3.
/**
 * Temporary pass to work around the issue of illegal casting of null reference.
 *
 * <p>rewrite cast expression:
 *
 * <pre>{@code
 * (Foo) getBar();
 * -> ($cast_expression = getBar(), $cast_expression != null ? (Foo) $cast_expression : null)
 * }</pre>
 */
public class NormalizeNullCasting extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteCastExpression(CastExpression castExpression) {
            if (castExpression.getCastTypeDescriptor().isPrimitive()
                || castExpression.getExpression().getTypeDescriptor().isPrimitive()) {
              return castExpression;
            }

            // Evaluate the expression to cast once.
            Variable expressionVariable =
                Variable.newBuilder()
                    .setFinal(true)
                    .setName("$cast_expression")
                    .setTypeDescriptor(TypeDescriptors.get().javaLangObject)
                    .build();

            VariableDeclarationExpression expressionVariableDeclaration =
                VariableDeclarationExpression.newBuilder()
                    .addVariableDeclaration(expressionVariable, castExpression.getExpression())
                    .build();

            // $cast_expression == null ? null : /* do the cast */;
            ConditionalExpression conditionalCast =
                ConditionalExpression.newBuilder()
                    .setConditionExpression(
                        MethodCall.Builder.from(
                                TypeDescriptors.get()
                                    .javaemulInternalPlatform
                                    .getMethodDescriptorByName("isNull"))
                            .setArguments(expressionVariable.createReference())
                            .build())
                    .setFalseExpression(
                        CastExpression.newBuilder()
                            .setExpression(expressionVariable.createReference())
                            .setCastTypeDescriptor(castExpression.getCastTypeDescriptor())
                            .build())
                    .setTrueExpression(castExpression.getCastTypeDescriptor().getDefaultValue())
                    .setTypeDescriptor(castExpression.getCastTypeDescriptor())
                    .build();

            return MultiExpression.newBuilder()
                .addExpressions(expressionVariableDeclaration, conditionalCast)
                .build();
          }
        });
  }
}
