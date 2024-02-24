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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Class for unary expressions. */
@Visitable
public abstract class UnaryExpression extends Expression {
  @Visitable Expression operand;

  UnaryExpression(Expression operand) {
    this.operand = checkNotNull(operand);
  }

  public Expression getOperand() {
    return operand;
  }

  public abstract Operator getOperator();

  @Override
  public boolean isCompileTimeConstant() {
    return !getOperator().hasSideEffect() && getOperand().isCompileTimeConstant();
  }

  @Override
  public boolean isSimpleOrCompoundAssignment() {
    return getOperator().hasSideEffect();
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    if (getOperator() == PrefixOperator.SPREAD) {
      // Not a Java operation on primitives.
      return operand.getTypeDescriptor();
    }

    if (getOperator().hasSideEffect()) {
      // JLS 5.6.1: increment and decrement operations are not subject to unary
      return operand.getTypeDescriptor();
    }

    PrimitiveTypeDescriptor operandTypeDescriptor = operand.getTypeDescriptor().toUnboxedType();

    // JLS 5.6.1
    if (TypeDescriptors.isPrimitiveShort(operandTypeDescriptor)
        || TypeDescriptors.isPrimitiveChar(operandTypeDescriptor)
        || TypeDescriptors.isPrimitiveByte(operandTypeDescriptor)) {
      return PrimitiveTypes.INT;
    }
    return operandTypeDescriptor;
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_UnaryExpression.visit(processor, this);
  }

  abstract Builder<?, ? extends UnaryExpression> createBuilder();

  /** A Builder for unary expressions. */
  public abstract static class Builder<T extends Builder<T, U>, U extends UnaryExpression> {
    private Expression operand;
    private Operator operator;

    public static Builder<?, ? extends UnaryExpression> from(UnaryExpression expression) {
      Builder<?, ? extends UnaryExpression> builder = expression.createBuilder();
      builder.operand = expression.getOperand();
      builder.operator = expression.getOperator();
      return builder;
    }

    public T setOperand(Expression operand) {
      this.operand = operand;
      return getThis();
    }

    public T setOperator(Operator operator) {
      this.operator = operator;
      return getThis();
    }

    @SuppressWarnings("unchecked")
    private T getThis() {
      return (T) this;
    }

    public final U build() {
      return doBuild(operand, operator);
    }

    abstract U doBuild(Expression operand, Operator operator);
  }
}
