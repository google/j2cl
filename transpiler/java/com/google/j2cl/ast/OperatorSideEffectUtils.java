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

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

import java.util.Arrays;

/**
 * Utility functions for splitting operators expressions that have a side effect.
 *
 * <p>For example:
 * <ul>
 * <li>i++ becomes i = i + 1<li>
 * <li>a = i++ becomes a = (v = i, i = i + 1, v)<li>
 * <li>a = change().i++ becomes a = (q = change(), v = q.i, q.i = q.i + 1, v)<li>
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
    Preconditions.checkArgument(binaryExpression.getOperator().isCompoundAssignment());

    BinaryOperator operator = binaryExpression.getOperator();
    Expression leftOperand = binaryExpression.getLeftOperand();
    Expression rightOperand = binaryExpression.getRightOperand();

    Expression qualifier = getQualifier(leftOperand);

    if (isSimpleCase(leftOperand)) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // a += x => a = a + x
      return expandExpressionNoQualifier(leftOperand, operator.withoutAssignment(), rightOperand);
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // q.a += b; => (Numbers.$q = q, Numbers.$q.a = Numbers.$q.a + b)
    BinaryExpression assignQualifier =
        new BinaryExpression(
            qualifier.getTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_QUALIFIER_TEMP, qualifier.getTypeDescriptor()),
            BinaryOperator.ASSIGN,
            qualifier); // Numbers.$q = q
    BinaryExpression assignment =
        expandExpressionWithQualifier(leftOperand, operator.withoutAssignment(), rightOperand);
    return new MultiExpression(Arrays.<Expression>asList(assignQualifier, assignment));
  }

  public static Expression splitPostfixExpression(PostfixExpression postfixExpression) {
    Expression operand = postfixExpression.getOperand();
    PostfixOperator operator = postfixExpression.getOperator();
    TypeDescriptor typeDescriptor = operand.getTypeDescriptor();

    Expression qualifier = getQualifier(operand);

    if (isSimpleCase(operand)) {
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
              operand, operator.withoutSideEffect(), createLiteralOne(typeDescriptor));
      return new MultiExpression(
          Arrays.<Expression>asList(
              assignVar,
              assignment,
              createNumbersFieldAccess(NUMBERS_VALUE_TEMP, operand.getTypeDescriptor())));
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // q.a++; =>
    // (Numbers.$q = q, Numbers.$v = Numbers.$q.a, Numbers.$q.a = Numbers.$q.a + 1, Numbers.$v)
    FieldDescriptor target = ((FieldAccess) operand).getTarget();

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
            new FieldAccess(
                createNumbersFieldAccess(NUMBERS_QUALIFIER_TEMP, qualifier.getTypeDescriptor()),
                target)); //Numbers.$v = Numbers.$q.a
    BinaryExpression assignment =
        expandExpressionWithQualifier(
            operand, operator.withoutSideEffect(), createLiteralOne(typeDescriptor));
    return new MultiExpression(
        Arrays.<Expression>asList(
            assignQualifier,
            assignVar,
            assignment,
            createNumbersFieldAccess(NUMBERS_VALUE_TEMP, target.getTypeDescriptor())));
  }

  public static Expression splitPrefixExpression(PrefixExpression prefixExpression) {
    Preconditions.checkArgument(prefixExpression.getOperator().hasSideEffect());

    Expression operand = prefixExpression.getOperand();
    PrefixOperator operator = prefixExpression.getOperator();
    TypeDescriptor typeDescriptor = operand.getTypeDescriptor();

    Expression qualifier = getQualifier(operand); // qualifier of the boxed instance

    if (isSimpleCase(operand)) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // ++a => a = a + 1
      return expandExpressionNoQualifier(
          operand, operator.withoutSideEffect(), createLiteralOne(typeDescriptor));
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // ++q.a; => (Numbers.$q = q, Numbers.$q.a = Numbers.$q.a + 1)
    BinaryExpression assignQualifier =
        new BinaryExpression(
            qualifier.getTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_QUALIFIER_TEMP, qualifier.getTypeDescriptor()),
            BinaryOperator.ASSIGN,
            qualifier); // Numbers.$q = q
    Expression assignment =
        expandExpressionWithQualifier(
            operand, operator.withoutSideEffect(), createLiteralOne(typeDescriptor));
    return new MultiExpression(Arrays.<Expression>asList(assignQualifier, assignment));
  }

  /**
   * Returns number literal with value 1.
   */
  public static NumberLiteral createLiteralOne(TypeDescriptor typeDescriptor) {
    TypeDescriptor primitiveTypeDescriptor =
        TypeDescriptors.isBoxedType(typeDescriptor)
            ? TypeDescriptors.getPrimitiveTypeFromBoxType(typeDescriptor)
            : typeDescriptor;

    return new NumberLiteral(primitiveTypeDescriptor, 1);
  }

  /**
   * Returns Numbers.fieldName.
   */
  private static FieldAccess createNumbersFieldAccess(
      String fieldName, TypeDescriptor typeDescriptor) {
    return new FieldAccess(
        null,
        FieldDescriptor.createRaw(
            true, // isStatic
            BootstrapType.NUMBERS.getDescriptor(),
            fieldName,
            typeDescriptor));
  }

  /**
   * Returns assignment in the form of {leftOperand = leftOperand operator rightOperand}.
   */
  private static BinaryExpression expandExpressionNoQualifier(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {
    TypeDescriptor numericTypeDescriptor =
        TypeDescriptors.asOperatorReturnType(leftOperand.getTypeDescriptor());
    BinaryExpression assignment =
        new BinaryExpression(
            leftOperand.getTypeDescriptor(),
            leftOperand,
            BinaryOperator.ASSIGN,
            new BinaryExpression(numericTypeDescriptor, leftOperand, operator, rightOperand));
    return assignment;
  }

  /**
   * For {@code leftOperand} in the form of qualifier.a, returns
   * Numbers.$q.a = Numbers.$q.a operator rightOperand.
   * If a is in the form of boxA.intValue(), fix it with
   * Numbers.$q.boxA = box(Numbers.$q.a operator rightOperand)
   */
  private static BinaryExpression expandExpressionWithQualifier(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {
    TypeDescriptor numericTypeDescriptor =
        TypeDescriptors.asOperatorReturnType(leftOperand.getTypeDescriptor());
    BinaryExpression binaryExpression =
        new BinaryExpression(
            numericTypeDescriptor,
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
   * If {@code expression} is in the form of q.boxA.intValue(), returns q.
   * Else if {@code expression} is in the form of q.a, returns q.
   * Otherwise returns null.
   */
  private static Expression getQualifier(Expression expression) {
    if (!(expression instanceof FieldAccess)) {
      return null;
    }
    return ((FieldAccess) expression).getQualifier();
  }

  /**
   * If the expression is a field access with a non-this qualifier, it needs to be split to avoid
   * double side-effect. Otherwise, it is a simple case.
   */
  private static boolean isSimpleCase(Expression expression) {
    if (!(expression instanceof FieldAccess)) {
      return true;
    }
    FieldAccess fieldAccess = (FieldAccess) expression;
    return AstUtils.hasThisReferenceAsQualifier(fieldAccess);
  }

  /**
   * If {@code expression} is in the form of q.boxA.intValue(), returns Numbers.$q.boxA.intValue().
   * Else if {@code expression} is in the form of q.a, returns Numbers.$q.a.
   * Otherwise returns {@code expression} itself.
   */
  private static Expression replaceQualifier(Expression expression) {
    if (!(expression instanceof FieldAccess)) {
      return expression;
    }
    Expression newQualifier =
        createNumbersFieldAccess(
            NUMBERS_QUALIFIER_TEMP, ((FieldAccess) expression).getQualifier().getTypeDescriptor());
    return new FieldAccess(newQualifier, ((FieldAccess) expression).getTarget());
  }
}
