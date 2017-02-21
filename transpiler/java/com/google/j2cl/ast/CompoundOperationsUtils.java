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
package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility functions for expanding compound, increment and decrement expressions.
 *
 * <p>For example:
 *
 * <ul>
 *   <li>i++ becomes i = i + 1
 *   <li>a = i++ becomes a = (v = i, i = i + 1, v)
 *   <li>a = change().i++ becomes a = (q = change(), v = q.i, q.i = q.i + 1, v)
 * </ul>
 *
 * <p>This kind of rewrite is useful when the numeric type in question is represented by an
 * immutable emulation class or when a conversion operation (such as boxing or unboxing) needs to be
 * inserted between the numeric and assignment stages of an operation.
 */
public class CompoundOperationsUtils {
  public static Expression expandCompoundExpression(BinaryExpression binaryExpression) {
    checkArgument(binaryExpression.getOperator().isCompoundAssignment());

    BinaryOperator operator = binaryExpression.getOperator();
    Expression leftOperand = binaryExpression.getLeftOperand();
    Expression rightOperand = binaryExpression.getRightOperand();

    if (leftOperand.isIdempotent()) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // a += x => a = a + x
      return assignToLeftOperand(leftOperand, operator.getUnderlyingBinaryOperator(), rightOperand);
    }

    if (leftOperand instanceof ArrayAccess) {
      return expandArrayAccess(
          operator.getUnderlyingBinaryOperator(), (ArrayAccess) leftOperand, rightOperand);
    }
    return expandQualifiedFieldAccess(
        operator.getUnderlyingBinaryOperator(), (FieldAccess) leftOperand, rightOperand);
  }

  private static Expression expandQualifiedFieldAccess(
      BinaryOperator operator, FieldAccess leftOperand, Expression rightOperand) {
    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // q.a += b; => (let $qualifier = q, $qualifier.a = $qualifier.a + b)
    FieldAccess targetFieldAccess = leftOperand;
    Expression qualifier = targetFieldAccess.getQualifier().clone();
    Variable qualifierVariable =
        Variable.newBuilder()
            .setIsFinal(true)
            .setName("$qualifier")
            .setTypeDescriptor(qualifier.getTypeDescriptor())
            .build();
    return MultiExpression.newBuilder()
        .setExpressions(
            // Declare the temporary variable and initialize to the evaluated qualifier.
            VariableDeclarationExpression.newBuilder()
                .addVariableDeclaration(qualifierVariable, qualifier)
                .build(),
            assignToLeftOperand(
                FieldAccess.Builder.from(targetFieldAccess)
                    .setQualifier(qualifierVariable.getReference())
                    .build(),
                operator,
                rightOperand))
        .build();
  }


  private static Expression expandArrayAccess(
      BinaryOperator operator, ArrayAccess leftOperand, Expression rightOperand) {
    return expandArrayAccessReturningPreValue(operator, leftOperand, rightOperand, null);
  }

  private static Expression expandArrayAccessReturningPreValue(
      BinaryOperator operator,
      ArrayAccess leftOperand,
      Expression rightOperand,
      Variable valueVariable) {
    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // q.a += b; => (let $qualifier = q, $qualifier.a = $qualifier.a + b)
    List<Expression> expressions = new ArrayList<>();
    List<VariableDeclarationFragment> variableDeclarationFragments = new ArrayList<>();

    Expression arrayExpression = leftOperand.getArrayExpression();
    Expression indexExpression = leftOperand.getIndexExpression();
    if (!arrayExpression.isIdempotent() || !indexExpression.isIdempotent()) {
      // If index expression can not be evaluated twice it might have a side effect that affects
      // the array expression. In that case, the value for the array expression is obtained and
      // stored in $array.
      Variable arrayExpressionVariable =
          Variable.newBuilder()
              .setIsFinal(true)
              .setName("$array")
              .setTypeDescriptor(arrayExpression.getTypeDescriptor())
              .build();
      variableDeclarationFragments.add(
          new VariableDeclarationFragment(arrayExpressionVariable, arrayExpression));
      arrayExpression = arrayExpressionVariable.getReference();
    }

    if (!indexExpression.isIdempotent()) {
      Variable indexExpressionVariable =
          Variable.newBuilder()
              .setIsFinal(true)
              .setName("$index")
              .setTypeDescriptor(TypeDescriptors.get().primitiveInt)
              .build();
      variableDeclarationFragments.add(
          new VariableDeclarationFragment(indexExpressionVariable, indexExpression));
      indexExpression = indexExpressionVariable.getReference();
    }

    ArrayAccess arrayAccess =
        ArrayAccess.newBuilder()
            .setArrayExpression(arrayExpression)
            .setIndexExpression(indexExpression)
            .build();
    if (valueVariable != null) {
      variableDeclarationFragments.add(
          new VariableDeclarationFragment(valueVariable, arrayAccess.clone()));
    }

    if (!variableDeclarationFragments.isEmpty()) {
      // Declare the temporary variable and initialize to the evaluated qualifier.
      expressions.add(
          VariableDeclarationExpression.newBuilder()
              .setVariableDeclarationFragments(variableDeclarationFragments)
              .build());
    }
    expressions.add(assignToLeftOperand(arrayAccess, operator, rightOperand));
    if (valueVariable != null) {
      expressions.add(valueVariable.getReference());
    }

    if (expressions.size() == 1) {
      return expressions.get(0);
    }
    return MultiExpression.newBuilder().addExpressions(expressions).build();
  }

  public static Expression expandExpression(PostfixExpression postfixExpression) {
    Expression operand = postfixExpression.getOperand();
    PostfixOperator operator = postfixExpression.getOperator();
    Variable valueVariable =
        Variable.newBuilder()
            .setIsFinal(true)
            .setName("$value")
            .setTypeDescriptor(operand.getTypeDescriptor())
            .build();

    if (operand.isIdempotent()) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // a++; => (let $value = a, a = a + 1, $value)

      return MultiExpression.newBuilder()
          .setExpressions(
              // Declare the temporary variable to hold the initial value.
              VariableDeclarationExpression.newBuilder()
                  .addVariableDeclaration(valueVariable, operand)
                  .build(),
              assignToLeftOperand(
                  operand.clone(),
                  operator.getUnderlyingBinaryOperator(),
                  createLiteralOne(operand.getTypeDescriptor())),
              valueVariable.getReference())
          .build();
    }

    VariableDeclarationExpression variableDeclaration;
    Expression expandedOperand;
    if (operand instanceof ArrayAccess) {
      return expandArrayAccessReturningPreValue(
          operator.getUnderlyingBinaryOperator(),
          (ArrayAccess) operand,
          createLiteralOne(operand.getTypeDescriptor()),
          valueVariable);
    }

    FieldAccess fieldAccess = (FieldAccess) operand;
    Expression qualifier = fieldAccess.getQualifier().clone();
    Variable qualifierVariable =
        Variable.newBuilder()
            .setIsFinal(true)
            .setName("$qualifier")
            .setTypeDescriptor(qualifier.getTypeDescriptor())
            .build();
    expandedOperand =
        FieldAccess.Builder.from(fieldAccess)
            .setQualifier(qualifierVariable.getReference())
            .build();
    variableDeclaration =
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(qualifierVariable, qualifier)
            .addVariableDeclaration(valueVariable, expandedOperand.clone())
            .build();
    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // q.a++; =>
    // (let $qualifier = q, let $value = $qualifier.a, $qualifier.a = $qualifier.a + 1, $value)

    return MultiExpression.newBuilder()
        .setExpressions(
            // Declare the temporary variables to hold the qualifier and the initial value.
            variableDeclaration,
            assignToLeftOperand(
                expandedOperand,
                operator.getUnderlyingBinaryOperator(),
                createLiteralOne(operand.getTypeDescriptor())),
            valueVariable.getReference())
        .build();
  }

  public static Expression expandExpression(PrefixExpression prefixExpression) {
    checkArgument(prefixExpression.getOperator().hasSideEffect());

    Expression operand = prefixExpression.getOperand();
    PrefixOperator operator = prefixExpression.getOperator();

    if (operand.isIdempotent()) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // ++a => a = a + 1
      return assignToLeftOperand(
          operand,
          operator.getUnderlyingBinaryOperator(),
          createLiteralOne(operand.getTypeDescriptor()));
    }

    if (operand instanceof FieldAccess) {
      // Treat as a++ as a += 1.
      return expandQualifiedFieldAccess(
          operator.getUnderlyingBinaryOperator(),
          (FieldAccess) operand,
          createLiteralOne(operand.getTypeDescriptor()));
    }

    // Treat as a[i]++ as a[i] += 1.
    return expandArrayAccess(
        operator.getUnderlyingBinaryOperator(),
        (ArrayAccess) operand,
        createLiteralOne(operand.getTypeDescriptor()));
  }

  /** Returns number literal with value 1. */
  public static NumberLiteral createLiteralOne(TypeDescriptor typeDescriptor) {
    TypeDescriptor primitiveTypeDescriptor =
        TypeDescriptors.isBoxedType(typeDescriptor)
            ? TypeDescriptors.getPrimitiveTypeFromBoxType(typeDescriptor)
            : typeDescriptor;

    return new NumberLiteral(primitiveTypeDescriptor, 1);
  }

  /** Returns assignment in the form of {leftOperand = leftOperand operator rightOperand}. */
  private static BinaryExpression assignToLeftOperand(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {

    return BinaryExpression.Builder.asAssignmentTo(leftOperand)
        .setRightOperand(
            BinaryExpression.newBuilder()
                .setTypeDescriptor(
                    binaryOperationResultType(
                        operator,
                        leftOperand.getTypeDescriptor(),
                        rightOperand.getTypeDescriptor()))
                .setLeftOperand(leftOperand.clone())
                .setOperator(operator)
                .setRightOperand(rightOperand)
                .build())
        .build();
  }

  /** Determines the binary operation type based on the types of the operands. */
  private static TypeDescriptor binaryOperationResultType(
      BinaryOperator operator, TypeDescriptor leftOperandType, TypeDescriptor rightOperandType) {

    if (TypeDescriptors.get().isJavaLangString(leftOperandType)) {
      return leftOperandType;
    }

    leftOperandType = getCorrespondingPrimitiveType(leftOperandType);

    /**
     * Rules per JLS (Chapter 15) require that binary promotion be previously applied to the
     * operands and makes the operation to be the same type as both operands. Since this method is
     * potentially called before or while numeric promotion is being performed there is no guarantee
     * operand promotion was already performed; so that fact is taken into account.
     */
    switch (operator) {
        /*
         * Bitwise and logical operators: JLS 15.22.
         */
      case BIT_AND:
      case BIT_OR:
      case BIT_XOR:
        if (TypeDescriptors.isPrimitiveBoolean(leftOperandType)) {
          // Handle logical operations (on type boolean).
          return leftOperandType;
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
        // TODO: Return primitiveInt if wider. Due to order in which promotion operations are
        // applied doing so here breaks code.
        checkArgument(TypeDescriptors.isBoxedOrPrimitiveType(rightOperandType));
        return widerType(leftOperandType, getCorrespondingPrimitiveType(rightOperandType));
      case LEFT_SHIFT:
      case RIGHT_SHIFT_SIGNED:
      case RIGHT_SHIFT_UNSIGNED:
        /**
         * Shift operators: JLS 15.19.
         *
         * <p>Type type of the operation is the type of the promoted left hand operand.
         */
        return leftOperandType;
      default:
        // This method only handles operations resulting from unfolding compound assignment
        // expressions.
        throw new IllegalArgumentException();
    }
  }

  /** Returns the type descriptor for the wider type. */
  private static TypeDescriptor widerType(
      TypeDescriptor thisTypeDescriptor, TypeDescriptor thatTypeDescriptor) {
    return TypeDescriptors.getWidth(thatTypeDescriptor)
            > TypeDescriptors.getWidth(thisTypeDescriptor)
        ? thatTypeDescriptor
        : thisTypeDescriptor;
  }

  /**
   * Returns the corresponding primitive type if the {@code setTypeDescriptor} is a boxed type;
   * {@code typeDescriptor} otherwise
   */
  public static TypeDescriptor getCorrespondingPrimitiveType(TypeDescriptor typeDescriptor) {
    if (TypeDescriptors.isBoxedType(typeDescriptor)) {
      return TypeDescriptors.getPrimitiveTypeFromBoxType(typeDescriptor);
    }
    checkArgument(typeDescriptor.isPrimitive());
    return typeDescriptor;
  }
}
