/*
 * Copyright 2025 Google Inc.
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
package com.google.j2cl.transpiler.ast;

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveChar;

import com.google.j2cl.common.InternalCompilerError;

/** Helpers to perform static evaluation of compile-time constant expressions. */
class StaticEvaluators {
  static int intOperation(BinaryOperator operator, int left, int right) {
    return switch (operator) {
      case PLUS -> left + right;
      case MINUS -> left - right;
      case TIMES -> left * right;
      case DIVIDE -> left / right;
      case REMAINDER -> left % right;
      case BIT_OR -> left | right;
      case BIT_AND -> left & right;
      case BIT_XOR -> left ^ right;
      case RIGHT_SHIFT_SIGNED -> left >> right;
      case RIGHT_SHIFT_UNSIGNED -> left >>> right;
      case LEFT_SHIFT -> left << right;
      default -> throw new IllegalArgumentException();
    };
  }

  static long longOperation(BinaryOperator operator, long left, long right) {
    return switch (operator) {
      case PLUS -> left + right;
      case MINUS -> left - right;
      case TIMES -> left * right;
      case DIVIDE -> left / right;
      case REMAINDER -> left % right;
      case BIT_OR -> left | right;
      case BIT_AND -> left & right;
      case BIT_XOR -> left ^ right;
      case RIGHT_SHIFT_SIGNED -> left >> right;
      case RIGHT_SHIFT_UNSIGNED -> left >>> right;
      case LEFT_SHIFT -> left << right;
      default -> throw new IllegalArgumentException();
    };
  }

  static float floatOperation(BinaryOperator operator, float left, float right) {
    return switch (operator) {
      case PLUS -> left + right;
      case MINUS -> left - right;
      case TIMES -> left * right;
      case DIVIDE -> left / right;
      case REMAINDER -> left % right;
      default -> throw new IllegalArgumentException();
    };
  }

  static double doubleOperation(BinaryOperator operator, double left, double right) {
    return switch (operator) {
      case PLUS -> left + right;
      case MINUS -> left - right;
      case TIMES -> left * right;
      case DIVIDE -> left / right;
      case REMAINDER -> left % right;
      default -> throw new IllegalArgumentException();
    };
  }

  static boolean longRelationalOperator(BinaryOperator operator, long left, long right) {
    return switch (operator) {
      case LESS -> left < right;
      case LESS_EQUALS -> left <= right;
      case EQUALS -> left == right;
      case NOT_EQUALS -> left != right;
      case GREATER -> left > right;
      case GREATER_EQUALS -> left >= right;
      default -> throw new IllegalArgumentException();
    };
  }

  static boolean doubleRelationalOperator(BinaryOperator operator, double left, double right) {
    return switch (operator) {
      case LESS -> left < right;
      case LESS_EQUALS -> left <= right;
      case EQUALS -> left == right;
      case NOT_EQUALS -> left != right;
      case GREATER -> left > right;
      case GREATER_EQUALS -> left >= right;
      default -> throw new IllegalArgumentException();
    };
  }

  static boolean booleanOperation(BinaryOperator operator, boolean left, boolean right) {
    return switch (operator) {
      case CONDITIONAL_AND -> left && right;
      case CONDITIONAL_OR -> left || right;
      default -> throw new IllegalArgumentException();
    };
  }

  static String stringOperation(BinaryOperator operator, String left, String right) {
    return switch (operator) {
      case PLUS -> left + right;
      default -> throw new IllegalArgumentException();
    };
  }

  static String convertToLiteralString(Literal literal) {
    return switch (literal) {
      case BooleanLiteral b -> String.valueOf(b.getValue());
      case StringLiteral s -> s.getValue();
      case NumberLiteral n when isPrimitiveChar(n.getTypeDescriptor()) ->
          String.valueOf((char) n.getValue().intValue());
      case NumberLiteral n -> String.valueOf(n.getValue());
      case NullLiteral n -> "null";
      default -> throw new InternalCompilerError("Unexpected literal: %s", literal);
    };
  }

  private StaticEvaluators() {}
}
