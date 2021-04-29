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

import static com.google.common.base.Preconditions.checkState;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveFloatOrDouble;
import static com.google.j2cl.transpiler.backend.wasm.GenerationEnvironment.getWasmTypeForPrimitive;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.Map;

/** Abstract the wasm binary instructions set used by j2wasm. */
public enum WasmBinaryOperation {
  DIV("_s"),
  SUB,
  ADD,
  MUL,
  REM_S,
  SHL,
  SHR_U,
  SHR_S,
  LT("_s"),
  GT("_s"),
  LE("_s"),
  GE("_s"),
  EQ,
  NE,
  XOR,
  AND,
  OR;

  private final String integerSuffix;

  WasmBinaryOperation() {
    this.integerSuffix = "";
  }

  WasmBinaryOperation(String integerSuffix) {
    this.integerSuffix = integerSuffix;
  }

  public String getInstruction(BinaryExpression expression) {
    TypeDescriptor operandType = getOperandType(expression);
    String prefix = operandType.isPrimitive() ? getWasmTypeForPrimitive(operandType) : "ref";
    String suffix = isPrimitiveFloatOrDouble(operandType) ? "" : integerSuffix;
    return prefix + "." + Ascii.toLowerCase(name()) + suffix;
  }

  public TypeDescriptor getOperandType(BinaryExpression expression) {
    if (expression.isReferenceComparison()) {
      return TypeDescriptors.get().javaLangObject;
    }

    checkState(expression.getTypeDescriptor().isPrimitive());

    // TODO(dramaix): when coercion are implemented, we should always return the left operand type
    //  descriptor for both relational and arithmetic operation.
    if (expression.getOperator().isRelationalOperator()) {
      // For relational expressions, the type of the wasm instruction to use is the type of the
      // operands. E.g. if you compare int, you need to use i32.eq; if you compare float you need to
      // use f32.eq.
      return expression.getLeftOperand().getTypeDescriptor();
    }

    // For Arithmetic expressions, the type of the wasm instruction to use is the type of the
    // BinaryExpression result. In the future, operands will be casted/coerced upfront.
    return expression.getTypeDescriptor();
  }

  private static final Map<BinaryOperator, WasmBinaryOperation> wasmOperationByBinaryOperator =
      ImmutableMap.<BinaryOperator, WasmBinaryOperation>builder()
          .put(BinaryOperator.DIVIDE, WasmBinaryOperation.DIV)
          .put(BinaryOperator.MINUS, WasmBinaryOperation.SUB)
          .put(BinaryOperator.PLUS, WasmBinaryOperation.ADD)
          .put(BinaryOperator.TIMES, WasmBinaryOperation.MUL)
          .put(BinaryOperator.REMAINDER, WasmBinaryOperation.REM_S)
          .put(BinaryOperator.LEFT_SHIFT, WasmBinaryOperation.SHL)
          .put(BinaryOperator.RIGHT_SHIFT_SIGNED, WasmBinaryOperation.SHR_S)
          .put(BinaryOperator.RIGHT_SHIFT_UNSIGNED, WasmBinaryOperation.SHR_U)
          .put(BinaryOperator.LESS, WasmBinaryOperation.LT)
          .put(BinaryOperator.GREATER, WasmBinaryOperation.GT)
          .put(BinaryOperator.LESS_EQUALS, WasmBinaryOperation.LE)
          .put(BinaryOperator.GREATER_EQUALS, WasmBinaryOperation.GE)
          .put(BinaryOperator.EQUALS, WasmBinaryOperation.EQ)
          .put(BinaryOperator.NOT_EQUALS, WasmBinaryOperation.NE)
          .put(BinaryOperator.BIT_XOR, WasmBinaryOperation.XOR)
          .put(BinaryOperator.BIT_AND, WasmBinaryOperation.AND)
          .put(BinaryOperator.BIT_OR, WasmBinaryOperation.OR)
          .build();

  public static WasmBinaryOperation getOperation(BinaryExpression expression) {
    BinaryOperator operator = expression.getOperator();

    checkState(wasmOperationByBinaryOperator.containsKey(operator));

    if (!expression.getTypeDescriptor().isPrimitive() && operator == BinaryOperator.PLUS) {
      // TODO(b/170691638): handle string concatenation
      return null;
    }

    return wasmOperationByBinaryOperator.get(operator);
  }
}
