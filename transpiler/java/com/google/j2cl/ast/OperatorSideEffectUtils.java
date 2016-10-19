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

import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

/**
 * Utility functions for splitting operators expressions that have a side effect.
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
public class OperatorSideEffectUtils {

  public static final String NUMBERS_QUALIFIER_TEMP = "$q";
  public static final String NUMBERS_VALUE_TEMP = "$v";

  public static Expression splitBinaryExpression(BinaryExpression binaryExpression) {
    checkArgument(binaryExpression.getOperator().isCompoundAssignment());

    BinaryOperator operator = binaryExpression.getOperator();
    Expression leftOperand = binaryExpression.getLeftOperand();
    Expression rightOperand = binaryExpression.getRightOperand();

    if (canExpressionBeEvaluatedTwice(leftOperand)) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // a += x => a = a + x
      return expandExpressionNoQualifier(
          leftOperand, operator.getUnderlyingBinaryOperator(), rightOperand);
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // q.a += b; => (Numbers.$q = q, Numbers.$q.a = Numbers.$q.a + b)
    Expression qualifier = ((FieldAccess) leftOperand).getQualifier().clone();
    BinaryExpression assignQualifier =
        new BinaryExpression(
            qualifier.getTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_QUALIFIER_TEMP, qualifier.getTypeDescriptor()),
            BinaryOperator.ASSIGN,
            qualifier); // Numbers.$q = q
    BinaryExpression assignment =
        expandExpressionWithQualifier(
            leftOperand, operator.getUnderlyingBinaryOperator(), rightOperand);
    return new MultiExpression(assignQualifier, assignment);
  }

  public static Expression splitPostfixExpression(PostfixExpression postfixExpression) {
    Expression operand = postfixExpression.getOperand();
    PostfixOperator operator = postfixExpression.getOperator();
    TypeDescriptor typeDescriptor = operand.getTypeDescriptor();

    if (canExpressionBeEvaluatedTwice(operand)) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // a++; => (Numbers.$v = a, a = a + 1, Numbers.$v)
      BinaryExpression assignVar =
          new BinaryExpression(
              operand.getTypeDescriptor(),
              createNumbersFieldAccess(NUMBERS_VALUE_TEMP, operand.getTypeDescriptor()),
              BinaryOperator.ASSIGN,
              operand); //Numbers.$v = boxA
      BinaryExpression assignment =
          expandExpressionNoQualifier(
              operand.clone(),
              operator.getUnderlyingBinaryOperator(),
              createLiteralOne(typeDescriptor));
      return new MultiExpression(
          assignVar,
          assignment,
          createNumbersFieldAccess(NUMBERS_VALUE_TEMP, operand.getTypeDescriptor()));
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // q.a++; =>
    // (Numbers.$q = q, Numbers.$v = Numbers.$q.a, Numbers.$q.a = Numbers.$q.a + 1, Numbers.$v)
    FieldAccess fieldAccess = (FieldAccess) operand;
    Expression qualifier = fieldAccess.getQualifier().clone();
    FieldDescriptor target = fieldAccess.getTarget();

    BinaryExpression assignQualifier =
        new BinaryExpression(
            qualifier.getTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_QUALIFIER_TEMP, qualifier.getTypeDescriptor()),
            BinaryOperator.ASSIGN,
            qualifier); // Numbers.$q = q
    BinaryExpression assignVar =
        new BinaryExpression(
            target.getEnclosingClassTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_VALUE_TEMP, target.getTypeDescriptor()),
            BinaryOperator.ASSIGN,
            FieldAccess.Builder.from(target)
                .setQualifier(
                    createNumbersFieldAccess(NUMBERS_QUALIFIER_TEMP, qualifier.getTypeDescriptor()))
                .build()); //Numbers.$v = Numbers.$q.a
    BinaryExpression assignment =
        expandExpressionWithQualifier(
            operand, operator.getUnderlyingBinaryOperator(), createLiteralOne(typeDescriptor));
    return new MultiExpression(
        assignQualifier,
        assignVar,
        assignment,
        createNumbersFieldAccess(NUMBERS_VALUE_TEMP, target.getTypeDescriptor()));
  }

  public static Expression splitPrefixExpression(PrefixExpression prefixExpression) {
    checkArgument(prefixExpression.getOperator().hasSideEffect());

    Expression operand = prefixExpression.getOperand();
    PrefixOperator operator = prefixExpression.getOperator();
    TypeDescriptor typeDescriptor = operand.getTypeDescriptor();

    if (canExpressionBeEvaluatedTwice(operand)) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // ++a => a = a + 1
      return expandExpressionNoQualifier(
          operand, operator.getUnderlyingBinaryOperator(), createLiteralOne(typeDescriptor));
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // ++q.a; => (Numbers.$q = q, Numbers.$q.a = Numbers.$q.a + 1)
    Expression qualifier = ((FieldAccess) operand).getQualifier();
    BinaryExpression assignQualifier =
        new BinaryExpression(
            qualifier.getTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_QUALIFIER_TEMP, qualifier.getTypeDescriptor()),
            BinaryOperator.ASSIGN,
            qualifier); // Numbers.$q = q
    Expression assignment =
        expandExpressionWithQualifier(
            operand, operator.getUnderlyingBinaryOperator(), createLiteralOne(typeDescriptor));
    return new MultiExpression(assignQualifier, assignment);
  }

  /** Returns number literal with value 1. */
  public static NumberLiteral createLiteralOne(TypeDescriptor typeDescriptor) {
    TypeDescriptor primitiveTypeDescriptor =
        TypeDescriptors.isBoxedType(typeDescriptor)
            ? TypeDescriptors.getPrimitiveTypeFromBoxType(typeDescriptor)
            : typeDescriptor;

    return new NumberLiteral(primitiveTypeDescriptor, 1);
  }

  /** Returns Numbers.fieldName. */
  private static FieldAccess createNumbersFieldAccess(
      String fieldName, TypeDescriptor typeDescriptor) {
    return FieldAccess.Builder.from(
            FieldDescriptor.Builder.fromDefault()
                .setEnclosingClassTypeDescriptor(BootstrapType.NUMBERS.getDescriptor())
                .setFieldName(fieldName)
                .setTypeDescriptor(typeDescriptor)
                .setIsStatic(true)
                .setJsInfo(JsInfo.RAW_FIELD)
                .build())
        .build();
  }

  /** Returns assignment in the form of {leftOperand = leftOperand operator rightOperand}. */
  private static BinaryExpression expandExpressionNoQualifier(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {

    // TODO: leftOperand is not being cloned and is duplicated in the AST. This
    // violates the invariant that the AST is a proper tree (not a DAG). Fix.

    return BinaryExpression.Builder.asAssignmentTo(leftOperand)
        .setRightOperand(
            new BinaryExpression(
                binaryOperationResultType(
                    operator, leftOperand.getTypeDescriptor(), rightOperand.getTypeDescriptor()),
                leftOperand.clone(),
                operator,
                rightOperand))
        .build();
  }

  /**
   * For {@code leftOperand} in the form of qualifier.a, returns Numbers.$q.a = Numbers.$q.a
   * operator rightOperand. If a is in the form of boxA.intValue(), fix it with Numbers.$q.boxA =
   * box(Numbers.$q.a operator rightOperand)
   */
  private static BinaryExpression expandExpressionWithQualifier(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {

    BinaryExpression binaryExpression =
        new BinaryExpression(
            binaryOperationResultType(
                operator, leftOperand.getTypeDescriptor(), rightOperand.getTypeDescriptor()),
            replaceQualifier(leftOperand), // Numbers.$q.a
            operator,
            rightOperand);
    BinaryExpression assignment =
        new BinaryExpression(
            leftOperand.getTypeDescriptor(),
            replaceQualifier(leftOperand), // Numbers.$q.a
            BinaryOperator.ASSIGN,
            binaryExpression);
    return assignment;
  }

  /**
   * If the expression is a field access with a non-this qualifier, it needs to be split to avoid
   * double side-effect. Otherwise, it is a simple case.
   */
  private static boolean canExpressionBeEvaluatedTwice(Expression expression) {
    if (expression instanceof VariableReference) {
      return true;
    }

    if (expression instanceof FieldAccess) {
      FieldAccess fieldAccess = (FieldAccess) expression;
      return AstUtils.hasTypeReferenceAsQualifier(fieldAccess)
          || AstUtils.hasThisReferenceAsQualifier(fieldAccess);
    }

    // For array access expressions.
    if (expression instanceof ArrayAccess) {
      ArrayAccess arrayAccess = (ArrayAccess) expression;
      return canExpressionBeEvaluatedTwice(arrayAccess.getArrayExpression())
          && canExpressionBeEvaluatedTwice(arrayAccess.getIndexExpression());
    }

    return false;
  }

  /**
   * If {@code expression} is in the form of q.boxA.intValue(), returns Numbers.$q.boxA.intValue().
   * Else if {@code expression} is in the form of q.a, returns Numbers.$q.a. Otherwise returns
   * {@code expression} itself.
   */
  private static Expression replaceQualifier(Expression expression) {
    if (!(expression instanceof FieldAccess)) {
      return expression;
    }
    FieldAccess fieldAccess = (FieldAccess) expression;
    Expression newQualifier =
        createNumbersFieldAccess(
            NUMBERS_QUALIFIER_TEMP, fieldAccess.getQualifier().getTypeDescriptor());
    return FieldAccess.Builder.from(fieldAccess).setQualifier(newQualifier).build();
  }

  /** Determines the binary operation type based on the types of the operands. */
  private static TypeDescriptor binaryOperationResultType(
      BinaryOperator operator, TypeDescriptor leftOperandType, TypeDescriptor rightRightType) {
    TypeDescriptor unboxedLeftOperandType = unboxIfBoxedType(leftOperandType);

    if (!unboxedLeftOperandType.isPrimitive()) {
      // This is a string operation.
      return unboxedLeftOperandType;
    }

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
        if (unboxedLeftOperandType.equalsIgnoreNullability(
            TypeDescriptors.get().primitiveBoolean)) {
          // Handle logical operations (on type boolean).
          return unboxedLeftOperandType;
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
        checkArgument(TypeDescriptors.isBoxedOrPrimitiveType(rightRightType));
        TypeDescriptor unboxedRightOperandType = unboxIfBoxedType(rightRightType);
        return widerType(unboxedLeftOperandType, unboxedRightOperandType);
      case LEFT_SHIFT:
      case RIGHT_SHIFT_SIGNED:
      case RIGHT_SHIFT_UNSIGNED:
        /**
         * Shift operators: JLS 15.19.
         *
         * <p>Type type of the operation is the type of the promoted left hand operand.
         */
        return unboxedLeftOperandType;
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
   * Returns the corresponding unboxed type if the {@code setTypeDescriptor} is a boxed type; {@code
   * setTypeDescriptor} otherwise
   */
  private static TypeDescriptor unboxIfBoxedType(TypeDescriptor typeDescriptor) {
    if (TypeDescriptors.isBoxedType(typeDescriptor)) {
      return TypeDescriptors.getPrimitiveTypeFromBoxType(typeDescriptor);
    }
    return typeDescriptor;
  }
}
