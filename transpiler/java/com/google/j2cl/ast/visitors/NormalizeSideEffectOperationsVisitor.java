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

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

import java.util.Arrays;

/**
 * Normalizes side effect operations (compound assignment, increment and decrement) for Long
 * instances and boxing instances.
 */
public class NormalizeSideEffectOperationsVisitor extends AbstractRewriter {
  private static final String NUMBERS_Q = "$q";
  private static final String NUMBERS_V = "$v";

  public static void applyTo(CompilationUnit compilationUnit) {
    new NormalizeSideEffectOperationsVisitor().normalizeSideEffectOperations(compilationUnit);
  }

  private void normalizeSideEffectOperations(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  @Override
  public Node rewriteBinaryExpression(BinaryExpression expression) {
    if (!shouldNormalize(expression)) {
      return expression;
    }
    BinaryOperator operator = expression.getOperator();
    Expression leftOperand = expression.getLeftOperand();
    Expression rightOperand = expression.getRightOperand();

    Expression qualifier = getQualifier(leftOperand);

    // The corresponding binary operator, e.g += => +, -= => -, etc.
    BinaryOperator binaryOperator = AstUtils.compoundAssignmentToBinaryOperator(operator);

    if (qualifier == null) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // a += x => a = a + x
      return expandExpressionNoQualifier(leftOperand, binaryOperator, rightOperand);
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // q.a += b; => (Numbers.$q = q, Numbers.$q.a = Numbers.$q.a + b)
    BinaryExpression assignQualifier =
        new BinaryExpression(
            qualifier.getTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_Q, qualifier.getTypeDescriptor()),
            BinaryOperator.ASSIGN,
            qualifier); // Numbers.$q = q
    BinaryExpression assignment =
        expandExpressionWithQualifier(leftOperand, binaryOperator, rightOperand);
    return new MultiExpression(Arrays.<Expression>asList(assignQualifier, assignment));
  }

  @Override
  public Node rewritePrefixExpression(PrefixExpression expression) {
    if (!shouldNormalize(expression)) {
      return expression;
    }
    Expression operand = expression.getOperand();
    PrefixOperator operator = expression.getOperator();
    TypeDescriptor typeDescriptor = operand.getTypeDescriptor();

    Expression qualifier = getQualifier(operand); // qualifier of the boxed instance

    // The corresponding binary operator, e.g ++ => +, -- => -
    BinaryOperator binaryOperator = AstUtils.compoundAssignmentToBinaryOperator(operator);

    if (qualifier == null) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // ++a => a = a + 1
      return expandExpressionNoQualifier(operand, binaryOperator, getOne(typeDescriptor));
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // ++q.a; => (Numbers.$q = q, Numbers.$q.a = Numbers.$q.a + 1)
    BinaryExpression assignQualifier =
        new BinaryExpression(
            qualifier.getTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_Q, qualifier.getTypeDescriptor()),
            BinaryOperator.ASSIGN,
            qualifier); // Numbers.$q = q
    Expression assignment =
        expandExpressionWithQualifier(operand, binaryOperator, getOne(typeDescriptor));
    return new MultiExpression(Arrays.<Expression>asList(assignQualifier, assignment));
  }

  @Override
  public Node rewritePostfixExpression(PostfixExpression expression) {
    if (!shouldNormalize(expression)) {
      return expression;
    }
    Expression operand = expression.getOperand();
    PostfixOperator operator = expression.getOperator();
    TypeDescriptor typeDescriptor = operand.getTypeDescriptor();

    Expression qualifier = getQualifier(operand);

    // The corresponding binary operator, e.g ++ => +, -- => -
    BinaryOperator binaryOperator = AstUtils.compoundAssignmentToBinaryOperator(operator);

    if (qualifier == null) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // a++; => (Numbers.$v = a, a = a + 1, Numbers.$v)
      Expression originalExpression = getOriginalExpression(operand);
      BinaryExpression assignVar =
          new BinaryExpression(
              originalExpression.getTypeDescriptor(),
              createNumbersFieldAccess(NUMBERS_V, originalExpression.getTypeDescriptor()),
              BinaryOperator.ASSIGN,
              originalExpression); //Numbers.$v = boxA
      BinaryExpression assignment =
          expandExpressionNoQualifier(operand, binaryOperator, getOne(typeDescriptor));
      return new MultiExpression(
          Arrays.<Expression>asList(
              assignVar,
              assignment,
              createNumbersFieldAccess(NUMBERS_V, originalExpression.getTypeDescriptor())));
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // q.a++; =>
    // (Numbers.$q = q, Numbers.$v = Numbers.$q.a, Numbers.$q.a = Numbers.$q.a + 1, Numbers.$v)
    FieldAccess fieldAccess = (FieldAccess) getOriginalExpression(operand);
    FieldDescriptor target = fieldAccess.getTarget();

    BinaryExpression assignQualifier =
        new BinaryExpression(
            qualifier.getTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_Q, qualifier.getTypeDescriptor()),
            BinaryOperator.ASSIGN,
            qualifier); // Numbers.$q = q
    BinaryExpression assignVar =
        new BinaryExpression(
            target.getEnclosingClassTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_V, target.getTypeDescriptor()),
            BinaryOperator.ASSIGN,
            new FieldAccess(
                createNumbersFieldAccess(NUMBERS_Q, qualifier.getTypeDescriptor()),
                target)); //Numbers.$v = Numbers.$q.a
    BinaryExpression assignment =
        expandExpressionWithQualifier(operand, binaryOperator, getOne(typeDescriptor));
    return new MultiExpression(
        Arrays.<Expression>asList(
            assignQualifier,
            assignVar,
            assignment,
            createNumbersFieldAccess(NUMBERS_V, target.getTypeDescriptor())));
  }

  /**
   * Returns assignment in the form of {leftOperand = leftOperand operator rightOperand}.
   * If leftOperand is in the form of boxA.intValue(),
   * fix it with boxA = box(leftOperand operator rightOpand)
   */
  private static BinaryExpression expandExpressionNoQualifier(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {
    TypeDescriptor typeDescriptor = leftOperand.getTypeDescriptor();
    BinaryExpression assignment =
        new BinaryExpression(
            typeDescriptor,
            leftOperand,
            BinaryOperator.ASSIGN,
            new BinaryExpression(typeDescriptor, leftOperand, operator, rightOperand));
    return maybeFixBoxAssignment(assignment);
  }

  /**
   * For {@code leftOperand} in the form of qualifier.a, returns
   * Numbers.$q.a = Numbers.$q.a operator rightOperand.
   * If a is in the form of boxA.intValue(), fix it with
   * Numbers.$q.boxA = box(Numbers.$q.a operator rightOperand)
   */
  private static BinaryExpression expandExpressionWithQualifier(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {
    TypeDescriptor typeDescriptor = leftOperand.getTypeDescriptor();
    BinaryExpression binaryExpression =
        new BinaryExpression(
            typeDescriptor,
            replaceQualifier(leftOperand), // Numbers.$q.a
            operator,
            rightOperand);
    BinaryExpression assignment =
        new BinaryExpression(
            typeDescriptor,
            replaceQualifier(leftOperand), // Numbers.$q.a
            BinaryOperator.ASSIGN,
            binaryExpression);
    return maybeFixBoxAssignment(assignment);
  }

  /**
   * Returns if the expression is the method call to Integer.intValue(), or Byte.bytevalue(), etc.
   */
  private static boolean isValueMethodCall(Expression expression) {
    if (!(expression instanceof MethodCall)) {
      return false;
    }
    MethodCall methodCall = (MethodCall) expression;
    MethodDescriptor target = methodCall.getTarget();
    TypeDescriptor enclosingTypeDescriptor = target.getEnclosingClassTypeDescriptor();
    if (!TypeDescriptors.isBoxedType(enclosingTypeDescriptor)) {
      return false; // not a boxed type.
    }
    TypeDescriptor primitiveTypeDescriptor =
        TypeDescriptors.getPrimitiveTypeFromBoxType(enclosingTypeDescriptor);
    String expectedMethodName =
        primitiveTypeDescriptor.getSimpleName() + MethodDescriptor.VALUE_METHOD_SUFFIX;
    return target.getMethodName().equals(expectedMethodName);
  }

  /**
   * If {@code expression} is in the form of q.boxA.intValue(), returns q.
   * Else if {@code expression} is in the form of q.a, returns q.
   * Otherwise returns null.
   */
  private static Expression getQualifier(Expression expression) {
    if (isValueMethodCall(expression)) {
      return getQualifier(((MethodCall) expression).getQualifier());
    }
    if (!(expression instanceof FieldAccess)) {
      return null;
    }
    return ((FieldAccess) expression).getQualifier();
  }

  /**
   * If {@code expression} is in the form of q.boxA.intValue(), returns Numbers.$q.boxA.intValue().
   * Else if {@code expression} is in the form of q.a, returns Numbers.$q.a.
   * Otherwise returns {@code expression} itself.
   */
  private static Expression replaceQualifier(Expression expression) {
    if (isValueMethodCall(expression)) {
      MethodCall methodCall = (MethodCall) expression;
      return new MethodCall(
          replaceQualifier(methodCall.getQualifier()),
          methodCall.getTarget(),
          methodCall.getArguments());
    }
    if (!(expression instanceof FieldAccess)) {
      return expression;
    }
    Expression newQualifier =
        createNumbersFieldAccess(
            NUMBERS_Q, ((FieldAccess) expression).getQualifier().getTypeDescriptor());
    return new FieldAccess(newQualifier, ((FieldAccess) expression).getTarget());
  }

  /**
   * If {@code expression} is in the form of boxA.intValue(), returns boxA.
   * Otherwise, returns {@code expression} itself.
   */
  private static Expression getOriginalExpression(Expression expression) {
    if (!isValueMethodCall(expression)) {
      return expression;
    }
    return ((MethodCall) expression).getQualifier();
  }

  /**
   * Fix the case boxA.intValue() = boxA.intValue() + x with
   * boxA = box(boxA.intvalue() + x)
   */
  private static BinaryExpression maybeFixBoxAssignment(BinaryExpression expression) {
    BinaryOperator operator = expression.getOperator();
    Expression leftOperand = expression.getLeftOperand();
    Expression rightOperand = expression.getRightOperand();
    if (BinaryOperator.ASSIGN != operator || !isValueMethodCall(leftOperand)) {
      return expression;
    }

    Preconditions.checkArgument(leftOperand instanceof MethodCall);
    MethodCall valueMethodCall = (MethodCall) leftOperand;
    Expression boxedInstance = valueMethodCall.getQualifier(); // boxA, the boxed instance.

    TypeDescriptor primitiveType = leftOperand.getTypeDescriptor();
    Preconditions.checkArgument(primitiveType.isPrimitive());
    TypeDescriptor boxType = TypeDescriptors.getBoxTypeFromPrimitiveType(primitiveType);

    return new BinaryExpression(
        boxType, boxedInstance, BinaryOperator.ASSIGN, AstUtils.box(rightOperand));
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
            TypeDescriptors.NUMBERS_TYPE_DESCRIPTOR,
            fieldName,
            typeDescriptor));
  }

  /**
   * Returns number literal with value 1.
   */
  private static Expression getOne(TypeDescriptor typeDescriptor) {
    return new NumberLiteral(typeDescriptor, 1);
  }

  private static boolean shouldNormalize(BinaryExpression expression) {
    return expression.getOperator() != BinaryOperator.ASSIGN
        && AstUtils.isAssignmentOperator(expression.getOperator())
        && (TypeDescriptors.get().primitiveLong == expression.getLeftOperand().getTypeDescriptor()
            || isValueMethodCall(expression.getLeftOperand()));
  }

  private static boolean shouldNormalize(PrefixExpression expression) {
    return AstUtils.isAssignmentOperator(expression.getOperator())
        && (TypeDescriptors.get().primitiveLong == expression.getTypeDescriptor()
            || isValueMethodCall(expression.getOperand()));
  }

  private static boolean shouldNormalize(PostfixExpression expression) {
    return TypeDescriptors.get().primitiveLong == expression.getTypeDescriptor()
        || isValueMethodCall(expression.getOperand());
  }
}
