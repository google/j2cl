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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveInt;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveLong;

import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import javax.annotation.Nullable;

/**
 * Class for prefix unary expressions.
 */
@Visitable
public class PrefixExpression extends UnaryExpression {
  private final PrefixOperator operator;

  private PrefixExpression(Expression operand, PrefixOperator operator) {
    super(operand);
    this.operator = checkNotNull(operator);
    checkArgument(!operator.hasSideEffect() || operand.isLValue());
  }

  @Override
  public PrefixOperator getOperator() {
    return operator;
  }

  @Override
  public Precedence getPrecedence() {
    return Precedence.PREFIX;
  }

  @Override
  @Nullable
  public Literal getConstantValue() {
    if (!isCompileTimeConstant()) {
      return null;
    }

    TypeDescriptor operationType = getTypeDescriptor();
    Literal operandValue = operand.getConstantValue();

    return switch (getOperator()) {
      case NOT -> BooleanLiteral.get(!((BooleanLiteral) operand.getConstantValue()).getValue());
      // JLS 15.15.5
      case COMPLEMENT -> {
        if (isPrimitiveLong(operationType)) {
          yield NumberLiteral.fromLong(~((NumberLiteral) operandValue).getValue().longValue());
        } else {
          checkState(isPrimitiveInt(operationType));
          yield NumberLiteral.fromInt(~((NumberLiteral) operandValue).getValue().intValue());
        }
      }
      case PLUS -> operandValue.getConstantValue();
      case MINUS -> {
        Number number = ((NumberLiteral) operandValue).getValue();
        if (TypeDescriptors.isPrimitiveLong(operationType)) {
          yield NumberLiteral.fromLong(-number.longValue());
        } else if (TypeDescriptors.isPrimitiveFloat(operationType)) {
          yield NumberLiteral.fromFloat(-number.floatValue());
        } else if (TypeDescriptors.isPrimitiveDouble(operationType)) {
          yield NumberLiteral.fromDouble(-number.doubleValue());
        } else {
          checkState(isPrimitiveInt(operationType));
          // For all other numeric types the operation is performed as integer.
          yield new NumberLiteral((PrimitiveTypeDescriptor) operationType, -number.intValue());
        }
      }
      default ->
          throw new InternalCompilerError(
              "Unexpected compile-tyme constant expression: %s%s",
              operationType, operandValue.getConstantValue());
    };
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_PrefixExpression.visit(processor, this);
  }

  @Override
  public PrefixExpression clone() {
    return new PrefixExpression(getOperand().clone(), operator);
  }

  @Override
  Builder createBuilder() {
    return newBuilder();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for prefix unary expressions. */
  public static class Builder extends UnaryExpression.Builder<Builder, PrefixExpression> {

    public static Builder from(UnaryExpression expression) {
      return newBuilder().setOperand(expression.getOperand()).setOperator(expression.getOperator());
    }

    @Override
    PrefixExpression doBuild(Expression operand, Operator operator) {
      return new PrefixExpression(operand, (PrefixOperator) operator);
    }
  }
}
