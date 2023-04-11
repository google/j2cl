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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.j2cl.transpiler.backend.wasm.WasmGenerationEnvironment.getWasmTypeForPrimitive;

import com.google.common.base.Ascii;
import com.google.j2cl.transpiler.ast.PrefixExpression;
import com.google.j2cl.transpiler.ast.PrefixOperator;
import com.google.j2cl.transpiler.ast.UnaryExpression;

/** Abstract the wasm unary instructions set used by j2wasm. */
public enum WasmUnaryOperation {
  EQZ,
  NEG;

  public static WasmUnaryOperation get(UnaryExpression expression) {
    // PostfixExpressions have been transformed to PrefixExpressions
    checkArgument(expression instanceof PrefixExpression);

    PrefixOperator operator = ((PrefixExpression) expression).getOperator();
    switch (operator) {
      case NOT:
        return EQZ;
      case MINUS:
        return NEG;
      default:
        // other PrefixOperator have been normalized
        throw new AssertionError("Invalid operator");
    }
  }

  public String getInstruction(UnaryExpression expression) {
    return getWasmTypeForPrimitive(expression.getOperand().getTypeDescriptor())
        + "."
        + Ascii.toLowerCase(name());
  }
}
