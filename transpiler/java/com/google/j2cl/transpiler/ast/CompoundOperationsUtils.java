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
            .setFinal(true)
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

  /**
   * Expands a binary expression that has an array access in the lhs. If {@code valueVariable} is
   * not {@code null}, the prior lhs value is stored in it and that returned by the resulting
   * expression.
   *
   * <ul>
   *   <li>Array and index expressions are idempotent (i.e. can be evaluated more that one time)
   *       <pre><code> array[0] += expression =>
   *     ($valueVariable = array[0],              // store in $valueVariable if it was passed
   *      array[0] = array[0] + expression,
   *      $valueVariable)                         // return $valueVariable if it was passed
   *   </code></pre>
   *   <li>Array expression is idempotent but index expression is not.
   *       <pre><code> getAndChangeArray()[0] += expression =>
   *     ($array = getAndChangeArray(),
   *      $valueVariable = $array[0],             // store in $valueVariable if it was passed
   *      $array[0] = $array[0] + expression,
   *      $valueVariable)                         // return $valueVariable if it was passed
   *   </code></pre>
   *   <li>Index expression is not idempotent (and might have a side-effect on the array
   *       expression).
   *       <pre><code> array[getIndexButModifyArrayOrIndex()] += expression =>
   *     ($array = array,
   *      $index = getIndexButModifyArrayOrIndex(),
   *      $valueVariable = $array[$index],        // store in $valueVariable if it was passed
   *      $array[$index] = $array[$index] + expression,
   *      $valueVariable)                         // return $valueVariable if it was passed
   *   </code></pre>
   * </ul>
   *
   * <p>If {@code valueVariable} is {@code null} then the assigment to it is omitted, and the
   * expression evaluates to the result of the binary expression.
   */
  private static Expression expandArrayAccessReturningPreValue(
      BinaryOperator operator,
      ArrayAccess leftOperand,
      Expression rightOperand,
      Variable valueVariable) {

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
              .setFinal(true)
              .setName("$array")
              .setTypeDescriptor(arrayExpression.getTypeDescriptor())
              .build();
      variableDeclarationFragments.add(
          VariableDeclarationFragment.newBuilder()
              .setVariable(arrayExpressionVariable)
              .setInitializer(arrayExpression)
              .build());
      arrayExpression = arrayExpressionVariable.getReference();
    }

    if (!indexExpression.isIdempotent()) {
      // Store the index expression so that it is not evaluated twice.
      Variable indexExpressionVariable =
          Variable.newBuilder()
              .setFinal(true)
              .setName("$index")
              .setTypeDescriptor(PrimitiveTypes.INT)
              .build();
      variableDeclarationFragments.add(
          VariableDeclarationFragment.newBuilder()
              .setVariable(indexExpressionVariable)
              .setInitializer(indexExpression)
              .build());
      indexExpression = indexExpressionVariable.getReference();
    }

    ArrayAccess arrayAccess =
        ArrayAccess.newBuilder()
            .setArrayExpression(arrayExpression)
            .setIndexExpression(indexExpression)
            .build();
    if (valueVariable != null) {
      // Evaluate the left hand side before evaluating the binary expression.
      variableDeclarationFragments.add(
          VariableDeclarationFragment.newBuilder()
              .setVariable(valueVariable)
              .setInitializer(arrayAccess.clone())
              .build());
    }

    if (!variableDeclarationFragments.isEmpty()) {
      // Declare the temporary variable and initialize to the evaluated qualifier.
      expressions.add(
          VariableDeclarationExpression.newBuilder()
              .setVariableDeclarationFragments(variableDeclarationFragments)
              .build());
    }
    // Evaluate the binary expression.
    expressions.add(assignToLeftOperand(arrayAccess, operator, rightOperand));
    if (valueVariable != null) {
      // Return the saved value.
      expressions.add(valueVariable.getReference());
    }

    // Leave the multi expression even if there is only one expression, because the expansion might
    // need to be parenthesized to preserve precedence.
    return MultiExpression.newBuilder().addExpressions(expressions).build();
  }

  public static Expression expandExpression(PostfixExpression postfixExpression) {
    Expression operand = postfixExpression.getOperand();
    PostfixOperator operator = postfixExpression.getOperator();
    Variable valueVariable =
        Variable.newBuilder()
            .setFinal(true)
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
            .setFinal(true)
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
      // ++a => (a = a + 1)
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

    // Treat as ++a[i] as a[i] += 1.
    return expandArrayAccess(
        operator.getUnderlyingBinaryOperator(),
        (ArrayAccess) operand,
        createLiteralOne(operand.getTypeDescriptor()));
  }

  /** Returns number literal with value 1. */
  private static NumberLiteral createLiteralOne(TypeDescriptor typeDescriptor) {
    return new NumberLiteral(typeDescriptor.toUnboxedType(), 1);
  }

  /** Returns assignment in the form of {leftOperand = leftOperand operator (rightOperand)}. */
  private static BinaryExpression assignToLeftOperand(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {

    return BinaryExpression.Builder.asAssignmentTo(leftOperand)
        .setRightOperand(
            maybeCast(
                leftOperand.getTypeDescriptor(),
                BinaryExpression.newBuilder()
                    .setLeftOperand(leftOperand.clone())
                    .setOperator(operator)
                    .setRightOperand(rightOperand)
                    .build()))
        .build();
  }

  // When expanding compound assignments (and prefix/postfix operations) there is an implicit
  // (narrowing) cast that might need to be inserted.
  //
  // Byte b;
  // ++b;
  //
  // is equivalent to
  //
  // Byte b;
  // b = (byte) (b + 1);
  //
  @SuppressWarnings("ReferenceEquality")
  private static Expression maybeCast(TypeDescriptor typeDescriptor, Expression expression) {
    typeDescriptor =
        TypeDescriptors.isBoxedType(typeDescriptor)
            ? typeDescriptor.toUnboxedType()
            : typeDescriptor;
    if (!TypeDescriptors.isNumericPrimitive(typeDescriptor)
        || typeDescriptor == expression.getTypeDescriptor()) {
      return expression;
    }

    return CastExpression.newBuilder()
        .setCastTypeDescriptor(typeDescriptor)
        .setExpression(expression)
        .build();
  }

}
