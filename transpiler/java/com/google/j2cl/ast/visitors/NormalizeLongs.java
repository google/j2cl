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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

/** Replaces long operations with corresponding long utils method calls. */
public class NormalizeLongs extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Rewriter());
  }

  private static class Rewriter extends AbstractRewriter {
    @Override
    public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
      Expression leftArgument = binaryExpression.getLeftOperand();
      Expression rightArgument = binaryExpression.getRightOperand();
      TypeDescriptor returnTypeDescriptor = binaryExpression.getTypeDescriptor();

      // Skips non-long operations.
      if ((!leftArgument
                  .getTypeDescriptor()
                  .equalsIgnoreNullability(TypeDescriptors.get().primitiveLong)
              && !rightArgument
                  .getTypeDescriptor()
                  .equalsIgnoreNullability(TypeDescriptors.get().primitiveLong))
          || (!returnTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveLong)
              && !returnTypeDescriptor.equalsIgnoreNullability(
                  TypeDescriptors.get().primitiveBoolean))) {
        return binaryExpression;
      }
      BinaryOperator operator = binaryExpression.getOperator();
      // Skips assignment because it doesn't need special handling.
      if (operator == BinaryOperator.ASSIGN) {
        return binaryExpression;
      }

      TypeDescriptor leftParameterTypeDescriptor = TypeDescriptors.get().primitiveLong;
      TypeDescriptor rightParameterTypeDescriptor = TypeDescriptors.get().primitiveLong;

      MethodDescriptor longUtilsMethodDescriptor =
          MethodDescriptor.Builder.fromDefault()
              .setJsInfo(JsInfo.RAW)
              .setIsStatic(true)
              .setEnclosingClassTypeDescriptor(BootstrapType.LONG_UTILS.getDescriptor())
              .setMethodName(getLongOperationFunctionName(operator))
              .setParameterTypeDescriptors(
                  Lists.newArrayList(leftParameterTypeDescriptor, rightParameterTypeDescriptor))
              .setReturnTypeDescriptor(returnTypeDescriptor)
              .build();
      // LongUtils.$someOperation(leftOperand, rightOperand);
      return MethodCall.Builder.from(longUtilsMethodDescriptor)
          .setArguments(Lists.newArrayList(leftArgument, rightArgument))
          .build();
    }

    @Override
    public Node rewritePrefixExpression(PrefixExpression prefixExpression) {
      Expression argument = prefixExpression.getOperand();
      // Only interested in longs.
      if (!argument
          .getTypeDescriptor()
          .equalsIgnoreNullability(TypeDescriptors.get().primitiveLong)) {
        return prefixExpression;
      }
      PrefixOperator operator = prefixExpression.getOperator();
      // Unwrap PLUS operator because it's a NOOP.
      if (operator == PrefixOperator.PLUS) {
        return prefixExpression.getOperand();
      }

      TypeDescriptor parameterTypeDescriptor = TypeDescriptors.get().primitiveLong;
      TypeDescriptor returnTypeDescriptor = TypeDescriptors.get().primitiveLong;

      MethodDescriptor longUtilsMethodDescriptor =
          MethodDescriptor.Builder.fromDefault()
              .setJsInfo(JsInfo.RAW)
              .setIsStatic(true)
              .setEnclosingClassTypeDescriptor(BootstrapType.LONG_UTILS.getDescriptor())
              .setMethodName(getLongOperationFunctionName(operator))
              .setParameterTypeDescriptors(Lists.newArrayList(parameterTypeDescriptor))
              .setReturnTypeDescriptor(returnTypeDescriptor)
              .build();
      // LongUtils.$someOperation(operand);
      return MethodCall.Builder.from(longUtilsMethodDescriptor).setArguments(argument).build();
    }
  }

  private static String getLongOperationFunctionName(PrefixOperator prefixOperator) {
    switch (prefixOperator) {
      case MINUS:
        return "$negate"; // Multiply by -1;
      case COMPLEMENT:
        return "$not"; // Bitwise not
      default:
        checkArgument(
            false, "The requested binary operator is invalid on Longs " + prefixOperator + ".");
        return null;
    }
  }

  private static String getLongOperationFunctionName(BinaryOperator binaryOperator) {
    switch (binaryOperator) {
      case TIMES:
        return "$times";
      case DIVIDE:
        return "$divide";
      case REMAINDER:
        return "$remainder";
      case PLUS:
        return "$plus";
      case MINUS:
        return "$minus";
      case LEFT_SHIFT:
        return "$leftShift";
      case RIGHT_SHIFT_SIGNED:
        return "$rightShiftSigned";
      case RIGHT_SHIFT_UNSIGNED:
        return "$rightShiftUnsigned";
      case LESS:
        return "$less";
      case GREATER:
        return "$greater";
      case LESS_EQUALS:
        return "$lessEquals";
      case GREATER_EQUALS:
        return "$greaterEquals";
      case EQUALS:
        return "$equals";
      case NOT_EQUALS:
        return "$notEquals";
      case BIT_XOR:
        return "$xor";
      case BIT_AND:
        return "$and";
      case BIT_OR:
        return "$or";
      default:
        checkArgument(
            false,
            "The requested binary operator doesn't translate to a LongUtils call: "
                + binaryOperator
                + ".");
        return null;
    }
  }
}
