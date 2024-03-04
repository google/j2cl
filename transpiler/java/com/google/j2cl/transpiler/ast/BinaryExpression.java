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

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/**
 * Binary operator expression.
 */
@Visitable
public class BinaryExpression extends Expression {
  private final TypeDescriptor typeDescriptor;
  @Visitable Expression leftOperand;
  private final BinaryOperator operator;
  @Visitable Expression rightOperand;

  private BinaryExpression(
      Expression leftOperand,
      BinaryOperator operator,
      Expression rightOperand) {
    this.leftOperand = checkNotNull(leftOperand);
    this.operator = checkNotNull(operator);
    this.rightOperand = checkNotNull(rightOperand);
    this.typeDescriptor =
        binaryOperationResultType(
            operator, leftOperand.getTypeDescriptor(), rightOperand.getTypeDescriptor());
    checkArgument(!operator.isSimpleOrCompoundAssignment() || leftOperand.isLValue());
  }

  public Expression getLeftOperand() {
    return leftOperand;
  }

  public BinaryOperator getOperator() {
    return operator;
  }

  public Expression getRightOperand() {
    return rightOperand;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public TypeDescriptor getDeclaredTypeDescriptor() {
    // As opposed to getTypeDescriptor() which is precomputed when the expression is created and
    // does not change even if the operands are rewritten, getDeclaredTypeDescriptor() returns the
    // current declared type descriptor if the expression is an assignment or the precomputed
    // type for the expression if it is not.

    if (operator.isSimpleOrCompoundAssignment()) {
      // From the perspective of the type of binary expression as a value, which is used for
      // conversions on assignment, etc., an assignment (which includes compound assignments) has
      // the same type as the lhs. (e.g.)
      //        class Container<T> {
      //          Container(Object o) { this.data = (T) o; }
      //          T data;
      //        }
      //        Container<String> c = new Container<>(1); // container with inconsistent type.
      //        c.data = c.data;   // allowed as it does no violate runtime type safety.
      //        String s = c.data = c.data // throws CCE on assignment to s, because the type of the
      //                                   // the assignment expression has been specialized to
      //                                   // String.
      return leftOperand.getDeclaredTypeDescriptor();
    }

    // Return the precomputed type for the expression, do not use
    // binaryOperationResult(
    //     leftOperand.getDeclaredTypeDescriptor(), rightOperand.getTypeDescriptor())
    // because binary expressions that are not assignments are not the point of inference for type
    // specialization, hence their declared type has to be exactly the same as its inferred type.
    return getTypeDescriptor();
  }

  @Override
  public Expression.Precedence getPrecedence() {
    return getOperator().getPrecedence();
  }

  @Override
  public boolean isIdempotent() {
    return !operator.hasSideEffect() && leftOperand.isIdempotent() && rightOperand.isIdempotent();
  }

  @Override
  public boolean isCompileTimeConstant() {
    return !operator.hasSideEffect()
        && leftOperand.isCompileTimeConstant()
        && rightOperand.isCompileTimeConstant();
  }

  public boolean isReferenceComparison() {
    return (getOperator() == BinaryOperator.EQUALS || getOperator() == BinaryOperator.NOT_EQUALS)
        && !getLeftOperand().getTypeDescriptor().isPrimitive()
        && !getRightOperand().getTypeDescriptor().isPrimitive()
        // We use the value type in the case of JsEnums with the declaration descriptor to ignore
        // specialization of type variables in case any boxing occurs.
        && !AstUtils.isPrimitiveNonNativeJsEnum(getLeftOperand().getDeclaredTypeDescriptor())
        && !AstUtils.isPrimitiveNonNativeJsEnum(getRightOperand().getDeclaredTypeDescriptor());
  }

  @Override
  public boolean isSimpleAssignment() {
    return getOperator().isSimpleAssignment();
  }

  @Override
  public boolean isSimpleOrCompoundAssignment() {
    return getOperator().isSimpleOrCompoundAssignment();
  }

  @Override
  public boolean canBeNull() {
    // Only plain assignments can return nulls.
    return getOperator().isSimpleAssignment() && super.canBeNull();
  }

  @Override
  public BinaryExpression clone() {
    return newBuilder()
        .setLeftOperand(leftOperand.clone())
        .setOperator(operator)
        .setRightOperand(rightOperand.clone())
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_BinaryExpression.visit(processor, this);
  }

  /** Determines the binary operation type based on the types of the operands. */
  private static TypeDescriptor binaryOperationResultType(
      BinaryOperator operator, TypeDescriptor leftOperandType, TypeDescriptor rightOperandType) {

    if (operator.isSimpleOrCompoundAssignment()) {
      return leftOperandType;
    }

    if (isStringConcatenation(operator, leftOperandType, rightOperandType)) {
      return TypeDescriptors.get().javaLangString.toNonNullable();
    }

    if (operator.isRelationalOperator()) {
      return PrimitiveTypes.BOOLEAN;
    }

    PrimitiveTypeDescriptor primitiveLeftOperandType = leftOperandType.toUnboxedType();

    /**
     * Rules per JLS (Chapter 15) require that binary promotion be previously applied to the
     * operands and makes the operation to be the same type as both operands. Since this method is
     * potentially called before or while numeric promotion is being performed there is no guarantee
     * operand promotion was already performed; so that fact is taken into account.
     */
    switch (operator) {
        /*
         * Conditional operators: JLS 15.23.
         */
      case CONDITIONAL_AND:
      case CONDITIONAL_OR:
        /*
         * Bitwise and logical operators: JLS 15.22.
         */
      case BIT_AND:
      case BIT_OR:
      case BIT_XOR:
        if (TypeDescriptors.isPrimitiveBoolean(primitiveLeftOperandType)) {
          // Handle logical operations (on type boolean).
          return primitiveLeftOperandType;
        }
        // fallthrough for bitwise operations on numbers.
        /*
         * Additive operators for numeric types: JLS 15.18.2.
         */
      case PLUS:
      case MINUS:
        /*
         * Multiplicative operators for numeric types: JLS 15.17.
         */
      case TIMES:
      case DIVIDE:
      case REMAINDER:
        /**
         * The type of the operation should the promoted type of the operands, which is equivalent
         * to the widest type of its operands (or integer is integer is wider).
         */
        checkArgument(TypeDescriptors.isBoxedOrPrimitiveType(rightOperandType));
        return AstUtils.getNumericBinaryExpressionTypeDescriptor(
            primitiveLeftOperandType, rightOperandType.toUnboxedType());
      case LEFT_SHIFT:
      case RIGHT_SHIFT_SIGNED:
      case RIGHT_SHIFT_UNSIGNED:
        /**
         * Shift operators: JLS 15.19.
         *
         * <p>Type type of the operation is the type of the promoted left hand operand.
         */
        return AstUtils.getNumericUnaryExpressionTypeDescriptor(primitiveLeftOperandType);
      default:
        // All binary operators should have been handled.
        throw new IllegalStateException("Unhandled operator: " + operator);
    }
  }

  public boolean isStringConcatenation() {
    return isStringConcatenation(
        operator, leftOperand.getTypeDescriptor(), rightOperand.getTypeDescriptor());
  }

  private static boolean isStringConcatenation(
      BinaryOperator operator, TypeDescriptor leftOperandType, TypeDescriptor rightOperandType) {

    if (!operator.isPlusOperator()) {
      return false;
    }

    if (TypeDescriptors.isJavaLangString(leftOperandType.toRawTypeDescriptor())
        || TypeDescriptors.isJavaLangString(rightOperandType.toRawTypeDescriptor())) {
      return true;
    }

    return false;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * A Builder for binary expressions.
   */
  public static class Builder {
    private BinaryOperator operator;
    private Expression leftOperand;
    private Expression rightOperand;

    public static Builder from(BinaryExpression expression) {
      return new Builder()
          .setLeftOperand(expression.getLeftOperand())
          .setOperator(expression.getOperator())
          .setRightOperand(expression.getRightOperand());
    }

    public static Builder asAssignmentTo(Expression lvalue) {
      return new Builder().setLeftOperand(lvalue).setOperator(BinaryOperator.ASSIGN);
    }

    public static Builder asAssignmentTo(Field field) {
      return asAssignmentTo(field.getDescriptor());
    }

    public static Builder asAssignmentTo(FieldDescriptor fieldDescriptor) {

      return new Builder()
          .setLeftOperand(
              FieldAccess.Builder.from(fieldDescriptor).setDefaultInstanceQualifier().build())
          .setOperator(BinaryOperator.ASSIGN);
    }

    public static Builder asAssignmentTo(Variable variable) {
      return new Builder()
          .setLeftOperand(variable.createReference())
          .setOperator(BinaryOperator.ASSIGN);
    }

    public Builder setLeftOperand(Expression operand) {
      this.leftOperand = operand;
      return this;
    }

    public Builder setLeftOperand(Variable variable) {
      this.leftOperand = variable.createReference();
      return this;
    }

    public Builder setRightOperand(Expression operand) {
      this.rightOperand = operand;
      return this;
    }

    public Builder setRightOperand(Variable variable) {
      this.rightOperand = variable.createReference();
      return this;
    }

    public Builder setOperator(BinaryOperator operator) {
      this.operator = operator;
      return this;
    }

    public final BinaryExpression build() {
      return new BinaryExpression(leftOperand, operator, rightOperand);
    }
  }
}
