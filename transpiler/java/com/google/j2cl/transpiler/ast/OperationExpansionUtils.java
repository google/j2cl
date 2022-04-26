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
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility functions for expanding assignment, increment and decrement expressions.
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
public class OperationExpansionUtils {
  public static Expression expandCompoundExpression(BinaryExpression binaryExpression) {
    checkArgument(binaryExpression.getOperator().isCompoundAssignment());

    BinaryOperator operator = binaryExpression.getOperator();
    Expression leftOperand = binaryExpression.getLeftOperand();
    Expression rightOperand = binaryExpression.getRightOperand();

    List<VariableDeclarationFragment> temporaryVariableDeclarations = new ArrayList<>();
    Expression lhs =
        leftOperand.isIdempotent()
            ? leftOperand
            : decomposeLhs(leftOperand, temporaryVariableDeclarations);

    return constructReturnedExpression(
        temporaryVariableDeclarations,
        assignToLeftOperand(lhs, operator.getUnderlyingBinaryOperator(), rightOperand));
  }

  /**
   * Decomposes the lhs of a compound assignment (or the operand of a prefix/postfix expressions
   * introducing temporary variables if necessary.
   */
  private static Expression decomposeLhs(
      Expression lhs, List<VariableDeclarationFragment> temporaryVariableDeclarations) {
    if (lhs instanceof VariableReference) {
      // The lhs will be modified but it can be safely evaluated twice in a row without caring to
      // avoid double side-effects if expanded. See the counter example showing an incorrect
      // rewrite:
      //     a[++i] += i => a[++i] = a[++i] + x
      return lhs;
    }

    if (lhs instanceof ArrayAccess) {
      return decomposeArrayAccess((ArrayAccess) lhs, temporaryVariableDeclarations);
    }
    checkState(lhs instanceof FieldAccess);
    return decomposeFieldAccess((FieldAccess) lhs, temporaryVariableDeclarations);
  }

  private static FieldAccess decomposeFieldAccess(
      FieldAccess lhs, List<VariableDeclarationFragment> temporaryVariableDeclarations) {

    if (lhs.getTarget().isStatic()) {
      // The qualifier here should not be extracted since it is a constructor reference.
      //
      // Note that checking idempotence of the qualifier is not correct since it might still be
      // affected by the side effects in the rhs, e.g.
      //
      //      SomeClass a = foo;
      //      String rv = a.result = (a = bar).toString();
      //
      // if this was not decomposed (because 'a' is idempotent), the above snippet would modify
      // the instance 'foo' but return the value of 'bar.result' instead of 'foo.result' (see
      // expandAssignmentExpression).
      // What ensures correctness here without a rewrite is the qualifier being constant - not
      // being idempotent.
      return lhs;
    }

    // The qualifier is not guaranteed to be free of side effects, so evaluate it first and assign
    // to a temporary variable.
    Expression qualifier = lhs.getQualifier();
    Variable qualifierVariable =
        createTemporaryVariableDeclaration(
            qualifier.getTypeDescriptor(), "$qualifier", qualifier, temporaryVariableDeclarations);
    return FieldAccess.Builder.from(lhs).setQualifier(qualifierVariable.createReference()).build();
  }

  /** Creates a variable that holds the value of {@code expression} to avoid evaluating it twice. */
  private static Variable createTemporaryVariableDeclaration(
      TypeDescriptor variableType,
      String variableName,
      Expression expression,
      List<VariableDeclarationFragment> temporaryVariableDeclarations) {

    Variable qualifierVariable =
        Variable.newBuilder()
            .setFinal(true)
            .setName(variableName)
            .setTypeDescriptor(variableType)
            .build();
    temporaryVariableDeclarations.add(
        VariableDeclarationFragment.newBuilder()
            .setVariable(qualifierVariable)
            .setInitializer(expression)
            .build());
    return qualifierVariable;
  }

  private static ArrayAccess decomposeArrayAccess(
      ArrayAccess lhs, List<VariableDeclarationFragment> temporaryVariableDeclarations) {
    Variable arrayExpressionVariable =
        createTemporaryVariableDeclaration(
            lhs.getArrayExpression().getTypeDescriptor(),
            "$array",
            lhs.getArrayExpression(),
            temporaryVariableDeclarations);

    Variable indexExpressionVariable =
        createTemporaryVariableDeclaration(
            PrimitiveTypes.INT, "$index", lhs.getIndexExpression(), temporaryVariableDeclarations);
    return ArrayAccess.newBuilder()
        .setArrayExpression(arrayExpressionVariable.createReference())
        .setIndexExpression(indexExpressionVariable.createReference())
        .build();
  }

  /**
   * Returns a multiexpression that declares {@code temporaryVariableDeclarations} and executes
   * {@code expressions}.
   */
  private static Expression constructReturnedExpression(
      List<VariableDeclarationFragment> temporaryVariableDeclarations, Expression... expressions) {

    MultiExpression.Builder builder = MultiExpression.newBuilder();
    if (!temporaryVariableDeclarations.isEmpty()) {
      builder.addExpressions(
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclarationFragments(temporaryVariableDeclarations)
              .build());
    }
    return builder.addExpressions(expressions).build();
  }

  public static Expression expandExpression(PostfixExpression postfixExpression) {
    Expression operand = postfixExpression.getOperand();
    PostfixOperator operator = postfixExpression.getOperator();

    List<VariableDeclarationFragment> temporaryVariableDeclarations = new ArrayList<>();
    Expression lhs =
        operand.isIdempotent() ? operand : decomposeLhs(operand, temporaryVariableDeclarations);

    Variable valueVariable =
        createTemporaryVariableDeclaration(
            operand.getTypeDescriptor(), "$value", lhs, temporaryVariableDeclarations);

    return constructReturnedExpression(
        temporaryVariableDeclarations,
        // a++; => (let $value = a, a = a + 1, $value)
        assignToLeftOperand(
            lhs.clone(),
            operator.getUnderlyingBinaryOperator(),
            createLiteralOne(operand.getTypeDescriptor())),
        valueVariable.createReference());
  }

  public static Expression expandExpression(PrefixExpression prefixExpression) {
    checkArgument(prefixExpression.getOperator().hasSideEffect());

    Expression operand = prefixExpression.getOperand();
    PrefixOperator operator = prefixExpression.getOperator();

    List<VariableDeclarationFragment> temporaryVariables = new ArrayList<>();
    Expression lhs = operand.isIdempotent() ? operand : decomposeLhs(operand, temporaryVariables);
    return constructReturnedExpression(
        temporaryVariables,
        assignToLeftOperand(
            lhs,
            operator.getUnderlyingBinaryOperator(),
            createLiteralOne(operand.getTypeDescriptor())));
  }

  public static Expression expandAssignmentExpression(BinaryExpression binaryExpression) {
    checkArgument(binaryExpression.isSimpleAssignment());

    List<VariableDeclarationFragment> temporaryVariables = new ArrayList<>();
    Expression newLhs = decomposeLhs(binaryExpression.getLeftOperand(), temporaryVariables);

    Variable returnedVariable;
    Expression newRhs = binaryExpression.getRightOperand();
    if (newLhs instanceof VariableReference) {
      // No need to introduce a temporary variable for the result since even if the rhs modifies it,
      // it will be overwritten by the assignment operation itself.
      returnedVariable = ((VariableReference) newLhs).getTarget();
    } else {
      returnedVariable =
          createTemporaryVariableDeclaration(
              newLhs.getTypeDescriptor(),
              "$value",
              binaryExpression.getRightOperand(),
              temporaryVariables);
      newRhs = returnedVariable.createReference();
    }
    return constructReturnedExpression(
        temporaryVariables,
        BinaryExpression.Builder.asAssignmentTo(newLhs).setRightOperand(newRhs).build(),
        returnedVariable.createReference());
  }

  /** Returns number literal with value 1. */
  private static NumberLiteral createLiteralOne(TypeDescriptor typeDescriptor) {
    return new NumberLiteral(typeDescriptor.toUnboxedType(), 1);
  }

  /** Returns assignment in the form of {leftOperand = leftOperand operator (rightOperand)}. */
  private static BinaryExpression assignToLeftOperand(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {

    checkArgument(leftOperand.isIdempotent());
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

  private OperationExpansionUtils() {}
}
