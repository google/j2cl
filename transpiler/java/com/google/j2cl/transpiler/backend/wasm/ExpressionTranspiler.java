/*
 * Copyright 2020 Google Inc.
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
package com.google.j2cl.transpiler.backend.wasm;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionWithComment;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;

/** Transforms expressions into WASM code. */
final class ExpressionTranspiler {
  public static void render(
      Expression expression,
      final SourceBuilder sourceBuilder,
      final GenerationEnvironment environment) {

    new AbstractVisitor() {
      @Override
      public boolean enterBooleanLiteral(BooleanLiteral booleanLiteral) {
        sourceBuilder.append("(i32.const " + (booleanLiteral.getValue() ? "1" : "0") + ")");
        return false;
      }

      @Override
      public boolean enterExpressionWithComment(ExpressionWithComment expressionWithComment) {
        sourceBuilder.append(";; " + expressionWithComment.getComment());
        render(expressionWithComment.getExpression());
        return false;
      }

      @Override
      public boolean enterMultiExpression(MultiExpression multiExpression) {
        multiExpression.getExpressions().forEach(this::render);
        return false;
      }

      @Override
      public boolean enterNullLiteral(NullLiteral nullLiteral) {
        sourceBuilder.append("(ref.null MISSING TYPE)");
        return false;
      }

      @Override
      public boolean enterNumberLiteral(NumberLiteral numberLiteral) {
        PrimitiveTypeDescriptor typeDescriptor = numberLiteral.getTypeDescriptor();
        String wasmType = checkNotNull(environment.getWasmType(typeDescriptor));
        sourceBuilder.append("(" + wasmType + ".const " + numberLiteral.getValue() + ")");
        return false;
      }

      private void render(Expression expression) {
        expression.accept(this);
      }
    }.render(expression);
  }

  private ExpressionTranspiler() {}
}
